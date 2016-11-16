package org.knime.knip.io.nodes.imgreader3.readfrominput;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformation;
import org.knime.base.filehandling.remote.files.Connection;
import org.knime.base.filehandling.remote.files.ConnectionMonitor;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pair;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.io.nodes.imgreader3.AbstractReadImgFunction;
import org.knime.knip.io.nodes.imgreader3.ColumnCreationMode;
import org.knime.knip.io.nodes.imgreader3.URLUtil;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * {@link Function} to read an {@link Img}, OME-XML Metadata or both from a file
 * path.
 * 
 * @author <a href="mailto:danielseebacher@t-online.de">Daniel Seebacher,
 *         University of Konstanz.</a>
 * @author Gabriel Einsdorf
 */
class ReadImgTable2Function<T extends RealType<T> & NativeType<T>> extends AbstractReadImgFunction<T, DataRow> {

	private final ColumnCreationMode columnCreationMode;
	private final int stringIndex;
	private final ConnectionMonitor<? extends Connection> monitor;
	private final ConnectionInformation connectionInfo;

	public ReadImgTable2Function(ExecutionContext exec, int numberOfFiles, SettingsModelSubsetSelection2 sel,
			boolean readImage, boolean readMetadata, boolean readAllMetaData, boolean checkFileFormat,
			boolean isGroupFiles, int seriesSelectionFrom, int seriesSelectionTo, ImgFactory<T> imgFactory,
			ColumnCreationMode columnCreationMode, int stringIndex, ConnectionInformation connectionInfo) {
		super(exec, numberOfFiles, sel, readImage, readMetadata, readAllMetaData, checkFileFormat, false, isGroupFiles,
				seriesSelectionFrom, seriesSelectionTo, imgFactory);

		this.columnCreationMode = columnCreationMode;
		this.stringIndex = stringIndex;
		this.connectionInfo = connectionInfo;

		monitor = new ConnectionMonitor<>();
	}

	@Override
	public Stream<Pair<DataRow, Optional<Throwable>>> apply(DataRow input) {
		List<Pair<DataRow, Optional<Throwable>>> tempResults = new ArrayList<>();

		if (input.getCell(stringIndex).isMissing()) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException("no path specified", input.getKey().getString(),
					new IllegalArgumentException("Input was missing"))).stream();
		}

		final String t = ((StringValue) input.getCell(stringIndex)).getStringValue();
		URI uri = null;
		try {
			uri = URLUtil.encode(t);
		} catch (RuntimeException e) { // TODO Fix exception Type thrown by
										// URLUtil
			handlExceptionDuringExecute(input, t, e);
		}

		final String path;
		int numSeries;
		try {
			URL url = uri.toURL();
			// check if its an Internet address;

			if (url.getProtocol().equalsIgnoreCase("FILE")) {
				path = FileUtil.resolveToPath(url).toString();
			} else if (connectionInfo != null) {
				path = createRemoteFileURI(uri, connectionInfo);
			} else {
				if (url.getProtocol().equalsIgnoreCase("HTTP") || url.getProtocol().equalsIgnoreCase("FTP")
						|| url.getProtocol().equalsIgnoreCase("HTTPS")) {
					path = url.toURI().toString();
				} else {
					path = ""; // FIXME
				}
			}

			numSeries = m_imgSource.getSeriesCount(path);
		} catch (InvalidPathException | IOException | URISyntaxException exc) {
			return handlExceptionDuringExecute(input, t, exc);
		} catch (Exception exc) {
			return handlExceptionDuringExecute(input, t, exc);
		}
		int seriesStart = m_selectedSeriesFrom == -1 ? 0 : m_selectedSeriesFrom;
		int seriesEnd = m_selectedSeriesTo == -1 ? numSeries : Math.min(m_selectedSeriesTo + 1, numSeries);

		// load image and metadata for each series index
		IntStream.range(seriesStart, seriesEnd).forEachOrdered(currentSeries -> {
			RowKey rowKey = input.getKey();
			if (currentSeries > 0) {
				rowKey = new RowKey(rowKey.getString() + "_" + currentSeries);
			}

			tempResults.add(readImageAndMetadata(path, rowKey, currentSeries));
		});

		m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);

		return createOutput(input, tempResults, columnCreationMode, stringIndex);
	}

	private String createRemoteFileURI(URI uri, ConnectionInformation info) {

		String path = uri.getPath();
		String scheme = uri.getScheme();
		try {
			final RemoteFile<? extends Connection> file = RemoteFileFactory.createRemoteFile(uri, connectionInfo,
					monitor);

			String query = MessageFormat.format("omero:name={0}&server={1}&port={2}&user={3}&password={4}", file.getName(),
					info.getHost(), info.getPort(), info.getUser(), info.getPassword());

			if (info.getProtocol().equals("ome")) { // TODO make smart
				query += "&imageId=" + path.substring(path.lastIndexOf("/"));
			}

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
		// TODO Auto-generated method stub

	}

	private Stream<Pair<DataRow, Optional<Throwable>>> handlExceptionDuringExecute(DataRow input, String t,
			Exception exc) {
		m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
		return Arrays.asList(createResultFromException(t, input.getKey().getString(), exc)).stream();
	}

	/**
	 * Takes the input {@link DataRow}, the read images and metadata and creates
	 * the output result row.
	 * 
	 * @param inputRow
	 *            the input {@link DataRow}
	 * @param readFiles
	 *            the {@link List} of read {@link Img}s, with {@link Optional}
	 *            {@link Exception}s.
	 * @param columnSelectionMode
	 *            the column selection mode see
	 *            {@link ImgReaderTable2NodeModel#COL_CREATION_MODES}
	 * @param inputColumnIndex
	 *            the column index of the path.
	 * @return a {@link Stream} with the output {@link DataRow}s.
	 */
	private Stream<Pair<DataRow, Optional<Throwable>>> createOutput(DataRow inputRow,
			List<Pair<DataRow, Optional<Throwable>>> readFiles, ColumnCreationMode columnCreationMode,
			int inputColumnIndex) {

		List<Pair<DataRow, Optional<Throwable>>> outputResults = new ArrayList<>();
		if (columnCreationMode == ColumnCreationMode.NEW_TABLE) {
			return readFiles.stream();
		} else if (columnCreationMode == ColumnCreationMode.APPEND) {
			for (Pair<DataRow, Optional<Throwable>> result : readFiles) {

				List<DataCell> cells = new ArrayList<>();
				for (int i = 0; i < inputRow.getNumCells(); i++) {
					cells.add(inputRow.getCell(i));
				}

				for (int i = 0; i < result.getFirst().getNumCells(); i++) {
					cells.add(result.getFirst().getCell(i));
				}

				outputResults.add(new Pair<>(
						new DefaultRow(result.getFirst().getKey(), cells.toArray(new DataCell[cells.size()])),
						result.getSecond()));
			}
		} else if (columnCreationMode == ColumnCreationMode.REPLACE) {
			for (Pair<DataRow, Optional<Throwable>> result : readFiles) {

				List<DataCell> cells = new ArrayList<>();
				for (int i = 0; i < inputRow.getNumCells(); i++) {
					cells.add(inputRow.getCell(i));
				}

				cells.set(stringIndex, result.getFirst().getCell(0));
				if (result.getFirst().getNumCells() > 1) {
					cells.add(stringIndex + 1, result.getFirst().getCell(1));
				}

				outputResults.add(new Pair<>(
						new DefaultRow(result.getFirst().getKey(), cells.toArray(new DataCell[cells.size()])),
						result.getSecond()));
			}
		} else {
			throw new IllegalStateException(
					"Support for the columncreation mode" + columnCreationMode.toString() + " is not implemented!");
		}

		return outputResults.stream();
	}
}
