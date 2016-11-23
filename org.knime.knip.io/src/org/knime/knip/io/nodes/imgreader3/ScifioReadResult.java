package org.knime.knip.io.nodes.imgreader3;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataRow;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ScifioReadResult<T extends RealType<T> & NativeType<T>> {

	private List<DataRow> rows;
	private List<Exception> errors;

	public ScifioReadResult(List<DataRow> rows, List<Exception> errors) {
		this.rows = rows;
		this.errors = errors;

	}

	/**
	 * @return all exceptions encountered
	 */
	public List<Exception> getErrors() {
		return errors;
	}

	/**
	 * @return all succesfully read rows.
	 */
	public List<DataRow> getRows() {
		return rows;

	}

}
