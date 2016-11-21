package org.knime.knip.io.nodes.imgreader3.readfromdialog;

import java.io.File;

import org.knime.base.filehandling.remote.connectioninformation.port.ConnectionInformationPortObject;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.knip.io.nodes.imgreader3.AbstractImgReaderNodeModel;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImgReaderTable2NodeModel<T extends RealType<T> & NativeType<T>> extends AbstractImgReaderNodeModel<T> {

	protected ImgReaderTable2NodeModel() {
		super(new PortType[] { ConnectionInformationPortObject.TYPE_OPTIONAL, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
	}

	@Override
	protected void doLoadInternals(File nodeInternDir, ExecutionMonitor exec) {
		// TODO Auto-generated method stub

	}

	@Override
	protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {
	
		// TODO 
		
		
		
		return null;
	}

}
