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
package org.knime.knip.core.ops.spotdetection;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import net.imglib2.FinalInterval;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.gauss3.SeparableSymmetricConvolution;
import net.imglib2.converter.Converters;
import net.imglib2.converter.RealFloatConverter;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.ops.img.BinaryOperationAssignment;
import net.imglib2.ops.operation.SubsetOperations;
import net.imglib2.ops.operation.UnaryOperation;
import net.imglib2.ops.operation.real.binary.RealSubtract;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Op to create a a trous wavelet decomposition of a 2D image as stack on the z axis.
 *
 *
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 */
public class ATrousWaveletCreator<T extends RealType<T>> implements
        UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> {

    private final Integer[] m_skipLevels;
    private ExecutorService m_service;

    public ATrousWaveletCreator(final ExecutorService service) {
        m_skipLevels = new Integer[]{};
        m_service = service;
    }

    /**
     * @param skipLevels at these indices the output will only contain temporary results and not the wavelet plane. This
     *            can be used to speed up the computations if not all wavelet planes are required.
     */
    public ATrousWaveletCreator(final Integer... skipLevels) {
        m_skipLevels = skipLevels;
    }

    @Override
    public UnaryOperation<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> copy() {
        return new ATrousWaveletCreator<T>(m_skipLevels);
    }

    /**
     * Computes a a trous wavelet representation with output.z - 1 wavelet planes W_i and one residual plane A_z.
     * Depending on the {@link #m_skipLevels} parameter some of the planes may contain only temporary results.<br>
     * <br>
     * The image can be recomputed as follows A_z + W_i | i � (0..z-1) <br>
     * sum(output_0 .. output_z-1) + output_z<br>
     * This works only if no skip levels are specified.
     */
    @Override
    public RandomAccessibleInterval<FloatType> compute(final RandomAccessibleInterval<T> input,
                                                       final RandomAccessibleInterval<FloatType> output) {

        if ((input.numDimensions() != 2) || (output.numDimensions() != 3)) {
            throw new RuntimeException(new IncompatibleTypeException(input,
                    "input has to be a 2D image, output a 3D image"));
        }

        if (output.dimension(2) < 2) {
            throw new RuntimeException(new IncompatibleTypeException(input,
                    "output requires at least 2 XY planes i.e.  {[0..sizeX], [0..sizeY], [0..a] | a >= 1}"));
        }

        final long shortSize = Math.min(input.dimension(0), input.dimension(1));
        if (shortSize < getMinSize(output.dimension(2) - 1)) {
            throw new RuntimeException("image to small (to many wavelet levels)");
        }

        try {
            return createCoefficients(input, output);
        } catch (final IncompatibleTypeException e) {
            throw new RuntimeException("Separable Symmetric Convolution failed", e);
        }
    }

    private RandomAccessibleInterval<FloatType>
            createCoefficients(final RandomAccessibleInterval<T> input2D,
                               final RandomAccessibleInterval<FloatType> outputStack) throws IncompatibleTypeException {

        final long[] min = new long[]{input2D.min(0), input2D.min(1), -1};
        final long[] max = new long[]{input2D.max(0), input2D.max(1), -1};

        RandomAccessible<FloatType> extendedInput =
                Views.extendMirrorDouble(Converters.convert(input2D, new RealFloatConverter<T>(), new FloatType()));

        // create output.z-1 wavelets
        for (int i = 0; i < (outputStack.dimension(2) - 1); i++) {
            // select slice from output
            min[2] = (i + 1);
            max[2] = (i + 1);
            final FinalInterval outputSlice = new FinalInterval(min, max);
            //

            final double[][] halfKernels = createHalfKernel(i);
            SeparableSymmetricConvolution.convolve(halfKernels, extendedInput,
                                                   SubsetOperations.subsetview(outputStack, outputSlice), m_service);

            extendedInput = Views.extendMirrorDouble(SubsetOperations.subsetview(outputStack, outputSlice));

        }

        // now subtract the appropriate levels

        final BinaryOperationAssignment<FloatType, FloatType, FloatType> substract =
                new BinaryOperationAssignment<FloatType, FloatType, FloatType>(
                        new RealSubtract<FloatType, FloatType, FloatType>());
        IterableInterval<FloatType> in1 =
                Views.iterable(Converters.convert(input2D, new RealFloatConverter<T>(), new FloatType()));

        for (int i = 0; i < (outputStack.dimension(2) - 1); i++) {
            if (!Arrays.asList(m_skipLevels).contains(Integer.valueOf(i))) {
                // out
                min[2] = i;
                max[2] = i;
                final FinalInterval out = new FinalInterval(min, max);
                // in1
                min[2] = (i + 1);
                max[2] = (i + 1);
                final FinalInterval sub = new FinalInterval(min, max);
                //
                substract.compute(in1, Views.iterable(SubsetOperations.subsetview(outputStack, sub)),
                                  Views.iterable(SubsetOperations.subsetview(outputStack, out)));
                //
            }
            min[2] = (i + 1);
            max[2] = (i + 1);
            final FinalInterval tmp = new FinalInterval(min, max);
            in1 = Views.iterable(SubsetOperations.subsetview(outputStack, tmp));
        }

        return outputStack;
    }

    private double[][] createHalfKernel(final int level) {
        final double[][] ret = new double[2][];

        // kernel constants
        final float[] numbers = new float[]{3.0f / 8.0f, 1.0f / 4.0f, 1.0f / 16.0f};

        final long zeroTap = (long)Math.pow(2.0, level) - 1;
        final int length = ((int)((zeroTap * 4) + 4) / 2) + 1;

        ret[0] = new double[length];
        ret[1] = new double[length];

        int index = 0;
        for (final float number : numbers) {
            ret[0][index] = number;
            ret[1][index] = number;
            index += (zeroTap + 1);
        }

        return ret;
    }

    private long getMinSize(final long levels) {
        return 5 + ((long)(Math.pow(2, levels - 1)) * 4);
    }
}
