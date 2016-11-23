package org.knime.knip.io.nodes.imgreader3;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.DataRow;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ScifioReadResult<T extends RealType<T> & NativeType<T>> {

	public Optional<Exception> getErrors() {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * @return
	 */
	public List<DataRow> getRows() {
		// TODO Auto-generated method stub
		return null;
		
	}

}
