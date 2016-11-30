package org.knime.knip.io.nodes.imgreader3.readfromtable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.io.nodes.imgreader3.AbstractImgReaderNodeModel;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettings;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettings.ColumnCreationMode;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettings.MetadataMode;
import org.knime.knip.io.nodes.imgreader3.ScifioImgReader;
import org.knime.knip.io.nodes.imgreader3.ScifioImgReader.ScifioReaderBuilder;
import org.knime.knip.io.nodes.imgreader3.ScifioReadResult;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImgReaderTable2NodeModel<T extends RealType<T> & NativeType<T>> extends AbstractImgReaderNodeModel<T> {

	private static final int CONNECTION_PORT = 0;
	private static final int DATA_PORT = 0;
	protected static final NodeLogger LOGGER = NodeLogger.getLogger(ImgReaderTable2NodeModel.class);

	/** Settings Models */
	private final SettingsModelColumnName filenameColumnModel = ImgReaderSettings.createFileURIColumnModel();
	private final SettingsModelString columnCreationModeModel = ImgReaderSettings.createColumnCreationModeModel();
	private final SettingsModelString columnSuffixModel = ImgReaderSettings.createColumnSuffixNodeModel();

	private boolean useRemote;

	protected ImgReaderTable2NodeModel() {
		super(new PortType[] { ConnectionInformationPortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });

		addAdditionalSettingsModels(Arrays.asList(filenameColumnModel, columnCreationModeModel, columnSuffixModel));
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

		// check if we are using a remote location
		useRemote = inSpecs[CONNECTION_PORT] != null;

		return createOutSpec(inSpecs);
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {

		final AtomicInteger errorCount = new AtomicInteger(0);

		final BufferedDataTable in = (BufferedDataTable) inObjects[DATA_PORT];
		final BufferedDataContainer container = exec.createDataContainer(in.getDataTableSpec());
		final int uriColIdx = getUriColIdx(in.getDataTableSpec());

		ScifioImgReader<T> reader;
		if (useRemote) {
			reader = createRemoteScifioReader(exec, (ConnectionInformationPortObject) inObjects[CONNECTION_PORT],
					uriColIdx);
		} else {
			reader = createLocalScifioReader(exec);
		}

		for (final DataRow row : in) {
			exec.checkCanceled();

			final ScifioReadResult<T> res = reader.read(row);
			res.getRows().forEach(container::addRowToTable);

			// errors during execution
			if (!res.getError().isPresent()) {
				handleReadErrors(errorCount, row.getKey(), res.getError().get());
			}
		}

		reader.close();
		container.close();
		return new PortObject[] { container.getTable() };
	}

	private PortObjectSpec[] createOutSpec(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

		// ensure there is a valid column
		final int uriColIdx = getUriColIdx(inSpecs[DATA_PORT]);

		// initialze the settings
		final MetadataMode metaDataMode = EnumUtils.valueForName(metadataModeModel.getStringValue(),
				MetadataMode.values());
		final DataTableSpec spec = (DataTableSpec) inSpecs[DATA_PORT];

		final boolean readImage = metaDataMode == MetadataMode.NO_METADATA
				|| metaDataMode == MetadataMode.APPEND_METADATA;
		final boolean readMetadata = metaDataMode == MetadataMode.APPEND_METADATA
				|| metaDataMode == MetadataMode.METADATA_ONLY;

		final ColumnCreationMode columnCreationMode = EnumUtils.valueForName(columnCreationModeModel.getStringValue(),
				ColumnCreationMode.values());

		// Create the outspec

		final DataTableSpec outSpec;
		if (columnCreationMode == ColumnCreationMode.NEW_TABLE) {

			final DataColumnSpec imgSpec = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE).createSpec();
			final DataColumnSpec omeSpec = new DataColumnSpecCreator("OME-XML Metadata", XMLCell.TYPE).createSpec();

			if (readImage && readMetadata) {
				outSpec = new DataTableSpec(imgSpec, omeSpec);
			} else if (readImage) {
				outSpec = new DataTableSpec(imgSpec);
			} else {
				outSpec = new DataTableSpec(omeSpec);
			}

		} else { // Append and replace

			final DataColumnSpec imgSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Image" + columnSuffixModel.getStringValue()),
					ImgPlusCell.TYPE).createSpec();
			final DataColumnSpec omeSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "OME-XML Metadata" + columnSuffixModel.getStringValue()),
					XMLCell.TYPE).createSpec();

			final List<DataColumnSpec> columnSpecs = new ArrayList<>();
			for (final DataColumnSpec s : spec) {
				columnSpecs.add(s);
			}

			if (columnCreationMode == ColumnCreationMode.APPEND) {

				if (readImage && readMetadata) {
					columnSpecs.add(imgSpec);
					columnSpecs.add(omeSpec);
				} else if (readImage) {
					columnSpecs.add(imgSpec);
				} else {
					columnSpecs.add(omeSpec);
				}

				outSpec = new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]));

			} else if (columnCreationMode == ColumnCreationMode.REPLACE) {

				if (readImage && readMetadata) {
					// only the read images replace the URI column
					columnSpecs.set(uriColIdx, imgSpec);
					columnSpecs.add(uriColIdx + 1, omeSpec);
				} else if (readImage) {
					columnSpecs.set(uriColIdx, imgSpec);
				} else {
					columnSpecs.set(uriColIdx, omeSpec);
				}

				outSpec = new DataTableSpec(columnSpecs.toArray(new DataColumnSpec[columnSpecs.size()]));
			} else {
				// should really not happen
				throw new IllegalStateException("Support for the columncreation mode"
						+ columnCreationModeModel.getStringValue() + " is not implemented!");
			}
		}
		return new PortObjectSpec[] { outSpec };
	}

	private int getUriColIdx(final PortObjectSpec inSpec) throws InvalidSettingsException {
		return NodeUtils.autoColumnSelection((DataTableSpec) inSpec, filenameColumnModel, URIDataValue.class,
				ImgReaderTable2NodeModel.class);
	}

	@Override
	public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

		return new StreamableOperator() {
			@Override
			public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
					throws Exception {

				final RowInput in = (RowInput) inputs[DATA_PORT];
				final RowOutput out = (RowOutput) outputs[0];

				final AtomicInteger encounteredExceptionsCount = new AtomicInteger(0);

				final ScifioImgReader<T> reader;
				DataRow row;
				final int uriColIdx = getUriColIdx(inSpecs[DATA_PORT]);
				if (useRemote) {
					final ConnectionInformationPortObject connection = (ConnectionInformationPortObject) ((PortObjectInput) inputs[CONNECTION_PORT])
							.getPortObject();
					reader = createRemoteScifioReader(exec, connection, uriColIdx);
				} else {
					reader = createLocalScifioReader(exec);
				}

				// get next row from input
				while ((row = in.poll()) != null) {
					
					// TODO Make less ugly?
					final ScifioReadResult<T> res = reader
							.read(((URIDataValue) row.getCell(uriColIdx)).getURIContent().getURI());

					for (final DataRow resRow : res.getRows()) {
						out.push(resRow);
					}

					// count number of errors
					if (!res.getError().isPresent()) {
						handleReadErrors(encounteredExceptionsCount, row.getKey(), res.getError().get());
					}
				}

				in.close();
				out.close();
				reader.close();
			}
		};
	}

	private ScifioImgReader<T> createLocalScifioReader(final ExecutionContext exec) {
		// TODO Auto-generated method stub
		return null;
	}

	private ScifioImgReader<T> createRemoteScifioReader(final ExecutionContext exec,
			final ConnectionInformationPortObject connection, int uriColIdx) {

		ScifioReaderBuilder<T> builder = new ScifioReaderBuilder<>();

		// File settings
		builder.checkFileFormat(checkFileFormatModel.getBooleanValue());
		builder.metaDataMode(EnumUtils.valueForName(metadataModeModel.getStringValue(), MetadataMode.values()));
		builder.imgFactory(createImgFactory());

		// connection info
		builder.connectionInfo(connection.getConnectionInformation());

		// Table settings
		builder.exec(exec);
		builder.uriColumnIdx(uriColIdx);
		builder.columnCreationMode(
				EnumUtils.valueForName(columnCreationModeModel.getStringValue(), ColumnCreationMode.values()));

		return builder.build();
	}

	@Override
	protected void doLoadInternals(final File nodeInternDir, final ExecutionMonitor exec) {
		// nothing to do
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE, InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	private void handleReadErrors(final AtomicInteger encounteredExceptionsCount, final RowKey rowKey,
			final Throwable throwable) {
		encounteredExceptionsCount.incrementAndGet();

		LOGGER.warn("Encountered exception while reading from source: " + rowKey + " ; view log for more info.");

		LOGGER.debug(throwable);
	}

}
