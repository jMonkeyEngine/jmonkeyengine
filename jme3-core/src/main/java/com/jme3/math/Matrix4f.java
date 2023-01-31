/*
 * Copyright (c) 2009-2023 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.math;

import com.jme3.export.*;
import com.jme3.util.BufferUtils;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * A 4x4 matrix composed of 16 single-precision elements, used to represent
 * linear or perspective transformations of 3-D coordinates.
 *
 * <p>The rightmost column (column 3) stores the translation vector. Element
 * numbering is (row,column), so m03 is the row 0, column 3, which is the X
 * translation.
 *
 * <p>Methods with names ending in "Local" modify the current instance. They are
 * used to avoid creating garbage.
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Matrix4f implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private static final Logger logger = Logger.getLogger(Matrix4f.class.getName());
    /**
     * The element in row 0, column 0.
     */
    public float m00;
    /**
     * The element in row 0, column 1.
     */
    public float m01;
    /**
     * The element in row 0, column 2.
     */
    public float m02;
    /**
     * The element in row 0, column 3 (the X translation).
     */
    public float m03;
    /**
     * The element in row 1, column 0.
     */
    public float m10;
    /**
     * The element in row 1, column 1.
     */
    public float m11;
    /**
     * The element in row 1, column 2.
     */
    public float m12;
    /**
     * The element in row 1, column 3 (the Y translation).
     */
    public float m13;
    /**
     * The element in row 2, column 0.
     */
    public float m20;
    /**
     * The element in row 2, column 1.
     */
    public float m21;
    /**
     * The element in row 2, column 2.
     */
    public float m22;
    /**
     * The element in row 2, column 3 (the Z translation).
     */
    public float m23;
    /**
     * The element in row 3, column 0.
     */
    public float m30;
    /**
     * The element in row 3, column 1.
     */
    public float m31;
    /**
     * The element in row 3, column 2.
     */
    public float m32;
    /**
     * The element in row 3, column 3.
     */
    public float m33;
    /**
     * an instance of the zero matrix (all elements = 0)
     */
    public static final Matrix4f ZERO = new Matrix4f(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    /**
     * an instance of the identity matrix (diagonals = 1, other elements = 0)
     */
    public static final Matrix4f IDENTITY = new Matrix4f();

    /**
     * Instantiates an identity matrix (diagonals = 1, other elements = 0).
     */
    public Matrix4f() {
        loadIdentity();
    }

    /**
     * Create a <code>Matrix4f</code> with the specified element values.
     *
     * @param m00 the desired value for row 0, column 0
     * @param m01 the desired value for row 0, column 1
     * @param m02 the desired value for row 0, column 2
     * @param m03 the desired value for row 0, column 3
     * @param m10 the desired value for row 1, column 0
     * @param m11 the desired value for row 1, column 1
     * @param m12 the desired value for row 1, column 2
     * @param m13 the desired value for row 1, column 3
     * @param m20 the desired value for row 2, column 0
     * @param m21 the desired value for row 2, column 1
     * @param m22 the desired value for row 2, column 2
     * @param m23 the desired value for row 2, column 3
     * @param m30 the desired value for row 3, column 0
     * @param m31 the desired value for row 3, column 1
     * @param m32 the desired value for row 3, column 2
     * @param m33 the desired value for row 3, column 3
     */
    public Matrix4f(float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    /**
     * Create a <code>Matrix4f</code> from the specified array.
     *
     * @param array the source array: 16 floats in column-major order
     * (translation in elements 12, 13, and 14)
     */
    public Matrix4f(float[] array) {
        set(array, false);
    }

    /**
     * Create a <code>Matrix4f</code> that duplicates the specified matrix. If
     * null is specified, the new matrix is initialized to identity (diagonals =
     * 1, other elements = 0).
     *
     * @param mat the source matrix (unaffected, may be null)
     */
    public Matrix4f(Matrix4f mat) {
        copy(mat);
    }

    /**
     * Copy all elements of the specified matrix to this matrix. If null is
     * specified, load identity (diagonals = 1, other elements = 0).
     *
     * @param matrix the source matrix (may be null, unaffected)
     */
    public void copy(Matrix4f matrix) {
        if (null == matrix) {
            loadIdentity();
        } else {
            m00 = matrix.m00;
            m01 = matrix.m01;
            m02 = matrix.m02;
            m03 = matrix.m03;
            m10 = matrix.m10;
            m11 = matrix.m11;
            m12 = matrix.m12;
            m13 = matrix.m13;
            m20 = matrix.m20;
            m21 = matrix.m21;
            m22 = matrix.m22;
            m23 = matrix.m23;
            m30 = matrix.m30;
            m31 = matrix.m31;
            m32 = matrix.m32;
            m33 = matrix.m33;
        }
    }

    public void fromFrame(Vector3f location, Vector3f direction, Vector3f up, Vector3f left) {
        TempVars vars = TempVars.get();
        try {
            Vector3f fwdVector = vars.vect1.set(direction);
            Vector3f leftVector = vars.vect2.set(fwdVector).crossLocal(up);
            Vector3f upVector = vars.vect3.set(leftVector).crossLocal(fwdVector);

            m00 = leftVector.x;
            m01 = leftVector.y;
            m02 = leftVector.z;
            m03 = -leftVector.dot(location);

            m10 = upVector.x;
            m11 = upVector.y;
            m12 = upVector.z;
            m13 = -upVector.dot(location);

            m20 = -fwdVector.x;
            m21 = -fwdVector.y;
            m22 = -fwdVector.z;
            m23 = fwdVector.dot(location);

            m30 = 0f;
            m31 = 0f;
            m32 = 0f;
            m33 = 1f;
        } finally {
            vars.release();
        }
    }

    /**
     * Copy all elements to a float array, in row-major order.
     *
     * @param matrix the destination array (not null, length=16)
     */
    public void get(float[] matrix) {
        get(matrix, true);
    }

    /**
     * Copy all elements to a float array.
     *
     * @param matrix the destination array (not null, length=16)
     * @param rowMajor true to store in row-major order, false to store in
     * column-major order
     */
    public void get(float[] matrix, boolean rowMajor) {
        if (matrix.length != 16) {
            throw new IllegalArgumentException(
                    "Array must be of size 16.");
        }

        if (rowMajor) {
            matrix[0] = m00;
            matrix[1] = m01;
            matrix[2] = m02;
            matrix[3] = m03;
            matrix[4] = m10;
            matrix[5] = m11;
            matrix[6] = m12;
            matrix[7] = m13;
            matrix[8] = m20;
            matrix[9] = m21;
            matrix[10] = m22;
            matrix[11] = m23;
            matrix[12] = m30;
            matrix[13] = m31;
            matrix[14] = m32;
            matrix[15] = m33;
        } else {
            matrix[0] = m00;
            matrix[4] = m01;
            matrix[8] = m02;
            matrix[12] = m03;
            matrix[1] = m10;
            matrix[5] = m11;
            matrix[9] = m12;
            matrix[13] = m13;
            matrix[2] = m20;
            matrix[6] = m21;
            matrix[10] = m22;
            matrix[14] = m23;
            matrix[3] = m30;
            matrix[7] = m31;
            matrix[11] = m32;
            matrix[15] = m33;
        }
    }

    /**
     * Retrieve the element at the specified position.
     *
     * @param i the row index of the element to retrieve (0, 1, 2, or 3)
     * @param j the column index of the element to retrieve (0, 1, 2, or 3)
     * @return the value at (i, j)
     * @throws IllegalArgumentException if either index is invalid.
     */
    @SuppressWarnings("fallthrough")
    public float get(int i, int j) {
        switch (i) {
            case 0:
                switch (j) {
                    case 0:
                        return m00;
                    case 1:
                        return m01;
                    case 2:
                        return m02;
                    case 3:
                        return m03;
                }
            case 1:
                switch (j) {
                    case 0:
                        return m10;
                    case 1:
                        return m11;
                    case 2:
                        return m12;
                    case 3:
                        return m13;
                }
            case 2:
                switch (j) {
                    case 0:
                        return m20;
                    case 1:
                        return m21;
                    case 2:
                        return m22;
                    case 3:
                        return m23;
                }
            case 3:
                switch (j) {
                    case 0:
                        return m30;
                    case 1:
                        return m31;
                    case 2:
                        return m32;
                    case 3:
                        return m33;
                }
        }

        logger.warning("Invalid matrix index.");
        throw new IllegalArgumentException("Invalid indices into matrix.");
    }

    /**
     * Copy the specified column to a new float array.
     *
     * @param i the index of the column to copy (0, 1, 2, or 3)
     * @return a new array with length=4
     */
    public float[] getColumn(int i) {
        return getColumn(i, null);
    }

    /**
     * Copy the specified column to a float array.
     *
     * @param i the index of the column to copy (0, 1, 2, or 3)
     * @param store storage for the result (modified) or null to create a new
     * array
     * @return either store or a new array with length=4
     */
    public float[] getColumn(int i, float[] store) {
        if (store == null) {
            store = new float[4];
        }
        switch (i) {
            case 0:
                store[0] = m00;
                store[1] = m10;
                store[2] = m20;
                store[3] = m30;
                break;
            case 1:
                store[0] = m01;
                store[1] = m11;
                store[2] = m21;
                store[3] = m31;
                break;
            case 2:
                store[0] = m02;
                store[1] = m12;
                store[2] = m22;
                store[3] = m32;
                break;
            case 3:
                store[0] = m03;
                store[1] = m13;
                store[2] = m23;
                store[3] = m33;
                break;
            default:
                logger.warning("Invalid column index.");
                throw new IllegalArgumentException("Invalid column index. " + i);
        }
        return store;
    }

    /**
     * Load the specified column from the specified array.
     *
     * @param i the index of the column to fill (0, 1, 2, or 3)
     * @param column the source array (unaffected) or null
     */
    public void setColumn(int i, float[] column) {

        if (column == null) {
            logger.warning("Column is null. Ignoring.");
            return;
        }
        switch (i) {
            case 0:
                m00 = column[0];
                m10 = column[1];
                m20 = column[2];
                m30 = column[3];
                break;
            case 1:
                m01 = column[0];
                m11 = column[1];
                m21 = column[2];
                m31 = column[3];
                break;
            case 2:
                m02 = column[0];
                m12 = column[1];
                m22 = column[2];
                m32 = column[3];
                break;
            case 3:
                m03 = column[0];
                m13 = column[1];
                m23 = column[2];
                m33 = column[3];
                break;
            default:
                logger.warning("Invalid column index.");
                throw new IllegalArgumentException("Invalid column index. " + i);
        }
    }

    /**
     * Store the specified value at the specified position.
     *
     * @param i the row index of the element to set (0, 1, 2, or 3)
     * @param j the column index of the element to set (0, 1, 2, or 3)
     * @param value the value for element (i, j)
     * @throws IllegalArgumentException if either index is invalid.
     */
    @SuppressWarnings("fallthrough")
    public void set(int i, int j, float value) {
        switch (i) {
            case 0:
                switch (j) {
                    case 0:
                        m00 = value;
                        return;
                    case 1:
                        m01 = value;
                        return;
                    case 2:
                        m02 = value;
                        return;
                    case 3:
                        m03 = value;
                        return;
                }
            case 1:
                switch (j) {
                    case 0:
                        m10 = value;
                        return;
                    case 1:
                        m11 = value;
                        return;
                    case 2:
                        m12 = value;
                        return;
                    case 3:
                        m13 = value;
                        return;
                }
            case 2:
                switch (j) {
                    case 0:
                        m20 = value;
                        return;
                    case 1:
                        m21 = value;
                        return;
                    case 2:
                        m22 = value;
                        return;
                    case 3:
                        m23 = value;
                        return;
                }
            case 3:
                switch (j) {
                    case 0:
                        m30 = value;
                        return;
                    case 1:
                        m31 = value;
                        return;
                    case 2:
                        m32 = value;
                        return;
                    case 3:
                        m33 = value;
                        return;
                }
        }

        logger.warning("Invalid matrix index.");
        throw new IllegalArgumentException("Invalid indices into matrix.");
    }

    /**
     * Load all elements from the specified 4x4 array.
     *
     * @param matrix the source array (not null, unaffected)
     * @throws IllegalArgumentException if the source array isn't 4x4.
     */
    public void set(float[][] matrix) {
        if (matrix.length != 4 || matrix[0].length != 4) {
            throw new IllegalArgumentException(
                    "Array must be of size 16.");
        }

        m00 = matrix[0][0];
        m01 = matrix[0][1];
        m02 = matrix[0][2];
        m03 = matrix[0][3];
        m10 = matrix[1][0];
        m11 = matrix[1][1];
        m12 = matrix[1][2];
        m13 = matrix[1][3];
        m20 = matrix[2][0];
        m21 = matrix[2][1];
        m22 = matrix[2][2];
        m23 = matrix[2][3];
        m30 = matrix[3][0];
        m31 = matrix[3][1];
        m32 = matrix[3][2];
        m33 = matrix[3][3];
    }

    /**
     * Load the specified element values.
     *
     * @param m00 the desired value for row 0, column 0
     * @param m01 the desired value for row 0, column 1
     * @param m02 the desired value for row 0, column 2
     * @param m03 the desired value for row 0, column 3
     * @param m10 the desired value for row 1, column 0
     * @param m11 the desired value for row 1, column 1
     * @param m12 the desired value for row 1, column 2
     * @param m13 the desired value for row 1, column 3
     * @param m20 the desired value for row 2, column 0
     * @param m21 the desired value for row 2, column 1
     * @param m22 the desired value for row 2, column 2
     * @param m23 the desired value for row 2, column 3
     * @param m30 the desired value for row 3, column 0
     * @param m31 the desired value for row 3, column 1
     * @param m32 the desired value for row 3, column 2
     * @param m33 the desired value for row 3, column 3
     */
    public void set(float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {

        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
    }

    /**
     * Copy all elements of the specified matrix to this matrix.
     *
     * @param matrix the source matrix (not null, unaffected)
     * @return this (modified)
     */
    public Matrix4f set(Matrix4f matrix) {
        m00 = matrix.m00;
        m01 = matrix.m01;
        m02 = matrix.m02;
        m03 = matrix.m03;
        m10 = matrix.m10;
        m11 = matrix.m11;
        m12 = matrix.m12;
        m13 = matrix.m13;
        m20 = matrix.m20;
        m21 = matrix.m21;
        m22 = matrix.m22;
        m23 = matrix.m23;
        m30 = matrix.m30;
        m31 = matrix.m31;
        m32 = matrix.m32;
        m33 = matrix.m33;
        return this;
    }

    /**
     * Load all elements from the specified array.
     *
     * @param matrix the source array, in row-major order (not null, length=16,
     * unaffected)
     */
    public void set(float[] matrix) {
        set(matrix, true);
    }

    /**
     * Load all elements from the specified array.
     *
     * @param matrix the source array (not null, length=16, unaffected)
     * @param rowMajor true if the source array is in row-major order, false if
     * it's in column-major order
     */
    public void set(float[] matrix, boolean rowMajor) {
        if (matrix.length != 16) {
            throw new IllegalArgumentException(
                    "Array must be of size 16.");
        }

        if (rowMajor) {
            m00 = matrix[0];
            m01 = matrix[1];
            m02 = matrix[2];
            m03 = matrix[3];
            m10 = matrix[4];
            m11 = matrix[5];
            m12 = matrix[6];
            m13 = matrix[7];
            m20 = matrix[8];
            m21 = matrix[9];
            m22 = matrix[10];
            m23 = matrix[11];
            m30 = matrix[12];
            m31 = matrix[13];
            m32 = matrix[14];
            m33 = matrix[15];
        } else {
            m00 = matrix[0];
            m01 = matrix[4];
            m02 = matrix[8];
            m03 = matrix[12];
            m10 = matrix[1];
            m11 = matrix[5];
            m12 = matrix[9];
            m13 = matrix[13];
            m20 = matrix[2];
            m21 = matrix[6];
            m22 = matrix[10];
            m23 = matrix[14];
            m30 = matrix[3];
            m31 = matrix[7];
            m32 = matrix[11];
            m33 = matrix[15];
        }
    }

    /**
     * Generate the transpose.
     *
     * @return a new Matrix4f with the rows and columns transposed
     */
    public Matrix4f transpose() {
        float[] tmp = new float[16];
        get(tmp, true);
        Matrix4f mat = new Matrix4f(tmp);
        return mat;
    }

    /**
     * Transpose in place.
     *
     * @return this (transposed)
     */
    public Matrix4f transposeLocal() {
        float tmp = m01;
        m01 = m10;
        m10 = tmp;

        tmp = m02;
        m02 = m20;
        m20 = tmp;

        tmp = m03;
        m03 = m30;
        m30 = tmp;

        tmp = m12;
        m12 = m21;
        m21 = tmp;

        tmp = m13;
        m13 = m31;
        m31 = tmp;

        tmp = m23;
        m23 = m32;
        m32 = tmp;

        return this;
    }

    /**
     * Copy all elements to a new, direct FloatBuffer.
     *
     * @return a rewound buffer containing all 16 element values in row-major
     * order
     */
    public FloatBuffer toFloatBuffer() {
        return toFloatBuffer(false);
    }

    /**
     * Copy all elements to a new, direct FloatBuffer.
     *
     * @param columnMajor true to store in column-major order, false to store in
     * row-major order
     * @return a rewound buffer containing all 16 element values
     */
    public FloatBuffer toFloatBuffer(boolean columnMajor) {
        FloatBuffer fb = BufferUtils.createFloatBuffer(16);
        fillFloatBuffer(fb, columnMajor);
        fb.rewind();
        return fb;
    }

    /**
     * Copy all elements to an existing FloatBuffer, starting at its current
     * position, in row-major order.
     *
     * @param fb the destination buffer (not null, must have space remaining for
     * 16 floats)
     * @return the destination buffer, its position advanced by 16
     */
    public FloatBuffer fillFloatBuffer(FloatBuffer fb) {
        return fillFloatBuffer(fb, false);
    }

    /**
     * Copy all elements to an existing FloatBuffer, starting at its current
     * position.
     *
     * @param fb the destination buffer (not null, must have space remaining for
     * 16 floats)
     * @param columnMajor true to store in column-major order, false to store in
     * row-major order
     * @return the destination buffer, its position advanced by 16
     */
    public FloatBuffer fillFloatBuffer(FloatBuffer fb, boolean columnMajor) {
//        if (columnMajor) {
//            fb.put(m00).put(m10).put(m20).put(m30);
//            fb.put(m01).put(m11).put(m21).put(m31);
//            fb.put(m02).put(m12).put(m22).put(m32);
//            fb.put(m03).put(m13).put(m23).put(m33);
//        } else {
//            fb.put(m00).put(m01).put(m02).put(m03);
//            fb.put(m10).put(m11).put(m12).put(m13);
//            fb.put(m20).put(m21).put(m22).put(m23);
//            fb.put(m30).put(m31).put(m32).put(m33);
//        }

        TempVars vars = TempVars.get();

        fillFloatArray(vars.matrixWrite, columnMajor);
        fb.put(vars.matrixWrite, 0, 16);

        vars.release();

        return fb;
    }

    /**
     * Copy all elements to a float array.
     *
     * @param f the destination array (not null, length&ge;16, modified)
     * @param columnMajor true &rarr; column-major order, false &rarr; row-major
     * order
     */
    public void fillFloatArray(float[] f, boolean columnMajor) {
        if (columnMajor) {
            f[0] = m00;
            f[1] = m10;
            f[2] = m20;
            f[3] = m30;
            f[4] = m01;
            f[5] = m11;
            f[6] = m21;
            f[7] = m31;
            f[8] = m02;
            f[9] = m12;
            f[10] = m22;
            f[11] = m32;
            f[12] = m03;
            f[13] = m13;
            f[14] = m23;
            f[15] = m33;
        } else {
            f[0] = m00;
            f[1] = m01;
            f[2] = m02;
            f[3] = m03;
            f[4] = m10;
            f[5] = m11;
            f[6] = m12;
            f[7] = m13;
            f[8] = m20;
            f[9] = m21;
            f[10] = m22;
            f[11] = m23;
            f[12] = m30;
            f[13] = m31;
            f[14] = m32;
            f[15] = m33;
        }
    }

    /**
     * Load from the specified FloatBuffer, in row-major order.
     *
     * @param fb the source buffer, must have 16 floats remaining to get
     * @return this (modified)
     */
    public Matrix4f readFloatBuffer(FloatBuffer fb) {
        return readFloatBuffer(fb, false);
    }

    /**
     * Load from the specified FloatBuffer.
     *
     * @param fb the source buffer, must have 16 floats remaining to get
     * @param columnMajor if true, the buffer contains column-major data,
     * otherwise it contains row-major data.
     * @return this (modified)
     */
    public Matrix4f readFloatBuffer(FloatBuffer fb, boolean columnMajor) {

        if (columnMajor) {
            m00 = fb.get();
            m10 = fb.get();
            m20 = fb.get();
            m30 = fb.get();
            m01 = fb.get();
            m11 = fb.get();
            m21 = fb.get();
            m31 = fb.get();
            m02 = fb.get();
            m12 = fb.get();
            m22 = fb.get();
            m32 = fb.get();
            m03 = fb.get();
            m13 = fb.get();
            m23 = fb.get();
            m33 = fb.get();
        } else {
            m00 = fb.get();
            m01 = fb.get();
            m02 = fb.get();
            m03 = fb.get();
            m10 = fb.get();
            m11 = fb.get();
            m12 = fb.get();
            m13 = fb.get();
            m20 = fb.get();
            m21 = fb.get();
            m22 = fb.get();
            m23 = fb.get();
            m30 = fb.get();
            m31 = fb.get();
            m32 = fb.get();
            m33 = fb.get();
        }
        return this;
    }

    /**
     * Configures as an identity matrix (diagonals = 1, other elements = 0).
     */
    public void loadIdentity() {
        m01 = m02 = m03 = 0.0f;
        m10 = m12 = m13 = 0.0f;
        m20 = m21 = m23 = 0.0f;
        m30 = m31 = m32 = 0.0f;
        m00 = m11 = m22 = m33 = 1.0f;
    }

    /**
     * Load a perspective-view transform with the specified clipping planes.
     *
     * @param near the coordinate of the near plane
     * @param far the coordinate of the far plane
     * @param left the coordinate of the left plane
     * @param right the coordinate of the right plane
     * @param top the coordinate of the top plane
     * @param bottom the coordinate of the bottom plane
     * @param parallel true &rarr; parallel sides, false &rarr; perspective
     */
    public void fromFrustum(float near, float far, float left, float right,
            float top, float bottom, boolean parallel) {
        loadIdentity();
        if (parallel) {
            // scale
            m00 = 2.0f / (right - left);
            //m11 = 2.0f / (bottom - top);
            m11 = 2.0f / (top - bottom);
            m22 = -2.0f / (far - near);
            m33 = 1f;

            // translation
            m03 = -(right + left) / (right - left);
            //m31 = -(bottom + top) / (bottom - top);
            m13 = -(top + bottom) / (top - bottom);
            m23 = -(far + near) / (far - near);
        } else {
            m00 = (2.0f * near) / (right - left);
            m11 = (2.0f * near) / (top - bottom);
            m32 = -1.0f;
            m33 = -0.0f;

            // A
            m02 = (right + left) / (right - left);

            // B
            m12 = (top + bottom) / (top - bottom);

            // C
            m22 = -(far + near) / (far - near);

            // D
            m23 = -(2.0f * far * near) / (far - near);
        }
    }

    /**
     * Load a 3-D rotation specified by an angle and axis. If the axis is
     * already normalized, use
     * {@link #fromAngleNormalAxis(float, com.jme3.math.Vector3f)} instead
     * because it's more efficient.
     *
     * @param angle the angle to rotate (in radians)
     * @param axis the axis of rotation (not null)
     */
    public void fromAngleAxis(float angle, Vector3f axis) {
        Vector3f normAxis = axis.normalize();
        fromAngleNormalAxis(angle, normAxis);
    }

    /**
     * Load a 3-D rotation specified by an angle and axis. Assumes the axis is
     * already normalized.
     *
     * @param angle the angle to rotate (in radians)
     * @param axis the axis of rotation (not null, already normalized)
     */
    public void fromAngleNormalAxis(float angle, Vector3f axis) {
        zero();
        m33 = 1;

        float fCos = FastMath.cos(angle);
        float fSin = FastMath.sin(angle);
        float fOneMinusCos = ((float) 1.0) - fCos;
        float fX2 = axis.x * axis.x;
        float fY2 = axis.y * axis.y;
        float fZ2 = axis.z * axis.z;
        float fXYM = axis.x * axis.y * fOneMinusCos;
        float fXZM = axis.x * axis.z * fOneMinusCos;
        float fYZM = axis.y * axis.z * fOneMinusCos;
        float fXSin = axis.x * fSin;
        float fYSin = axis.y * fSin;
        float fZSin = axis.z * fSin;

        m00 = fX2 * fOneMinusCos + fCos;
        m01 = fXYM - fZSin;
        m02 = fXZM + fYSin;
        m10 = fXYM + fZSin;
        m11 = fY2 * fOneMinusCos + fCos;
        m12 = fYZM - fXSin;
        m20 = fXZM - fYSin;
        m21 = fYZM + fXSin;
        m22 = fZ2 * fOneMinusCos + fCos;
    }

    /**
     * Multiplies in place by the scalar argument.
     *
     * @param scalar the scaling factor to apply to all elements
     */
    public void multLocal(float scalar) {
        m00 *= scalar;
        m01 *= scalar;
        m02 *= scalar;
        m03 *= scalar;
        m10 *= scalar;
        m11 *= scalar;
        m12 *= scalar;
        m13 *= scalar;
        m20 *= scalar;
        m21 *= scalar;
        m22 *= scalar;
        m23 *= scalar;
        m30 *= scalar;
        m31 *= scalar;
        m32 *= scalar;
        m33 *= scalar;
    }

    /**
     * Multiply by the specified scalar.
     *
     * @param scalar the scale factor to apply to all elements
     * @return a new Matrix4f with every element scaled
     */
    public Matrix4f mult(float scalar) {
        Matrix4f out = new Matrix4f();
        out.set(this);
        out.multLocal(scalar);
        return out;
    }

    /**
     * Multiply by the specified scalar.
     *
     * @param scalar the scale factor to apply to all elements
     * @param store storage for the result (modified) or null to create a new
     * matrix
     * @return a scaled matrix (either store or a new instance)
     */
    public Matrix4f mult(float scalar, Matrix4f store) {
        store.set(this);
        store.multLocal(scalar);
        return store;
    }

    /**
     * Right-multiply by the specified matrix. (This matrix is the left factor.)
     *
     * @param in2 the right factor (not null, unaffected)
     * @return the product, this times in2 (a new instance)
     */
    public Matrix4f mult(Matrix4f in2) {
        return mult(in2, null);
    }

    /**
     * Right-multiply by the specified matrix. (This matrix is the left factor.)
     *
     * @param in2 the right factor (not null)
     * @param store storage for the result (modified) or null to create a new
     * matrix. It is safe for in2 and store to be the same object.
     * @return the product, this times in2 (either store or a new instance)
     */
    public Matrix4f mult(Matrix4f in2, Matrix4f store) {
        if (store == null) {
            store = new Matrix4f();
        }

        TempVars v = TempVars.get();
        float[] m = v.matrixWrite;

        m[0] = m00 * in2.m00
                + m01 * in2.m10
                + m02 * in2.m20
                + m03 * in2.m30;
        m[1] = m00 * in2.m01
                + m01 * in2.m11
                + m02 * in2.m21
                + m03 * in2.m31;
        m[2] = m00 * in2.m02
                + m01 * in2.m12
                + m02 * in2.m22
                + m03 * in2.m32;
        m[3] = m00 * in2.m03
                + m01 * in2.m13
                + m02 * in2.m23
                + m03 * in2.m33;

        m[4] = m10 * in2.m00
                + m11 * in2.m10
                + m12 * in2.m20
                + m13 * in2.m30;
        m[5] = m10 * in2.m01
                + m11 * in2.m11
                + m12 * in2.m21
                + m13 * in2.m31;
        m[6] = m10 * in2.m02
                + m11 * in2.m12
                + m12 * in2.m22
                + m13 * in2.m32;
        m[7] = m10 * in2.m03
                + m11 * in2.m13
                + m12 * in2.m23
                + m13 * in2.m33;

        m[8] = m20 * in2.m00
                + m21 * in2.m10
                + m22 * in2.m20
                + m23 * in2.m30;
        m[9] = m20 * in2.m01
                + m21 * in2.m11
                + m22 * in2.m21
                + m23 * in2.m31;
        m[10] = m20 * in2.m02
                + m21 * in2.m12
                + m22 * in2.m22
                + m23 * in2.m32;
        m[11] = m20 * in2.m03
                + m21 * in2.m13
                + m22 * in2.m23
                + m23 * in2.m33;

        m[12] = m30 * in2.m00
                + m31 * in2.m10
                + m32 * in2.m20
                + m33 * in2.m30;
        m[13] = m30 * in2.m01
                + m31 * in2.m11
                + m32 * in2.m21
                + m33 * in2.m31;
        m[14] = m30 * in2.m02
                + m31 * in2.m12
                + m32 * in2.m22
                + m33 * in2.m32;
        m[15] = m30 * in2.m03
                + m31 * in2.m13
                + m32 * in2.m23
                + m33 * in2.m33;

        store.m00 = m[0];
        store.m01 = m[1];
        store.m02 = m[2];
        store.m03 = m[3];
        store.m10 = m[4];
        store.m11 = m[5];
        store.m12 = m[6];
        store.m13 = m[7];
        store.m20 = m[8];
        store.m21 = m[9];
        store.m22 = m[10];
        store.m23 = m[11];
        store.m30 = m[12];
        store.m31 = m[13];
        store.m32 = m[14];
        store.m33 = m[15];
        v.release();
        return store;
    }

    /**
     * Right-multiply in place, by the specified matrix. (This matrix is the
     * left factor.)
     *
     * @param in2 the right factor (not null)
     * @return this (modified)
     */
    public Matrix4f multLocal(Matrix4f in2) {
        return mult(in2, this);
    }

    /**
     * Apply this 3-D coordinate transform to the specified Vector3f.
     *
     * @param vec the vector to transform (not null)
     * @return a new vector
     */
    public Vector3f mult(Vector3f vec) {
        return mult(vec, null);
    }

    /**
     * Apply this 3-D coordinate transform to the specified Vector3f.
     *
     * @param vec the vector to transform (not null)
     * @param store storage for the result (modified) or null to create a new
     * vector
     * @return the transformed vector (either store or a new vector)
     */
    public Vector3f mult(Vector3f vec, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }

        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m01 * vy + m02 * vz + m03;
        store.y = m10 * vx + m11 * vy + m12 * vz + m13;
        store.z = m20 * vx + m21 * vy + m22 * vz + m23;

        return store;
    }

    /**
     * Multiply the specified Vector4f by this matrix.
     *
     * @param vec the vector to multiply (unaffected) or null
     * @return a new vector or null
     */
    public Vector4f mult(Vector4f vec) {
        return mult(vec, null);
    }

    /**
     * Multiply the specified Vector4f by this matrix.
     *
     * @param vec the vector to multiply (unaffected) or null
     * @param store storage for the result (modified) or null to create a new
     * vector
     * @return the product (either store or a new vector) or null
     */
    public Vector4f mult(Vector4f vec, Vector4f store) {
        if (null == vec) {
            logger.warning("Source vector is null, null result returned.");
            return null;
        }
        if (store == null) {
            store = new Vector4f();
        }

        float vx = vec.x, vy = vec.y, vz = vec.z, vw = vec.w;
        store.x = m00 * vx + m01 * vy + m02 * vz + m03 * vw;
        store.y = m10 * vx + m11 * vy + m12 * vz + m13 * vw;
        store.z = m20 * vx + m21 * vy + m22 * vz + m23 * vw;
        store.w = m30 * vx + m31 * vy + m32 * vz + m33 * vw;

        return store;
    }

    /**
     * Multiply the specified Vector4f by the transform of this matrix.
     *
     * @param vec the vector to multiply (unaffected) or null
     * @return a new vector or null
     */
    public Vector4f multAcross(Vector4f vec) {
        return multAcross(vec, null);
    }

    /**
     * Multiply the specified Vector4f by the transform of this matrix.
     *
     * @param vec the vector to multiply (unaffected) or null
     * @param store storage for the result (modified) or null to create a new
     * vector
     * @return the product (either store or a new vector) or null
     */
    public Vector4f multAcross(Vector4f vec, Vector4f store) {
        if (null == vec) {
            logger.warning("Source vector is null, null result returned.");
            return null;
        }
        if (store == null) {
            store = new Vector4f();
        }

        float vx = vec.x, vy = vec.y, vz = vec.z, vw = vec.w;
        store.x = m00 * vx + m10 * vy + m20 * vz + m30 * vw;
        store.y = m01 * vx + m11 * vy + m21 * vz + m31 * vw;
        store.z = m02 * vx + m12 * vy + m22 * vz + m32 * vw;
        store.w = m03 * vx + m13 * vy + m23 * vz + m33 * vw;

        return store;
    }

    /**
     * Rotate and scale the specified vector, but don't translate it.
     *
     * @param vec the vector to transform (not null, unaffected)
     * @param store storage for the result (modified) or null to create a new
     * vector
     * @return the transformed vector (either store or a new vector)
     */
    public Vector3f multNormal(Vector3f vec, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }

        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m01 * vy + m02 * vz;
        store.y = m10 * vx + m11 * vy + m12 * vz;
        store.z = m20 * vx + m21 * vy + m22 * vz;

        return store;
    }

    /**
     * Rotate and scale the specified vector by the transpose, but don't
     * translate it.
     *
     * @param vec the vector to transform (not null, unaffected)
     * @param store storage for the result (modified) or null to create a new
     * vector
     * @return the transformed vector (either store or a new vector)
     */
    public Vector3f multNormalAcross(Vector3f vec, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }

        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m10 * vy + m20 * vz;
        store.y = m01 * vx + m11 * vy + m21 * vz;
        store.z = m02 * vx + m12 * vy + m22 * vz;

        return store;
    }

    /**
     * Apply this perspective transform to the specified Vector3f. Return the W
     * value, calculated by dotting the vector with the last row.
     *
     * @param vec the vector to transform (not null, unaffected)
     * @param store storage for the result (not null, modified)
     * @return the W value
     */
    public float multProj(Vector3f vec, Vector3f store) {
        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m01 * vy + m02 * vz + m03;
        store.y = m10 * vx + m11 * vy + m12 * vz + m13;
        store.z = m20 * vx + m21 * vy + m22 * vz + m23;
        return m30 * vx + m31 * vy + m32 * vz + m33;
    }

    /**
     * Apply the transform of this 3-D coordinate transform to the specified
     * Vector3f.
     *
     * @param vec the vector to transform (unaffected) or null
     * @param store storage for the result (modified) or null to create a new
     * vector
     * @return the transformed vector (either store or a new vector) or null
     */
    public Vector3f multAcross(Vector3f vec, Vector3f store) {
        if (null == vec) {
            logger.warning("Source vector is null, null result returned.");
            return null;
        }
        if (store == null) {
            store = new Vector3f();
        }

        float vx = vec.x, vy = vec.y, vz = vec.z;
        store.x = m00 * vx + m10 * vy + m20 * vz + m30 * 1;
        store.y = m01 * vx + m11 * vy + m21 * vz + m31 * 1;
        store.z = m02 * vx + m12 * vy + m22 * vz + m32 * 1;

        return store;
    }

    /**
     * Multiply the specified Quaternion by this matrix.
     *
     * @param vec the Quaternion to multiply (unaffected) or null
     * @param store storage for the result (modified) or null to create a new
     * Quaternion
     * @return the product (either store or a new Quaternion) or null
     */
    public Quaternion mult(Quaternion vec, Quaternion store) {

        if (null == vec) {
            logger.warning("Source vector is null, null result returned.");
            return null;
        }
        if (store == null) {
            store = new Quaternion();
        }

        float x = m00 * vec.x + m10 * vec.y + m20 * vec.z + m30 * vec.w;
        float y = m01 * vec.x + m11 * vec.y + m21 * vec.z + m31 * vec.w;
        float z = m02 * vec.x + m12 * vec.y + m22 * vec.z + m32 * vec.w;
        float w = m03 * vec.x + m13 * vec.y + m23 * vec.z + m33 * vec.w;
        store.x = x;
        store.y = y;
        store.z = z;
        store.w = w;

        return store;
    }

    /**
     * Multiply the specified float array by this matrix.
     *
     * @param vec4f the array to multiply or null
     * @return vec4f (modified) or null
     */
    public float[] mult(float[] vec4f) {
        if (null == vec4f || vec4f.length != 4) {
            logger.warning("invalid array given, must be nonnull and length 4");
            return null;
        }

        float x = vec4f[0], y = vec4f[1], z = vec4f[2], w = vec4f[3];

        vec4f[0] = m00 * x + m01 * y + m02 * z + m03 * w;
        vec4f[1] = m10 * x + m11 * y + m12 * z + m13 * w;
        vec4f[2] = m20 * x + m21 * y + m22 * z + m23 * w;
        vec4f[3] = m30 * x + m31 * y + m32 * z + m33 * w;

        return vec4f;
    }

    /**
     * Multiply the specified float array by the transform of this matrix.
     *
     * @param vec4f the array to multiply or null
     * @return vec4f (modified) or null
     */
    public float[] multAcross(float[] vec4f) {
        if (null == vec4f || vec4f.length != 4) {
            logger.warning("invalid array given, must be nonnull and length 4");
            return null;
        }

        float x = vec4f[0], y = vec4f[1], z = vec4f[2], w = vec4f[3];

        vec4f[0] = m00 * x + m10 * y + m20 * z + m30 * w;
        vec4f[1] = m01 * x + m11 * y + m21 * z + m31 * w;
        vec4f[2] = m02 * x + m12 * y + m22 * z + m32 * w;
        vec4f[3] = m03 * x + m13 * y + m23 * z + m33 * w;

        return vec4f;
    }

    /**
     * Generate the inverse.
     *
     * @return a new instance
     */
    public Matrix4f invert() {
        return invert(null);
    }

    /**
     * Generate the inverse.
     *
     * @param store storage for the result (modified) or null to create a new
     * matrix
     * @return either store or a new instance
     * @throws ArithmeticException if cannot be inverted
     */
    public Matrix4f invert(Matrix4f store) {
        if (store == null) {
            store = new Matrix4f();
        }

        float fA0 = m00 * m11 - m01 * m10;
        float fA1 = m00 * m12 - m02 * m10;
        float fA2 = m00 * m13 - m03 * m10;
        float fA3 = m01 * m12 - m02 * m11;
        float fA4 = m01 * m13 - m03 * m11;
        float fA5 = m02 * m13 - m03 * m12;
        float fB0 = m20 * m31 - m21 * m30;
        float fB1 = m20 * m32 - m22 * m30;
        float fB2 = m20 * m33 - m23 * m30;
        float fB3 = m21 * m32 - m22 * m31;
        float fB4 = m21 * m33 - m23 * m31;
        float fB5 = m22 * m33 - m23 * m32;
        float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;

        if (FastMath.abs(fDet) <= 0f) {
            throw new ArithmeticException("This matrix cannot be inverted");
        }

        store.m00 = +m11 * fB5 - m12 * fB4 + m13 * fB3;
        store.m10 = -m10 * fB5 + m12 * fB2 - m13 * fB1;
        store.m20 = +m10 * fB4 - m11 * fB2 + m13 * fB0;
        store.m30 = -m10 * fB3 + m11 * fB1 - m12 * fB0;
        store.m01 = -m01 * fB5 + m02 * fB4 - m03 * fB3;
        store.m11 = +m00 * fB5 - m02 * fB2 + m03 * fB1;
        store.m21 = -m00 * fB4 + m01 * fB2 - m03 * fB0;
        store.m31 = +m00 * fB3 - m01 * fB1 + m02 * fB0;
        store.m02 = +m31 * fA5 - m32 * fA4 + m33 * fA3;
        store.m12 = -m30 * fA5 + m32 * fA2 - m33 * fA1;
        store.m22 = +m30 * fA4 - m31 * fA2 + m33 * fA0;
        store.m32 = -m30 * fA3 + m31 * fA1 - m32 * fA0;
        store.m03 = -m21 * fA5 + m22 * fA4 - m23 * fA3;
        store.m13 = +m20 * fA5 - m22 * fA2 + m23 * fA1;
        store.m23 = -m20 * fA4 + m21 * fA2 - m23 * fA0;
        store.m33 = +m20 * fA3 - m21 * fA1 + m22 * fA0;

        float fInvDet = 1.0f / fDet;
        store.multLocal(fInvDet);

        return store;
    }

    /**
     * Inverts in place. If the current instance is singular, the matrix is
     * zeroed.
     *
     * @return the (inverted) current instance (for chaining)
     */
    public Matrix4f invertLocal() {

        float fA0 = m00 * m11 - m01 * m10;
        float fA1 = m00 * m12 - m02 * m10;
        float fA2 = m00 * m13 - m03 * m10;
        float fA3 = m01 * m12 - m02 * m11;
        float fA4 = m01 * m13 - m03 * m11;
        float fA5 = m02 * m13 - m03 * m12;
        float fB0 = m20 * m31 - m21 * m30;
        float fB1 = m20 * m32 - m22 * m30;
        float fB2 = m20 * m33 - m23 * m30;
        float fB3 = m21 * m32 - m22 * m31;
        float fB4 = m21 * m33 - m23 * m31;
        float fB5 = m22 * m33 - m23 * m32;
        float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;

        if (FastMath.abs(fDet) <= 0f) {
            return zero();
        }

        float f00 = +m11 * fB5 - m12 * fB4 + m13 * fB3;
        float f10 = -m10 * fB5 + m12 * fB2 - m13 * fB1;
        float f20 = +m10 * fB4 - m11 * fB2 + m13 * fB0;
        float f30 = -m10 * fB3 + m11 * fB1 - m12 * fB0;
        float f01 = -m01 * fB5 + m02 * fB4 - m03 * fB3;
        float f11 = +m00 * fB5 - m02 * fB2 + m03 * fB1;
        float f21 = -m00 * fB4 + m01 * fB2 - m03 * fB0;
        float f31 = +m00 * fB3 - m01 * fB1 + m02 * fB0;
        float f02 = +m31 * fA5 - m32 * fA4 + m33 * fA3;
        float f12 = -m30 * fA5 + m32 * fA2 - m33 * fA1;
        float f22 = +m30 * fA4 - m31 * fA2 + m33 * fA0;
        float f32 = -m30 * fA3 + m31 * fA1 - m32 * fA0;
        float f03 = -m21 * fA5 + m22 * fA4 - m23 * fA3;
        float f13 = +m20 * fA5 - m22 * fA2 + m23 * fA1;
        float f23 = -m20 * fA4 + m21 * fA2 - m23 * fA0;
        float f33 = +m20 * fA3 - m21 * fA1 + m22 * fA0;

        m00 = f00;
        m01 = f01;
        m02 = f02;
        m03 = f03;
        m10 = f10;
        m11 = f11;
        m12 = f12;
        m13 = f13;
        m20 = f20;
        m21 = f21;
        m22 = f22;
        m23 = f23;
        m30 = f30;
        m31 = f31;
        m32 = f32;
        m33 = f33;

        float fInvDet = 1.0f / fDet;
        multLocal(fInvDet);

        return this;
    }

    /**
     * Generate the adjoint.
     *
     * @return a new instance
     */
    public Matrix4f adjoint() {
        return adjoint(null);
    }

    /**
     * Load with the specified coordinate transform. The effective sequence of
     * operations is: scale, then rotate, then translate.
     *
     * @param position the desired translation (not null, unaffected)
     * @param scale the desired scale factors (not null, unaffected)
     * @param rotMat the desired rotation (not null, unaffected)
     */
    public void setTransform(Vector3f position, Vector3f scale, Matrix3f rotMat) {
        // Ordering:
        //    1. Scale
        //    2. Rotate
        //    3. Translate

        // Set up final matrix with scale, rotation and translation
        m00 = scale.x * rotMat.m00;
        m01 = scale.y * rotMat.m01;
        m02 = scale.z * rotMat.m02;
        m03 = position.x;
        m10 = scale.x * rotMat.m10;
        m11 = scale.y * rotMat.m11;
        m12 = scale.z * rotMat.m12;
        m13 = position.y;
        m20 = scale.x * rotMat.m20;
        m21 = scale.y * rotMat.m21;
        m22 = scale.z * rotMat.m22;
        m23 = position.z;

        // No projection term
        m30 = 0;
        m31 = 0;
        m32 = 0;
        m33 = 1;
    }

    /**
     * Generate the adjoint.
     *
     * @param store storage for the result (modified) or null to create a new
     * matrix
     * @return either store or a new instance
     */
    public Matrix4f adjoint(Matrix4f store) {
        if (store == null) {
            store = new Matrix4f();
        }

        float fA0 = m00 * m11 - m01 * m10;
        float fA1 = m00 * m12 - m02 * m10;
        float fA2 = m00 * m13 - m03 * m10;
        float fA3 = m01 * m12 - m02 * m11;
        float fA4 = m01 * m13 - m03 * m11;
        float fA5 = m02 * m13 - m03 * m12;
        float fB0 = m20 * m31 - m21 * m30;
        float fB1 = m20 * m32 - m22 * m30;
        float fB2 = m20 * m33 - m23 * m30;
        float fB3 = m21 * m32 - m22 * m31;
        float fB4 = m21 * m33 - m23 * m31;
        float fB5 = m22 * m33 - m23 * m32;

        store.m00 = +m11 * fB5 - m12 * fB4 + m13 * fB3;
        store.m10 = -m10 * fB5 + m12 * fB2 - m13 * fB1;
        store.m20 = +m10 * fB4 - m11 * fB2 + m13 * fB0;
        store.m30 = -m10 * fB3 + m11 * fB1 - m12 * fB0;
        store.m01 = -m01 * fB5 + m02 * fB4 - m03 * fB3;
        store.m11 = +m00 * fB5 - m02 * fB2 + m03 * fB1;
        store.m21 = -m00 * fB4 + m01 * fB2 - m03 * fB0;
        store.m31 = +m00 * fB3 - m01 * fB1 + m02 * fB0;
        store.m02 = +m31 * fA5 - m32 * fA4 + m33 * fA3;
        store.m12 = -m30 * fA5 + m32 * fA2 - m33 * fA1;
        store.m22 = +m30 * fA4 - m31 * fA2 + m33 * fA0;
        store.m32 = -m30 * fA3 + m31 * fA1 - m32 * fA0;
        store.m03 = -m21 * fA5 + m22 * fA4 - m23 * fA3;
        store.m13 = +m20 * fA5 - m22 * fA2 + m23 * fA1;
        store.m23 = -m20 * fA4 + m21 * fA2 - m23 * fA0;
        store.m33 = +m20 * fA3 - m21 * fA1 + m22 * fA0;

        return store;
    }

    /**
     * Calculate the determinant.
     *
     * @return the determinant
     */
    public float determinant() {
        float fA0 = m00 * m11 - m01 * m10;
        float fA1 = m00 * m12 - m02 * m10;
        float fA2 = m00 * m13 - m03 * m10;
        float fA3 = m01 * m12 - m02 * m11;
        float fA4 = m01 * m13 - m03 * m11;
        float fA5 = m02 * m13 - m03 * m12;
        float fB0 = m20 * m31 - m21 * m30;
        float fB1 = m20 * m32 - m22 * m30;
        float fB2 = m20 * m33 - m23 * m30;
        float fB3 = m21 * m32 - m22 * m31;
        float fB4 = m21 * m33 - m23 * m31;
        float fB5 = m22 * m33 - m23 * m32;
        float fDet = fA0 * fB5 - fA1 * fB4 + fA2 * fB3 + fA3 * fB2 - fA4 * fB1 + fA5 * fB0;
        return fDet;
    }

    /**
     * Sets all elements to zero.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Matrix4f zero() {
        m00 = m01 = m02 = m03 = 0.0f;
        m10 = m11 = m12 = m13 = 0.0f;
        m20 = m21 = m22 = m23 = 0.0f;
        m30 = m31 = m32 = m33 = 0.0f;
        return this;
    }

    /**
     * Add the specified matrix.
     *
     * @param mat the matrix to add (not null)
     * @return the sum (a new instance)
     */
    public Matrix4f add(Matrix4f mat) {
        Matrix4f result = new Matrix4f();
        result.m00 = this.m00 + mat.m00;
        result.m01 = this.m01 + mat.m01;
        result.m02 = this.m02 + mat.m02;
        result.m03 = this.m03 + mat.m03;
        result.m10 = this.m10 + mat.m10;
        result.m11 = this.m11 + mat.m11;
        result.m12 = this.m12 + mat.m12;
        result.m13 = this.m13 + mat.m13;
        result.m20 = this.m20 + mat.m20;
        result.m21 = this.m21 + mat.m21;
        result.m22 = this.m22 + mat.m22;
        result.m23 = this.m23 + mat.m23;
        result.m30 = this.m30 + mat.m30;
        result.m31 = this.m31 + mat.m31;
        result.m32 = this.m32 + mat.m32;
        result.m33 = this.m33 + mat.m33;
        return result;
    }

    /**
     * Sum in place, with the specified matrix.
     *
     * @param mat the matrix to add (not null)
     */
    public void addLocal(Matrix4f mat) {
        m00 += mat.m00;
        m01 += mat.m01;
        m02 += mat.m02;
        m03 += mat.m03;
        m10 += mat.m10;
        m11 += mat.m11;
        m12 += mat.m12;
        m13 += mat.m13;
        m20 += mat.m20;
        m21 += mat.m21;
        m22 += mat.m22;
        m23 += mat.m23;
        m30 += mat.m30;
        m31 += mat.m31;
        m32 += mat.m32;
        m33 += mat.m33;
    }

    /**
     * Determine the translation component of this 3-D coordinate transform.
     *
     * @return a new translation vector
     */
    public Vector3f toTranslationVector() {
        return new Vector3f(m03, m13, m23);
    }

    /**
     * Returns the translation component of the coordinate transform.
     *
     * @param vector storage for the result (not null, modified)
     * @return the translation component (in {@code vector}) for chaining
     */
    public Vector3f toTranslationVector(Vector3f vector) {
        return vector.set(m03, m13, m23);
    }

    /**
     * Determine the rotation component of this 3-D coordinate transform.
     *
     * @return a new rotation Quaternion
     */
    public Quaternion toRotationQuat() {
        Quaternion quat = new Quaternion();
        quat.fromRotationMatrix(toRotationMatrix());
        return quat;
    }

    /**
     * Returns the rotation component of the coordinate transform.
     *
     * @param q storage for the result (not null, modified)
     * @return the rotation component (in {@code q}) for chaining
     */
    public Quaternion toRotationQuat(Quaternion q) {
        return q.fromRotationMatrix(m00, m01, m02, m10,
                m11, m12, m20, m21, m22);
    }

    /**
     * Determine the rotation component of this 3-D coordinate transform.
     *
     * @return a new rotation Matrix3f
     */
    public Matrix3f toRotationMatrix() {
        return new Matrix3f(m00, m01, m02, m10, m11, m12, m20, m21, m22);
    }

    /**
     * Determines the rotation component of the coordinate transform.
     *
     * @param mat storage for the result (not null, modified)
     */
    public void toRotationMatrix(Matrix3f mat) {
        mat.m00 = m00;
        mat.m01 = m01;
        mat.m02 = m02;
        mat.m10 = m10;
        mat.m11 = m11;
        mat.m12 = m12;
        mat.m20 = m20;
        mat.m21 = m21;
        mat.m22 = m22;
    }

    /**
     * Determine the scale component of this 3-D coordinate transform.
     *
     * @return a new Vector3f
     */
    public Vector3f toScaleVector() {
        Vector3f result = new Vector3f();
        this.toScaleVector(result);
        return result;
    }

    /**
     * Determines the scale component of the coordinate transform.
     *
     * @param store storage for the result (not null, modified)
     * @return the scale factors (in {@code store}) for chaining
     */
    public Vector3f toScaleVector(Vector3f store) {
        float scaleX = (float) Math.sqrt(m00 * m00 + m10 * m10 + m20 * m20);
        float scaleY = (float) Math.sqrt(m01 * m01 + m11 * m11 + m21 * m21);
        float scaleZ = (float) Math.sqrt(m02 * m02 + m12 * m12 + m22 * m22);
        store.set(scaleX, scaleY, scaleZ);
        return store;
    }

    /**
     * Alters the scale component of the coordinate transform.
     *
     * @param x the desired scale factor for the X axis
     * @param y the desired scale factor for the Y axis
     * @param z the desired scale factor for the Z axis
     */
    public void setScale(float x, float y, float z) {

        float length = m00 * m00 + m10 * m10 + m20 * m20;
        if (length != 0f) {
            length = length == 1 ? x : (x / FastMath.sqrt(length));
            m00 *= length;
            m10 *= length;
            m20 *= length;
        }

        length = m01 * m01 + m11 * m11 + m21 * m21;
        if (length != 0f) {
            length = length == 1 ? y : (y / FastMath.sqrt(length));
            m01 *= length;
            m11 *= length;
            m21 *= length;
        }

        length = m02 * m02 + m12 * m12 + m22 * m22;
        if (length != 0f) {
            length = length == 1 ? z : (z / FastMath.sqrt(length));
            m02 *= length;
            m12 *= length;
            m22 *= length;
        }
    }

    /**
     * Alters the scale component of the coordinate transform.
     *
     * @param scale the desired scale factors (not null, unaffected)
     */
    public void setScale(Vector3f scale) {
        this.setScale(scale.x, scale.y, scale.z);
    }

    /**
     * Alter the translation component of this 3-D coordinate transform.
     *
     * @param translation the desired translation (not null, length=3,
     * unaffected)
     * @throws IllegalArgumentException if translation doesn't have length=3.
     */
    public void setTranslation(float[] translation) {
        if (translation.length != 3) {
            throw new IllegalArgumentException(
                    "Translation size must be 3.");
        }
        m03 = translation[0];
        m13 = translation[1];
        m23 = translation[2];
    }

    /**
     * Alter the translation component of this 3-D coordinate transform.
     *
     * @param x the desired X-axis offset
     * @param y the desired Y-axis offset
     * @param z the desired Z-axis offset
     */
    public void setTranslation(float x, float y, float z) {
        m03 = x;
        m13 = y;
        m23 = z;
    }

    /**
     * Alters the translation component of the coordinate transform.
     *
     * @param translation the desired translation (not null, unaffected)
     */
    public void setTranslation(Vector3f translation) {
        m03 = translation.x;
        m13 = translation.y;
        m23 = translation.z;
    }

    /**
     * Alter the inverse-translation component of this 3-D coordinate transform.
     *
     * @param translation the desired inverse translation (not null, length=3,
     * unaffected)
     * @throws IllegalArgumentException if translation doesn't have length=3.
     */
    public void setInverseTranslation(float[] translation) {
        if (translation.length != 3) {
            throw new IllegalArgumentException(
                    "Translation size must be 3.");
        }
        m03 = -translation[0];
        m13 = -translation[1];
        m23 = -translation[2];
    }

    /**
     * Load a rotation around three axes (x, y, z). Where each axis has a
     * specified rotation in degrees. These rotations are expressed in a single
     * <code>Vector3f</code> object.
     *
     * @param angles the desired rotation angles for each axis (in degrees)
     */
    public void angleRotation(Vector3f angles) {
        float angle;
        float sr, sp, sy, cr, cp, cy;

        angle = (angles.z * FastMath.DEG_TO_RAD);
        sy = FastMath.sin(angle);
        cy = FastMath.cos(angle);
        angle = (angles.y * FastMath.DEG_TO_RAD);
        sp = FastMath.sin(angle);
        cp = FastMath.cos(angle);
        angle = (angles.x * FastMath.DEG_TO_RAD);
        sr = FastMath.sin(angle);
        cr = FastMath.cos(angle);

        // matrix = (Z * Y) * X
        m00 = cp * cy;
        m10 = cp * sy;
        m20 = -sp;
        m01 = sr * sp * cy + cr * -sy;
        m11 = sr * sp * sy + cr * cy;
        m21 = sr * cp;
        m02 = (cr * sp * cy + -sr * -sy);
        m12 = (cr * sp * sy + -sr * cy);
        m22 = cr * cp;
        m03 = 0.0f;
        m13 = 0.0f;
        m23 = 0.0f;
    }

    /**
     * Load a rotation from a <code>Quaternion</code>.
     *
     * @param quat the desired rotation (not null, unaffected)
     * @throws NullPointerException if quat is null.
     */
    public void setRotationQuaternion(Quaternion quat) {
        quat.toRotationMatrix(this);
    }

    /**
     * Load an inverted rotation from Euler angles in radians.
     *
     * @param angles the desired Euler angles (in radians, not null, length=3)
     * @throws IllegalArgumentException if angles doesn't have length=3.
     */
    public void setInverseRotationRadians(float[] angles) {
        if (angles.length != 3) {
            throw new IllegalArgumentException(
                    "Angles must be of size 3.");
        }
        double cr = FastMath.cos(angles[0]);
        double sr = FastMath.sin(angles[0]);
        double cp = FastMath.cos(angles[1]);
        double sp = FastMath.sin(angles[1]);
        double cy = FastMath.cos(angles[2]);
        double sy = FastMath.sin(angles[2]);

        m00 = (float) (cp * cy);
        m10 = (float) (cp * sy);
        m20 = (float) (-sp);

        double srsp = sr * sp;
        double crsp = cr * sp;

        m01 = (float) (srsp * cy - cr * sy);
        m11 = (float) (srsp * sy + cr * cy);
        m21 = (float) (sr * cp);

        m02 = (float) (crsp * cy + sr * sy);
        m12 = (float) (crsp * sy - sr * cy);
        m22 = (float) (cr * cp);
    }

    /**
     * Load an inverted rotation from Euler angles in degrees.
     *
     * @param angles the desired Euler angles (in degrees, not null, length=3)
     * @throws IllegalArgumentException if angles doesn't have length=3.
     */
    public void setInverseRotationDegrees(float[] angles) {
        if (angles.length != 3) {
            throw new IllegalArgumentException(
                    "Angles must be of size 3.");
        }
        float vec[] = new float[3];
        vec[0] = (angles[0] * FastMath.RAD_TO_DEG);
        vec[1] = (angles[1] * FastMath.RAD_TO_DEG);
        vec[2] = (angles[2] * FastMath.RAD_TO_DEG);
        setInverseRotationRadians(vec);
    }

    /**
     * Inverse translate the specified vector using the translation component of
     * this 3-D coordinate transform.
     *
     * @param vec the vector to translate (not null, length=3, modified)
     * @throws IllegalArgumentException if vec doesn't have length=3.
     */
    public void inverseTranslateVect(float[] vec) {
        if (vec.length != 3) {
            throw new IllegalArgumentException(
                    "vec must be of size 3.");
        }

        vec[0] = vec[0] - m03;
        vec[1] = vec[1] - m13;
        vec[2] = vec[2] - m23;
    }

    /**
     * Inverse translate the specified Vector3f using the translation component
     * of this 3-D coordinate transform.
     *
     * @param data the Vector3f to translate (not null, modified)
     */
    public void inverseTranslateVect(Vector3f data) {
        data.x -= m03;
        data.y -= m13;
        data.z -= m23;
    }

    /**
     * Translate the specified Vector3f using the translation component of this
     * 3-D coordinate transform.
     *
     * @param data the Vector3f to translate (not null, modified)
     */
    public void translateVect(Vector3f data) {
        data.x += m03;
        data.y += m13;
        data.z += m23;
    }

    /**
     * Inverse rotate the specified Vector3f using the rotation component of
     * this 3-D coordinate transform.
     *
     * @param vec the Vector3f to inverse rotate (not null, modified)
     */
    public void inverseRotateVect(Vector3f vec) {
        float vx = vec.x, vy = vec.y, vz = vec.z;

        vec.x = vx * m00 + vy * m10 + vz * m20;
        vec.y = vx * m01 + vy * m11 + vz * m21;
        vec.z = vx * m02 + vy * m12 + vz * m22;
    }

    /**
     * Rotate the specified Vector3f using the rotation component of this 3-D
     * coordinate transform.
     *
     * @param vec the Vector3f to rotate (not null, modified)
     */
    public void rotateVect(Vector3f vec) {
        float vx = vec.x, vy = vec.y, vz = vec.z;

        vec.x = vx * m00 + vy * m01 + vz * m02;
        vec.y = vx * m10 + vy * m11 + vz * m12;
        vec.z = vx * m20 + vy * m21 + vz * m22;
    }

    /**
     * Returns a string representation of the matrix, which is unaffected. For
     * example, the identity matrix is represented by:
     * <pre>
     * Matrix4f
     * [
     *  1.0  0.0  0.0  0.0
     *  0.0  1.0  0.0  0.0
     *  0.0  0.0  1.0  0.0
     *  0.0  0.0  0.0  1.0
     * ]
     * </pre>
     *
     * @return the string representation (not null, not empty)
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Matrix4f\n[\n");
        result.append(" ");
        result.append(m00);
        result.append("  ");
        result.append(m01);
        result.append("  ");
        result.append(m02);
        result.append("  ");
        result.append(m03);
        result.append(" \n");
        result.append(" ");
        result.append(m10);
        result.append("  ");
        result.append(m11);
        result.append("  ");
        result.append(m12);
        result.append("  ");
        result.append(m13);
        result.append(" \n");
        result.append(" ");
        result.append(m20);
        result.append("  ");
        result.append(m21);
        result.append("  ");
        result.append(m22);
        result.append("  ");
        result.append(m23);
        result.append(" \n");
        result.append(" ");
        result.append(m30);
        result.append("  ");
        result.append(m31);
        result.append("  ");
        result.append(m32);
        result.append("  ");
        result.append(m33);
        result.append(" \n]");
        return result.toString();
    }

    /**
     * Returns a hash code. If two matrices have identical values, they will
     * have the same hash code. The matrix is unaffected.
     *
     * @return a 32-bit value for use in hashing
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        int hash = 37;
        hash = 37 * hash + Float.floatToIntBits(m00);
        hash = 37 * hash + Float.floatToIntBits(m01);
        hash = 37 * hash + Float.floatToIntBits(m02);
        hash = 37 * hash + Float.floatToIntBits(m03);

        hash = 37 * hash + Float.floatToIntBits(m10);
        hash = 37 * hash + Float.floatToIntBits(m11);
        hash = 37 * hash + Float.floatToIntBits(m12);
        hash = 37 * hash + Float.floatToIntBits(m13);

        hash = 37 * hash + Float.floatToIntBits(m20);
        hash = 37 * hash + Float.floatToIntBits(m21);
        hash = 37 * hash + Float.floatToIntBits(m22);
        hash = 37 * hash + Float.floatToIntBits(m23);

        hash = 37 * hash + Float.floatToIntBits(m30);
        hash = 37 * hash + Float.floatToIntBits(m31);
        hash = 37 * hash + Float.floatToIntBits(m32);
        hash = 37 * hash + Float.floatToIntBits(m33);

        return hash;
    }

    /**
     * Tests for exact equality with the argument, distinguishing -0 from 0. If
     * {@code o} is null, false is returned. Either way, the current instance is
     * unaffected.
     *
     * @param o the object to compare (may be null, unaffected)
     * @return true if {@code this} and {@code o} have identical values,
     *     otherwise false
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || o.getClass() != getClass()) {
            return false;
        }

        if (this == o) {
            return true;
        }

        Matrix4f comp = (Matrix4f) o;
        if (Float.compare(m00, comp.m00) != 0) {
            return false;
        }
        if (Float.compare(m01, comp.m01) != 0) {
            return false;
        }
        if (Float.compare(m02, comp.m02) != 0) {
            return false;
        }
        if (Float.compare(m03, comp.m03) != 0) {
            return false;
        }

        if (Float.compare(m10, comp.m10) != 0) {
            return false;
        }
        if (Float.compare(m11, comp.m11) != 0) {
            return false;
        }
        if (Float.compare(m12, comp.m12) != 0) {
            return false;
        }
        if (Float.compare(m13, comp.m13) != 0) {
            return false;
        }

        if (Float.compare(m20, comp.m20) != 0) {
            return false;
        }
        if (Float.compare(m21, comp.m21) != 0) {
            return false;
        }
        if (Float.compare(m22, comp.m22) != 0) {
            return false;
        }
        if (Float.compare(m23, comp.m23) != 0) {
            return false;
        }

        if (Float.compare(m30, comp.m30) != 0) {
            return false;
        }
        if (Float.compare(m31, comp.m31) != 0) {
            return false;
        }
        if (Float.compare(m32, comp.m32) != 0) {
            return false;
        }
        if (Float.compare(m33, comp.m33) != 0) {
            return false;
        }

        return true;
    }

    /**
     * Serialize to the specified exporter, for example when saving to a J3O
     * file.
     *
     * @param e (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule cap = e.getCapsule(this);
        cap.write(m00, "m00", 1);
        cap.write(m01, "m01", 0);
        cap.write(m02, "m02", 0);
        cap.write(m03, "m03", 0);
        cap.write(m10, "m10", 0);
        cap.write(m11, "m11", 1);
        cap.write(m12, "m12", 0);
        cap.write(m13, "m13", 0);
        cap.write(m20, "m20", 0);
        cap.write(m21, "m21", 0);
        cap.write(m22, "m22", 1);
        cap.write(m23, "m23", 0);
        cap.write(m30, "m30", 0);
        cap.write(m31, "m31", 0);
        cap.write(m32, "m32", 0);
        cap.write(m33, "m33", 1);
    }

    /**
     * De-serialize from the specified importer, for example when loading from a
     * J3O file.
     *
     * @param importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule cap = importer.getCapsule(this);
        m00 = cap.readFloat("m00", 1);
        m01 = cap.readFloat("m01", 0);
        m02 = cap.readFloat("m02", 0);
        m03 = cap.readFloat("m03", 0);
        m10 = cap.readFloat("m10", 0);
        m11 = cap.readFloat("m11", 1);
        m12 = cap.readFloat("m12", 0);
        m13 = cap.readFloat("m13", 0);
        m20 = cap.readFloat("m20", 0);
        m21 = cap.readFloat("m21", 0);
        m22 = cap.readFloat("m22", 1);
        m23 = cap.readFloat("m23", 0);
        m30 = cap.readFloat("m30", 0);
        m31 = cap.readFloat("m31", 0);
        m32 = cap.readFloat("m32", 0);
        m33 = cap.readFloat("m33", 1);
    }

    /**
     * Test for exact identity.
     *
     * @return true if this is an exact identity, otherwise false
     */
    public boolean isIdentity() {
        return (m00 == 1 && m01 == 0 && m02 == 0 && m03 == 0)
                && (m10 == 0 && m11 == 1 && m12 == 0 && m13 == 0)
                && (m20 == 0 && m21 == 0 && m22 == 1 && m23 == 0)
                && (m30 == 0 && m31 == 0 && m32 == 0 && m33 == 1);
    }

    /**
     * Scale by the specified Vector3f.
     *
     * @param scale the scale factors to apply
     */
    public void scale(Vector3f scale) {
        m00 *= scale.getX();
        m10 *= scale.getX();
        m20 *= scale.getX();
        m30 *= scale.getX();
        m01 *= scale.getY();
        m11 *= scale.getY();
        m21 *= scale.getY();
        m31 *= scale.getY();
        m02 *= scale.getZ();
        m12 *= scale.getZ();
        m22 *= scale.getZ();
        m32 *= scale.getZ();
    }

    static boolean equalIdentity(Matrix4f mat) {
        if (Math.abs(mat.m00 - 1) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m11 - 1) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m22 - 1) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m33 - 1) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m01) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m02) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m03) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m10) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m12) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m13) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m20) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m21) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m23) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m30) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m31) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m32) > 1e-4) {
            return false;
        }

        return true;
    }

    // XXX: This tests more solid than converting the q to a matrix and multiplying... why?
    public void multLocal(Quaternion rotation) {
        Vector3f axis = new Vector3f();
        float angle = rotation.toAngleAxis(axis);
        Matrix4f matrix4f = new Matrix4f();
        matrix4f.fromAngleAxis(angle, axis);
        multLocal(matrix4f);
    }

    /**
     * Tests for approximate equality with the specified matrix, using the
     * specified tolerance. If {@code other} is null, false is returned. Either
     * way, the current instance is unaffected.
     *
     * @param other the matrix to compare (unaffected) or null for none
     * @param epsilon the tolerance for each element
     * @return true if all 16 elements are within tolerance, otherwise false
     */
    public boolean isSimilar(Matrix4f other, float epsilon) {
        if (other == null) {
            return false;
        }

        if (Float.compare(Math.abs(other.m00 - m00), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m01 - m01), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m02 - m02), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m03 - m03), epsilon) > 0) {
            return false;
        }

        if (Float.compare(Math.abs(other.m10 - m10), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m11 - m11), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m12 - m12), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m13 - m13), epsilon) > 0) {
            return false;
        }

        if (Float.compare(Math.abs(other.m20 - m20), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m21 - m21), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m22 - m22), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m23 - m23), epsilon) > 0) {
            return false;
        }

        if (Float.compare(Math.abs(other.m30 - m30), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m31 - m31), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m32 - m32), epsilon) > 0) {
            return false;
        }
        if (Float.compare(Math.abs(other.m33 - m33), epsilon) > 0) {
            return false;
        }

        return true;
    }

    /**
     * Creates a copy. The current instance is unaffected.
     *
     * @return a new instance with the same element values
     */
    @Override
    public Matrix4f clone() {
        try {
            return (Matrix4f) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }
}
