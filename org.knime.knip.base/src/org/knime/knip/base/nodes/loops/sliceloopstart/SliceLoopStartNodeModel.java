/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.knip.base.nodes.loops.sliceloopstart;

import java.io.File;
import java.io.IOException;

import net.imglib2.Interval;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.ImgView;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.MetadataUtil;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

import org.knime.base.node.flowvariable.tablerowtovariable.TableToVariableNodeModel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.workflow.LoopStartNodeTerminator;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;

/**
 * @author dietzc, University of Konstanz
 */
public class SliceLoopStartNodeModel<T extends RealType<T> & NativeType<T>> extends TableToVariableNodeModel implements
        LoopStartNodeTerminator {

    private SettingsModelDimSelection m_dimSelection = createDimSelection();

    private SettingsModelInteger m_chunkSize = createChunkSizeModel();

    private ImgPlus<T> m_curr = null;

    private int m_imgIndex = -1;

    private int m_currIdx = -1;

    private CloseableRowIterator m_iterator;

    private Interval[] m_intervals;

    private boolean imgTerminated;

    private boolean isAllTerminated;

    private ImgPlusCellFactory m_imgPlusCellFactory;

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {

        // Hier: Gebe die Definition/Spezifikation von der resultierenden Tabelle zurück (layout der tabelle).
        // Das ist natürlich abhängig von den selektieren Spalten, d.h. falls du Labeling und Img selektiert hast, kommt hinten auch labeling bzw. img wieder raus.
        return new DataTableSpec[]{createResSpec()};
    }

    /**
     * @return
     */
    protected static SettingsModelInteger createChunkSizeModel() {
        return new SettingsModelInteger("chunk_size", 1);
    }

    /**
     * @return
     */
    protected static SettingsModelDimSelection createDimSelection() {
        return new SettingsModelDimSelection("dim_selection", "X", "Y");
    }

    /**
     * @return
     */
    private DataTableSpec createResSpec() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
            throws Exception {

        if (m_iterator == null) {
            final BufferedDataTable inTable = inData[0];
            m_iterator = inTable.iterator();
            m_imgPlusCellFactory = new ImgPlusCellFactory(exec);
        }

        if (m_curr == null) {
            m_curr = ((ImgPlusValue<T>)m_iterator.next().getCell(m_imgIndex)).getImgPlus();
        }

        if (m_intervals == null) {
            m_intervals = m_dimSelection.getIntervals(m_curr, m_curr);
            m_currIdx = 0;
        }

        // das ist deine input table.

        final BufferedDataContainer container = exec.createDataContainer(createResSpec());

        for (int i = m_currIdx; i < (Math.min(i + m_chunkSize.getIntValue(), m_intervals.length)); i++) {
            container.addRowToTable(new DefaultRow("Slice " + i, m_imgPlusCellFactory
                    .createCell(getImgPlusAsView(m_curr, m_intervals[i], new ArrayImgFactory<T>()))));
        }

        m_currIdx = (Math.min(m_currIdx + m_chunkSize.getIntValue(), m_intervals.length));

        if (m_currIdx == m_intervals.length) {
            imgTerminated = true;
            isAllTerminated = !m_iterator.hasNext();

            if (!isAllTerminated) {
                m_curr = null;
                m_intervals = null;
            }
        }

        // fülle container mit slices. je nach dem wieviele slices der user eingestellt hat.

        container.close();

        return new BufferedDataTable[]{container.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // Setze alles zurück auf null
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // Nothing to do here
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_dimSelection.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimSelection.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_dimSelection.loadSettingsFrom(settings);
    }

    public boolean terminateImg() {
        return imgTerminated;
    }

    @Override
    public boolean terminateLoop() {
        return isAllTerminated;
    }

    private ImgPlus<T> getImgPlusAsView(final ImgPlus<T> in, final Interval i, final ImgFactory<T> fac) {
        ImgPlus<T> imgPlus = in;
        while (imgPlus.getImg() instanceof ImgPlus) {
            imgPlus = (ImgPlus<T>)imgPlus.getImg();
        }

        ImgPlus<T> imgPlusView = new ImgPlus<T>(new ImgView<T>(SubsetOperations.subsetview(imgPlus.getImg(), i), fac));
        MetadataUtil.copyAndCleanImgPlusMetadata(i, in, imgPlusView);

        return imgPlusView;
    }
}
