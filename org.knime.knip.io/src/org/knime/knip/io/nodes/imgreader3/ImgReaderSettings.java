package org.knime.knip.io.nodes.imgreader3;

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleRange;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.node.nodesettings.SettingsModelSubsetSelection2;

public class ImgReaderSettings {

	private ImgReaderSettings() {
		// Utility Class
	}

	/**
	 * The available factory types available for selection.
	 */
	public static final String[] IMG_FACTORIES = new String[] { "Array Image Factory", "Planar Image Factory",
			"Cell Image Factory" };

	/**
	 * @return Model to store the check file format option.
	 */
	public static final SettingsModelBoolean createCheckFileFormatModel() {
		return new SettingsModelBoolean("check_file_format", true);
	}

	public static final SettingsModelBoolean createIsGroupFilesModel() {
		return new SettingsModelBoolean("group_files", true);
	}

	/**
	 * @return Model to store the OME_XML-metadata column option.
	 */
	public static final SettingsModelBoolean createAppendOmexmlColModel() {
		return new SettingsModelBoolean("xmlcolumns", false);
	}

	/**
	 * @return Model for the settings holding selected image planes.
	 */
	public static final SettingsModelSubsetSelection2 createPlaneSelectionModel() {
		return new SettingsModelSubsetSelection2("plane_selection");
	}

	/**
	 * @return Model to store whether all series should be read
	 */
	public static final SettingsModelBoolean createReadAllSeriesModel() {
		return new SettingsModelBoolean("read_all_series", true);
	}

	public static SettingsModelDoubleRange createSeriesSelectionRangeModel() {
		return new SettingsModelDoubleRange("series_range_selection", 0, Short.MAX_VALUE);
	}

	/**
	 * @return Model to store the factory used to create the images
	 */
	public static SettingsModelString createImgFactoryModel() {
		return new SettingsModelString("img_factory", IMG_FACTORIES[0]);
	}

	/**
	 * @return Model to store the metadata mode.
	 */
	public static SettingsModelString createMetaDataModeModel() {
		return new SettingsModelString("metadata_mode", MetadataMode.NO_METADATA.toString());
	}

	/**
	 * @return Model to store whether to read all meta data or not.
	 */
	public static SettingsModelBoolean createReadAllMetaDataModel() {
		return new SettingsModelBoolean("read_all_metadata", false);
	}

	public static SettingsModelString createPixelTypeModel() {
		return new SettingsModelString("m_pixeltype", "< Automatic >");
	}

}
