package org.knime.knip.io.nodes.imgreader3.readfromdialog;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.knip.cellviewer.CellNodeView;

import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class ImgReaderTable2NodeFactory<T extends NativeType<T> & RealType<T>>
		extends NodeFactory<ImgReaderTable2NodeModel<T>> {

	@Override
	public ImgReaderTable2NodeModel<T> createNodeModel() {
		return new ImgReaderTable2NodeModel<>();
	}

	@Override
	protected int getNrNodeViews() {
		return 1;
	}

	@Override
	public NodeView<ImgReaderTable2NodeModel<T>> createNodeView(int viewIndex, ImgReaderTable2NodeModel<T> nodeModel) {
		return new CellNodeView<>(nodeModel);
	}

	@Override
	protected boolean hasDialog() {
		return true;
	}

	@Override
	protected NodeDialogPane createNodeDialogPane() {
		return new ImgReaderTable2NodeDialog();
	}

}
