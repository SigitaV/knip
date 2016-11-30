package org.knime.knip.io.nodes.imgreader3;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataRow;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

/**
 * Stores the result of a reading operation
 *
 * @param <T>
 */
public class ScifioReadResult<T extends RealType<T> & NativeType<T>> {

	private List<DataRow> rows;
	private Optional<Throwable> error;

	/**
	 * @param rows
	 * @param error
	 */
	public ScifioReadResult(List<DataRow> rows, Optional<Throwable> error) {
		this.rows = rows;
		this.error = error;

	}

	/**
	 * @return all exceptions encountered
	 */
	public Optional<Throwable> getError() {
		return error;
	}

	/**
	 * @return all succesfully read rows.
	 */
	public List<DataRow> getRows() {
		return rows;

	}

}
