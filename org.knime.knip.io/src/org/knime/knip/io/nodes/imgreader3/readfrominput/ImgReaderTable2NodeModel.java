/**
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.io.nodes.imgreader3.readfrominput;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.xml.XMLCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.io.nodes.imgreader3.AbstractImgReaderNodeModel;
import org.knime.knip.io.nodes.imgreader3.ColumnCreationMode;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettings;
import org.knime.knip.io.nodes.imgreader3.MetadataMode;

import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.cell.CellImgFactory;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * This Node reads images.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael
 *         Zinsmaier</a>
 * @author <a href="mailto:gabriel.einsdorf@uni.kn"> Gabriel Einsdorf</a>
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 */
public class ImgReaderTable2NodeModel<T extends RealType<T> & NativeType<T>> extends AbstractImgReaderNodeModel<T> {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(ImgReaderTable2NodeModel.class);
	private static final int DATAPORT = 1;
	private static final int CONNECTIONPORT = 0;

	/**
	 * @return Model to store the selected column in the optional input table
	 */
	public static SettingsModelString createFilenameColumnModel() {
		return new SettingsModelString("filename_column", "");
	}

	public static SettingsModelString createColCreationModeModel() {
		return new SettingsModelString("m_colCreationMode", ColumnCreationMode.NEW_TABLE.toString());
	}

	public static SettingsModelString createColSuffixNodeModel() {
		return new SettingsModelString("m_colSuffix", "");
	}

	private final SettingsModelString m_filenameColumn = createFilenameColumnModel();
	private final SettingsModelString m_colCreationMode = createColCreationModeModel();
	private final SettingsModelString m_colSuffix = createColSuffixNodeModel();
	private ConnectionInformation connectionInfo;

	public ImgReaderTable2NodeModel() {
		super(new PortType[] { ConnectionInformationPortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });

		addSettingsModels(m_filenameColumn, m_colCreationMode, m_colSuffix);
	}

	@Override
	protected DataTableSpec[] configure(DataTableSpec[] inSpecs) throws InvalidSettingsException {

		int imgIdx = getPathColIdx(inSpecs[DATAPORT]);
		if (-1 == imgIdx) {
			throw new InvalidSettingsException("A string column must be selected!");
		}

		return new DataTableSpec[] { getOutspec(inSpecs[DATAPORT], imgIdx) };
	}

	@Override
	protected BufferedDataTable[] execute(PortObject[] inData, ExecutionContext exec) throws Exception {

		// boolean for exceptions and file format
		final AtomicBoolean encounteredExceptions = new AtomicBoolean(false);

		BufferedDataTable inTable = (BufferedDataTable) inData[DATAPORT];

		int imgIdx = getPathColIdx(inTable.getDataTableSpec());
		ReadImgTable2Function<T> rifp = createImgTableFunction(exec, inTable.getDataTableSpec(),
				Long.valueOf(inTable.size()).intValue());

		BufferedDataContainer bdc = exec.createDataContainer(getOutspec(inTable.getDataTableSpec(), imgIdx));

		connectionInfo = ((ConnectionInformationPortObject) inData[CONNECTIONPORT]).getConnectionInformation();

		Iterator<DataRow> iterator = inTable.iterator();
		while (iterator.hasNext()) {
			rifp.apply(iterator.next()).forEachOrdered(dataRow -> {

				if (dataRow.getSecond().isPresent()) {
					encounteredExceptions.set(true);
					LOGGER.warn("Encountered exception while reading image: " + dataRow.getFirst().getKey()
							+ "! view log for more info.");
					LOGGER.debug("Encountered exception while reading image:", dataRow.getSecond().get());
				}

				bdc.addRowToTable(dataRow.getFirst());

			});

			exec.checkCanceled();
		}

		// close img file sources
		rifp.close();

		bdc.close();
		// data table for the table cell viewer
		m_data = bdc.getTable();

		if (encounteredExceptions.get()) {
			setWarningMessage("Encountered errors during execution!");
		}

		return new BufferedDataTable[] { bdc.getTable() };
	}

	@Override
	public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new StreamableOperator() {
			@Override
			public void runFinal(PortInput[] inputs, PortOutput[] outputs, ExecutionContext exec) throws Exception {
				RowInput in = (RowInput) inputs[DATAPORT];
				RowOutput out = (RowOutput) outputs[0];

				// boolean for exceptions and file format
				final AtomicBoolean encounteredExceptions = new AtomicBoolean(false);

				ReadImgTable2Function<T> readImgFunction = createImgTableFunction(exec, in.getDataTableSpec(), 1);

				DataRow row;
				while ((row = in.poll()) != null) {
					readImgFunction.apply(row).forEachOrdered(result -> {
						if (result.getSecond().isPresent()) {
							encounteredExceptions.set(true);
							LOGGER.warn("Encountered exception while reading image: " + result.getFirst().getKey()
									+ "! view log for more info.");
							LOGGER.debug("Encountered exception while reading image:", result.getSecond().get());
						}

						try {
							out.push(result.getFirst());
						} catch (Exception exc) {
							encounteredExceptions.set(true);
							LOGGER.warn("Couldn't push row " + result.getFirst().getKey() + " into output stream.");
							LOGGER.debug("Encountered exception when trying to push result: ", exc);
						}
					});
				}

				if (encounteredExceptions.get()) {
					setWarningMessage("Encountered errors during execution!");
				}

				in.close();
				out.close();

				readImgFunction.close();
			}
		};
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.DISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.DISTRIBUTED };
	}

	private DataTableSpec getOutspec(DataTableSpec spec, int imgIdx) {
		MetadataMode metadataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(), MetadataMode.values());

		boolean readImage = metadataMode == MetadataMode.NO_METADATA || metadataMode == MetadataMode.APPEND_METADATA;
		boolean readMetadata = metadataMode == MetadataMode.APPEND_METADATA
				|| metadataMode == MetadataMode.METADATA_ONLY;

		DataTableSpec outSpec;
		// new table
		ColumnCreationMode columnCreationMode = ColumnCreationMode.fromString(m_colCreationMode.getStringValue());
		switch (columnCreationMode) {
		case NEW_TABLE: {
			DataColumnSpec imgSpec = new DataColumnSpecCreator("Image", ImgPlusCell.TYPE).createSpec();
			DataColumnSpec omeSpec = new DataColumnSpecCreator("OME-XML Metadata", XMLCell.TYPE).createSpec();
			if (readImage && readMetadata) {
				outSpec = new DataTableSpec(imgSpec, omeSpec);
			} else if (readImage) {
				outSpec = new DataTableSpec(imgSpec);
			} else {
				outSpec = new DataTableSpec(omeSpec);
			}
			break;
		}
		case APPEND: {
			DataColumnSpec imgSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Image" + m_colSuffix.getStringValue()), ImgPlusCell.TYPE)
							.createSpec();
			DataColumnSpec omeSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "OME-XML Metadata" + m_colSuffix.getStringValue()),
					XMLCell.TYPE).createSpec();
			List<DataColumnSpec> list = new ArrayList<>();
			for (int i = 0; i < spec.getNumColumns(); i++) {
				list.add(spec.getColumnSpec(i));
			}
			if (readImage && readMetadata) {
				list.add(imgSpec);
				list.add(omeSpec);
			} else if (readImage) {
				list.add(imgSpec);
			} else {
				list.add(omeSpec);
			}
			outSpec = new DataTableSpec(list.toArray(new DataColumnSpec[list.size()]));
			break;
		}
		case REPLACE: {
			DataColumnSpec imgSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "Image" + m_colSuffix.getStringValue()), ImgPlusCell.TYPE)
							.createSpec();
			DataColumnSpec omeSpec = new DataColumnSpecCreator(
					DataTableSpec.getUniqueColumnName(spec, "OME-XML Metadata" + m_colSuffix.getStringValue()),
					XMLCell.TYPE).createSpec();
			List<DataColumnSpec> list = new ArrayList<>();
			for (int i = 0; i < spec.getNumColumns(); i++) {
				list.add(spec.getColumnSpec(i));
			}
			if (readImage && readMetadata) {
				list.set(imgIdx, imgSpec);
				list.add(imgIdx + 1, omeSpec);
			} else if (readImage) {
				list.set(imgIdx, imgSpec);
			} else {
				list.set(imgIdx, omeSpec);
			}
			outSpec = new DataTableSpec(list.toArray(new DataColumnSpec[list.size()]));
			break;
		}
		default:
			throw new IllegalStateException("Support for the columncreation mode" + m_colCreationMode.getStringValue()
					+ " is not implemented!");
		}

		return outSpec;
	}

	private int getPathColIdx(final DataTableSpec inSpec) throws InvalidSettingsException {
		int imgColIndex = -1;
		if (null == this.m_filenameColumn.getStringValue()) {
			return imgColIndex;
		}
		imgColIndex = inSpec.findColumnIndex(this.m_filenameColumn.getStringValue());
		if (-1 == imgColIndex) {
			if ((imgColIndex = NodeUtils.autoOptionalColumnSelection(inSpec, this.m_filenameColumn,
					StringValue.class)) >= 0) {
				setWarningMessage("Auto-configure Image Column: " + this.m_filenameColumn.getStringValue());
			} else {
				throw new InvalidSettingsException("No column selected!");
			}
		}

		return imgColIndex;
	}

	private ReadImgTable2Function<T> createImgTableFunction(ExecutionContext exec, DataTableSpec inSpec, int rowCount)
			throws InvalidSettingsException {

		int imgIdx = getPathColIdx(inSpec);

		MetadataMode metadataMode = EnumUtils.valueForName(m_metadataModeModel.getStringValue(), MetadataMode.values());
		boolean readImage = (metadataMode == MetadataMode.NO_METADATA || metadataMode == MetadataMode.APPEND_METADATA)
				? true : false;
		boolean readMetadata = (metadataMode == MetadataMode.APPEND_METADATA
				|| metadataMode == MetadataMode.METADATA_ONLY) ? true : false;

		// create ImgFactory
		ImgFactory<T> imgFac;// TODO Improve comments
		if (m_imgFactory.getStringValue().equals(ImgReaderSettings.IMG_FACTORIES[1])) {
			imgFac = new PlanarImgFactory<>();
		} else if (m_imgFactory.getStringValue().equals(ImgReaderSettings.IMG_FACTORIES[2])) {
			imgFac = new CellImgFactory<>();
		} else {
			imgFac = new ArrayImgFactory<>();
		}

		// series selection
		int seriesSelectionFrom;
		int seriesSelectionTo;

		if (m_readAllSeries.getBooleanValue()) {
			seriesSelectionFrom = -1;
			seriesSelectionTo = -1;
		} else {
			seriesSelectionFrom = (int) m_seriesRangeSelection.getMinRange();
			seriesSelectionTo = (int) m_seriesRangeSelection.getMaxRange();
		}

		// create image function
		return new ReadImgTable2Function<T>(exec, rowCount, m_planeSelect, readImage, readMetadata,
				m_readAllMetaDataModel.getBooleanValue(), m_checkFileFormat.getBooleanValue(),
				m_isGroupFiles.getBooleanValue(), seriesSelectionFrom, seriesSelectionTo, imgFac,
				ColumnCreationMode.fromString(m_colCreationMode.getStringValue()), imgIdx, connectionInfo);
	}
}
