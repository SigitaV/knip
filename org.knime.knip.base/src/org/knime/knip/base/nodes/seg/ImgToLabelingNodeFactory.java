/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.base.nodes.seg;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingCellFactory;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.exceptions.KNIPRuntimeException;
import org.knime.knip.base.node.ValueToCellNodeDialog;
import org.knime.knip.base.node.ValueToCellNodeFactory;
import org.knime.knip.base.node.ValueToCellNodeModel;
import org.knime.knip.core.KNIPGateway;
import org.knime.knip.core.awt.labelingcolortable.DefaultLabelingColorTable;
import org.knime.knip.core.data.img.DefaultLabelingMetadata;
import org.knime.knip.core.util.MinimaUtils;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelingMapping;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.view.Views;

/**
 * NodeFactory for the Lab2Table Node
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ImgToLabelingNodeFactory<T extends IntegerType<T> & NativeType<T>, L>
        extends ValueToCellNodeFactory<ImgPlusValue<T>> {

    private static SettingsModelInteger createBackgroundValueModel() {
        return new SettingsModelInteger("background value", 0);
    }

    private static SettingsModelString createLabelingMapColModel() {
        return new SettingsModelString("labeling_mapping", "");
    }

    private static SettingsModelBoolean createSetBackgroundModel() {
        return new SettingsModelBoolean("has_background", true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ValueToCellNodeDialog<ImgPlusValue<T>> createNodeDialog() {
        return new ValueToCellNodeDialog<ImgPlusValue<T>>() {

            /**
             * {@inheritDoc}
             */
            @SuppressWarnings("unchecked")
            @Override
            public void addDialogComponents() {

                final SettingsModelString labCol = createLabelingMapColModel();

                addDialogComponent("Labeling Settings", "", new DialogComponentColumnNameSelection(labCol,
                        "Labels from ...", 0, false, true, LabelingValue.class));

                final SettingsModelBoolean setBackground = createSetBackgroundModel();
                final SettingsModelInteger backgroundValue = createBackgroundValueModel();
                setBackground.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(final ChangeEvent arg0) {
                        backgroundValue.setEnabled(setBackground.getBooleanValue());

                    }
                });

                addDialogComponent("Options", "Background",
                                   new DialogComponentBoolean(setBackground, "Use background value as background?"));

                addDialogComponent("Options", "Background",
                                   new DialogComponentNumber(backgroundValue, "Background value", 1));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected String getDefaultSuffixForAppend() {
                return "_itl";
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<L>> createNodeModel() {
        return new ValueToCellNodeModel<ImgPlusValue<T>, LabelingCell<L>>() {

            private final NodeLogger LOGGER = NodeLogger.getLogger(this.getClass());

            private final SettingsModelInteger m_background = createBackgroundValueModel();

            private LabelingCellFactory m_labCellFactory;

            private int m_labelingMappingColIdx = -1;

            private LabelingMapping<L> m_currentLabelingMapping;

            private final SettingsModelString m_labelingMappingColumn = createLabelingMapColModel();

            private final SettingsModelBoolean m_setBackground = createSetBackgroundModel();

            @Override
            protected void addSettingsModels(final List<SettingsModel> settingsModels) {
                settingsModels.add(m_labelingMappingColumn);
                settingsModels.add(m_background);
                settingsModels.add(m_setBackground);
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void prepareExecute(final ExecutionContext exec) {
                m_labCellFactory = new LabelingCellFactory(exec);
            }

            /**
             * {@inheritDoc}
             *
             * @throws IOException
             */
            @Override
            protected LabelingCell<L> compute(final ImgPlusValue<T> cellValue) throws IOException {

                final ImgPlus<T> fromCell = cellValue.getImgPlus();

                if (!(fromCell.firstElement() instanceof IntegerType)) {
                    throw new KNIPRuntimeException(
                            "Only Images of type IntegerType can be converted into a Labeling. Use the converter to convert your Image e.g. to ShortType, IntType, ByteType or BitType.");
                }

                final ImgPlus<T> zeroMinFromCell = MinimaUtils.getZeroMinImgPlus(fromCell);

                ImgLabeling<L, ?> res;

                T type =  zeroMinFromCell.firstElement().createVariable();
                int maxNumLabelSets = (int) (type.getMaxValue() - type.getMinValue());
                if (!m_setBackground.getBooleanValue()) {
                    //if the background is not set to a certain value, we need to reserve an extra label for the additional background
                    maxNumLabelSets++;
                }

                if (m_currentLabelingMapping == null) {
                    final ImgLabeling<Integer, ?> tmp = KNIPGateway.ops().create()
                            .imgLabeling(zeroMinFromCell, null, fromCell.factory(), maxNumLabelSets);

                    final boolean setBGValue = m_setBackground.getBooleanValue();
                    final Cursor<T> cursor = zeroMinFromCell.cursor();
                    final Cursor<LabelingType<Integer>> labType = tmp.cursor();
                    while (cursor.hasNext()) {
                        int val = cursor.next().getInteger();
                        if (!setBGValue || val != m_background.getIntValue()) {
                            labType.next().add(val);
                        } else {
                            labType.fwd();
                        }
                    }
                    res = (ImgLabeling<L, ?>)tmp;
                } else {
                    final ImgLabeling<String, ?> tmp = KNIPGateway.ops().create()
                            .imgLabeling(zeroMinFromCell, null, fromCell.factory(), maxNumLabelSets);

                    final boolean setBGValue = m_setBackground.getBooleanValue();
                    final Cursor<T> cursor = zeroMinFromCell.cursor();
                    final Cursor<LabelingType<String>> labType = tmp.cursor();
                    while (cursor.hasNext()) {
                        int index = cursor.next().getInteger();
                        if (setBGValue && index == m_background.getIntValue()) {
                            //do nothing
                            labType.fwd();
                        } else if (index >= 0 && index < m_currentLabelingMapping.numSets()) {
                            Set<String> labels = m_currentLabelingMapping.labelsAtIndex(index).stream()
                                    .map(o -> o.toString()).collect(Collectors.toSet());
                            if (!setBGValue || index != m_background.getIntValue()) {
                                labType.next().addAll(labels);
                            } else {
                                labType.fwd();
                            }
                        } else {
                            //no labels found in the labeling mapping for the given index
                            //add the index (as string) as label
                            labType.next().add(String.valueOf(index));
                        }
                    }
                    res = (ImgLabeling<L, ?>)tmp;
                }
                return m_labCellFactory.createCell(MinimaUtils.getTranslatedLabeling(fromCell, res),
                                                   new DefaultLabelingMetadata(cellValue.getMetadata(),
                                                           cellValue.getMetadata(), cellValue.getMetadata(),
                                                           new DefaultLabelingColorTable()));
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected void computeDataRow(final DataRow row) {
                if (m_labelingMappingColIdx != -1) {
                    if (row.getCell(m_labelingMappingColIdx).isMissing()) {
                        LOGGER.warn("Labeling is missing. No labeling mapping can be used.");
                        m_currentLabelingMapping = null;
                        return;
                    }
                    IterableInterval<LabelingType<L>> labeling =
                            Views.iterable(((LabelingValue<L>)row.getCell(m_labelingMappingColIdx)).getLabeling());
                    if (!(labeling.firstElement().getMapping().getLabels().iterator().next() instanceof String)) {
                        LOGGER.warn("Labeling for the Labeling Mapping not compatible with String. Labeling Mapping ignored for row  "
                                + row.getKey());
                        m_currentLabelingMapping = null;

                    } else {
                        m_currentLabelingMapping = labeling.firstElement().getMapping();
                    }

                } else {
                    m_currentLabelingMapping = null;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
                BufferedDataTable inTable = (BufferedDataTable)inObjects[0];
                m_labelingMappingColIdx = getLabelingMappingColIdx(inTable.getDataTableSpec());
                if (m_labelingMappingColIdx == -1) {
                    m_currentLabelingMapping = null;
                }
                return super.execute(inObjects, exec);
            }

            private int getLabelingMappingColIdx(final DataTableSpec inSpec) {
                return inSpec.findColumnIndex(m_labelingMappingColumn.getStringValue());
            }

        };
    }
}
