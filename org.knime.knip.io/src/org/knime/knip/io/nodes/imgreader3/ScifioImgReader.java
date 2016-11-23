package org.knime.knip.io.nodes.imgreader3;

import org.knime.core.data.DataRow;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ScifioImgReader<T extends RealType<T> & NativeType<T>> {

	public ScifioReadResult<T> read(DataRow row) {
		
		
		// TODO
		return null;
		
	}

	// cleanup operation 
	public void close() {
		// TODO Auto-generated method stub
		
	}

}
