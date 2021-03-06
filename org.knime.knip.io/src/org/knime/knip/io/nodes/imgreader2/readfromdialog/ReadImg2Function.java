package org.knime.knip.io.nodes.imgreader2.readfromdialog;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.ExecutionContext;
import org.knime.core.util.Pair;
import org.knime.core.util.pathresolve.ResolverUtil;
import org.knime.knip.base.exceptions.KNIPException;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;
import org.knime.knip.io.nodes.imgreader2.AbstractReadImgFunction;

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
 *
 */
class ReadImg2Function<T extends RealType<T> & NativeType<T>> extends AbstractReadImgFunction<T, URI> {

	public ReadImg2Function(ExecutionContext exec, int numberOfFiles, SettingsModelSubsetSelection2 sel,
			boolean readImage, boolean readMetadata, boolean readAllMetaData, boolean checkFileFormat,
			boolean completePathRowKey, boolean isGroupFiles, int seriesSelectionFrom, int seriesSelectionTo,
			ImgFactory<T> imgFactory, final String pixelType) {
		super(exec, numberOfFiles, sel, readImage, readMetadata, readAllMetaData, checkFileFormat, completePathRowKey,
				isGroupFiles, seriesSelectionFrom, seriesSelectionTo, imgFactory, pixelType);
	}

	@Override
	public Stream<Pair<DataRow, Optional<Throwable>>> apply(final URI t) {
		List<Pair<DataRow, Optional<Throwable>>> results = new ArrayList<>();

		try {
			final File localFile = ResolverUtil.resolveURItoLocalOrTempFile(t);
			final String localPath = localFile.getAbsolutePath();
			final String rowKey = (m_completePathRowKey) ? localPath
					: localPath.substring(localPath.lastIndexOf(File.separatorChar) + 1);

			if (!localFile.exists()) {
				results.add(createResultFromException(localPath, rowKey,
						new KNIPException("Image " + rowKey + " doesn't exist!")));
				m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
				return results.stream();
			}

			int numSeries = m_imgSource.getSeriesCount(localPath);
			// get start and end of the series
			int seriesStart = m_selectedSeriesFrom == -1 ? 0 : m_selectedSeriesFrom;
			int seriesEnd = m_selectedSeriesTo == -1 ? numSeries : Math.min(m_selectedSeriesTo + 1, numSeries);
			// load image and metadata for each series index
			IntStream.range(seriesStart, seriesEnd).forEachOrdered(currentSeries -> {
				final RowKey rk = currentSeries > 0 ? new RowKey(rowKey + "_" + currentSeries) : new RowKey(rowKey);
				results.add(readImageAndMetadata(localPath, rk, currentSeries));
			});
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);

			return results.stream();
		} catch (final Exception exc) {
			m_exec.setProgress(Double.valueOf(m_currentFile.incrementAndGet()) / m_numberOfFiles);
			return Arrays.asList(createResultFromException(t.toString(), t.toString(), exc)).stream();
		}

	}
}