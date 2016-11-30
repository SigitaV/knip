package org.knime.knip.io.nodes.imgreader3;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.knime.base.data.replace.ReplacedColumnsDataRow;
import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.data.DataRow;
import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.io.ScifioImgSource2;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettings.ColumnCreationMode;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettings.MetadataMode;

import net.imagej.ImgPlus;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * This class provides an unified way to read images for the different image
 * reader nodes,
 *
 * @author Gabriel Einsdorf
 *
 * @param <T>
 */
public class ScifioImgReader<T extends RealType<T> & NativeType<T>> {

	private final ScifioImgSource2 source;
	private final ConnectionInformation connectionInfo;
	private final ExecutionContext exec;
	private final ImgPlusCellFactory cellFactory;
	private final int uriColumnIdx;
	private final boolean checkFileFormat;
	private final ImgFactory<T> imgFactory;
	private final MetadataMode metaDataMode;
	private final ColumnCreationMode columnCreationMode;

	// public ScifioImgReader(ImgFactory<T> imgFactory, boolean checkFileFormat,
	// SCIFIOConfig config,
	// ExecutionContext exec) {
	// this(imgFactory, checkFileFormat, config, exec, null, 0); // FIXME
	// }
	//
	// /**
	// * Creates a ScifioReader that uses a remote connection
	// *
	// * @param imgFactory
	// * @param checkFileFormat
	// * @param config
	// * @param connectionInfo
	// * @param uriColumnIdx
	// */
	// public ScifioImgReader(ImgFactory<T> imgFactory, boolean checkFileFormat,
	// SCIFIOConfig config,
	// ExecutionContext exec, ConnectionInformation connectionInfo, int
	// uriColumnIdx) {
	//
	// this.connectionInfo = connectionInfo;
	// this.exec = exec;
	// this.uriColumnIdx = uriColumnIdx;
	// source = new ScifioImgSource2(imgFactory, checkFileFormat, config);
	// cellFactory = new ImgPlusCellFactory(exec);
	// }

	public ScifioReadResult<T> read(final URI uri) {

		final List<ImgPlus<T>> imgs;
		final List<Exception> errors;
		final List<DataRow> rows = null;

		// if (connectionInfo == null) {
		String metData;
		final boolean readImages = true; // FIXME
		final boolean readMetaData = false;

		try {

			if (readMetaData) {
				metData = source.getOMEXMLMetadata(uri);
			}
			if (readImages) {
				imgs = source.readImgs(uri);

			}

			// rows = makeRows(readImages, row, colMode);
		} catch (final Exception e) {
			return new ScifioReadResult<>(Collections.emptyList(), Optional.of(e));
		}

		// } else {
		// TODO Use new SCIFIO Location API
		// }

		return new ScifioReadResult<>(rows, Optional.empty());
	}

	private List<DataRow> makeRows(final List<ImgPlus<T>> readImages, final DataRow inputRow,
			final ColumnCreationMode mode) throws IOException {
		for (final ImgPlus<T> img : readImages) {
			final ImgPlusCell<T> cell = cellFactory.createCell(img);

			final DataRow outPutRow = new ReplacedColumnsDataRow(inputRow, cell, 0);

		}

		//

		// TODO Auto-generated method stub
		return null;
	}

	// cleanup operation
	public void close() {
		source.close();
	}

	public static class ScifioReaderBuilder<T extends RealType<T> & NativeType<T>> {
		// optional values
		private ConnectionInformation connectionInfo = null;
		private int uriColumnIdx = -1;
		private boolean checkFileFormat = true;

		private ExecutionContext exec;
		private ImgFactory<T> imgFactory;
		private MetadataMode metaDataMode;
		private ColumnCreationMode columnCreationMode;


		public ScifioReaderBuilder<T> connectionInfo(final ConnectionInformation connectionInfo) {
			this.connectionInfo = connectionInfo;
			return this;
		}

		public ScifioReaderBuilder<T> exec(final ExecutionContext exec) {
			this.exec = exec;
			return this;
		}

		public ScifioReaderBuilder<T> uriColumnIdx(final int uriColumnIdx) {
			this.uriColumnIdx = uriColumnIdx;
			return this;
		}

		public ScifioReaderBuilder<T> checkFileFormat(final boolean checkFileFormat) {
			this.checkFileFormat = checkFileFormat;
			return this;
		}

		public ScifioReaderBuilder<T> imgFactory(final ImgFactory<T> imgFactory) {
			this.imgFactory = imgFactory;
			return this;
		}

		public ScifioReaderBuilder<T> metaDataMode(final MetadataMode metaDataMode) {
			this.metaDataMode = metaDataMode;
			return this;
		}

		public ScifioReaderBuilder<T> columnCreationMode(final ColumnCreationMode columnCreationMode) {
			this.columnCreationMode = columnCreationMode;
			return this;
		}

		public ScifioImgReader<T> build() {
			return new ScifioImgReader<>(this);
		}
	}

	private ScifioImgReader(final ScifioReaderBuilder<T> builder) {
		this.connectionInfo = builder.connectionInfo;
		this.exec = builder.exec;
		this.uriColumnIdx = builder.uriColumnIdx;
		this.checkFileFormat = builder.checkFileFormat;
		this.imgFactory = builder.imgFactory;
		this.metaDataMode = builder.metaDataMode;
		this.columnCreationMode = builder.columnCreationMode;
		
		
		 source = new ScifioImgSource2(imgFactory, checkFileFormat, null); // FIXME
		 cellFactory = new ImgPlusCellFactory(exec);
	}
}
