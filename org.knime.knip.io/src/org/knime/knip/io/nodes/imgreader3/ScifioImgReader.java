package org.knime.knip.io.nodes.imgreader3;

import java.net.URI;
import java.util.List;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.core.data.DataRow;
import org.knime.core.data.uri.URIDataValue;
import org.knime.knip.io.ScifioImgSource;
import org.knime.knip.io.ScifioImgSource2;

import io.scif.config.SCIFIOConfig;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ScifioImgReader<T extends RealType<T> & NativeType<T>> {

	final private ScifioImgSource2 source;
	final private ConnectionInformation connectionInfo;

	public ScifioImgReader(ImgFactory<T> imgFactory, boolean checkFileFormat, SCIFIOConfig config, int uriColumnIdx) {
		this(imgFactory, checkFileFormat, config, null);
	}

	public ScifioImgReader(ImgFactory<T> imgFactory, boolean checkFileFormat, SCIFIOConfig config,
			ConnectionInformation connectionInfo) {

		this.connectionInfo = connectionInfo;
		source = new ScifioImgSource2(imgFactory, checkFileFormat, config);
	}

	public ScifioReadResult<T> read(final DataRow row, int uriColumnIdx) {

		URIDataValue value = (URIDataValue) row.getCell(uriColumnIdx);

		String extension = value.getURIContent().getExtension();
		URI uri = value.getURIContent().getURI();

		if (connectionInfo == null) {
			source.readImgs(uri);
		}

		// TODO

		final List<Exception> errors = null;
		final List<DataRow> rows = null;

		return new ScifioReadResult<>(rows, errors);
	}

	// cleanup operation
	public void close() {
		source.close();
	}

}
