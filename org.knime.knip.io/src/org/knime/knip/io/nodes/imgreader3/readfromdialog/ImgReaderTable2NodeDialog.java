package org.knime.knip.io.nodes.imgreader3.readfromdialog;

import org.knime.core.data.uri.URIDataValue;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.core.util.EnumUtils;
import org.knime.knip.io.nodes.imgreader3.AbstractImgReaderNodeDialog;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettingsModels;
import org.knime.knip.io.nodes.imgreader3.ImgReaderSettingsModels.ColumnCreationMode;

public class ImgReaderTable2NodeDialog extends AbstractImgReaderNodeDialog {

	private final SettingsModelString fileURIColumnModel;

	@SuppressWarnings("unchecked")
	public ImgReaderTable2NodeDialog() {
		super();

		createNewGroup("File Input Column");
		fileURIColumnModel = ImgReaderSettingsModels.createFileURIColumnModel();

		addDialogComponent(new DialogComponentColumnNameSelection(fileURIColumnModel, "File URI column in input table",
				1, true, false, URIDataValue.class));
		closeCurrentGroup();
		
		// insert default gui
		super.buildRemainingGUI();

		createNewTab("Column Settings");
		SettingsModelString colCreationModeModel = ImgReaderSettingsModels.createColumnCreationModeModel();
		addDialogComponent(new DialogComponentStringSelection(colCreationModeModel, "Column Creation Mode",
				EnumUtils.getStringListFromToString(ColumnCreationMode.values())));

		SettingsModelString columnSuffixModel = ImgReaderSettingsModels.createColumnSuffixNodeModel();
		addDialogComponent(new DialogComponentString(columnSuffixModel, "Column Suffix"));
	}

}
