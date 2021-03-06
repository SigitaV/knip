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
package org.knime.knip.core.ui.imgviewer.events;

import org.knime.knip.core.ui.event.KNIPEvent;

/**
 *
 * @author Andreas Burger, University of Konstanz
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 */
public class TablePositionEvent implements KNIPEvent {

    private final int m_width;

    private final int m_height;

    private final int m_x;

    private final int m_y;

    private final String m_colName;

    private final String m_rowName;

    public TablePositionEvent(final int width, final int height, final int x, final int y) {
        this(width, height, x, y, "", "");
    }

    public TablePositionEvent(final int width, final int height, final int x, final int y, final String colName, final String rowName) {
        m_width = width;
        m_height = height;
        m_x = x;
        m_y = y;
        m_colName = colName;
        m_rowName = rowName;
    }

    /**
     * @return the m_width
     */
    public int getwidth() {
        return m_width;
    }

    /**
     * @return the m_height
     */
    public int getheight() {
        return m_height;
    }

    /**
     * @return the m_x
     */
    public int getx() {
        return m_x;
    }

    /**
     * @return the m_y
     */
    public int gety() {
        return m_y;
    }

    public String getColumnName(){
        return m_colName;
    }

    public String getRowName(){
        return m_rowName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionPriority getExecutionOrder() {
        // TODO Auto-generated method stub
        return ExecutionPriority.LOW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <E extends KNIPEvent> boolean isRedundant(final E thatEvent) {
        if (thatEvent instanceof TablePositionEvent) {
            if (((TablePositionEvent)thatEvent).m_x == m_x && m_y == ((TablePositionEvent)thatEvent).m_y) {
                return true;
            }
        }

        return false;
    }

}
