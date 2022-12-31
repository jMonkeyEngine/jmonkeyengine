/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
 * A 3x3 matrix composed of 9 single-precision elements, used to represent
 * linear transformations of 3-D coordinates, such as rotations, reflections,
 * and scaling.
 *
 * <p>Element numbering is (row, column), so m01 is the element in row 0,
 * column 1.
 *
 * <p>For pure rotations, the {@link com.jme3.math.Quaternion} class provides a
 * more efficient representation.
 *
 * <p>With one exception, the methods with names ending in "Local" modify the
 * current instance. They are used to avoid creating garbage.
 *
 * @author Mark Powell
 * @author Joshua Slack
 */
public final class Matrix3f implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private static final Logger logger = Logger.getLogger(Matrix3f.class.getName());
    /**
     * The element in row 0, column 0.
     */
    protected float m00;
    /**
     * The element in row 0, column 1.
     */
    protected float m01;
    /**
     * The element in row 0, column 2.
     */
    protected float m02;
    /**
     * The element in row 1, column 0.
     */
    protected float m10;
    /**
     * The element in row 1, column 1.
     */
    protected float m11;
    /**
     * The element in row 1, column 2.
     */
    protected float m12;
    /**
     * The element in row 2, column 0.
     */
    protected float m20;
    /**
     * The element in row 2, column 1.
     */
    protected float m21;
    /**
     * The element in row 2, column 2.
     */
    protected float m22;
    /**
     * Shared instance of the all-zero matrix. Do not modify!
     */
    public static final Matrix3f ZERO = new Matrix3f(0, 0, 0, 0, 0, 0, 0, 0, 0);
    /**
     * Shared instance of the identity matrix (diagonals = 1, other elements =
     * 0). Do not modify!
     */
    public static final Matrix3f IDENTITY = new Matrix3f();

    /**
     * Instantiates an identity matrix (diagonals = 1, other elements = 0).
     */
    public Matrix3f() {
        loadIdentity();
    }

    /**
     * Instantiates a matrix with specified elements.
     *
     * @param m00 the desired value for row 0, column 0
     * @param m01 the desired value for row 0, column 1
     * @param m02 the desired value for row 0, column 2
     * @param m10 the desired value for row 1, column 0
     * @param m11 the desired value for row 1, column 1
     * @param m12 the desired value for row 1, column 2
     * @param m20 the desired value for row 2, column 0
     * @param m21 the desired value for row 2, column 1
     * @param m22 the desired value for row 2, column 2
     */
    public Matrix3f(float m00, float m01, float m02, float m10, float m11,
            float m12, float m20, float m21, float m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
    }

    /**
     * Instantiates a copy of the matrix argument. If the argument is null, an
     * identity matrix is produced.
     *
     * @param mat the matrix to copy (unaffected) or null for identity
     */
    public Matrix3f(Matrix3f mat) {
        set(mat);
    }

    /**
     * Replaces all 9 elements with their absolute values.
     */
    public void absoluteLocal() {
        m00 = FastMath.abs(m00);
        m01 = FastMath.abs(m01);
        m02 = FastMath.abs(m02);
        m10 = FastMath.abs(m10);
        m11 = FastMath.abs(m11);
        m12 = FastMath.abs(m12);
        m20 = FastMath.abs(m20);
        m21 = FastMath.abs(m21);
        m22 = FastMath.abs(m22);
    }

    /**
     * Copies the matrix argument. If the argument is null, the current instance
     * is set to identity (diagonals = 1, other elements = 0).
     *
     * @param matrix the matrix to copy (unaffected) or null for identity
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f set(Matrix3f matrix) {
        if (null == matrix) {
            loadIdentity();
        } else {
            m00 = matrix.m00;
            m01 = matrix.m01;
            m02 = matrix.m02;
            m10 = matrix.m10;
            m11 = matrix.m11;
            m12 = matrix.m12;
            m20 = matrix.m20;
            m21 = matrix.m21;
            m22 = matrix.m22;
        }
        return this;
    }

    /**
     * Returns the element at the specified position. The matrix is unaffected.
     *
     * @param i the row index (0, 1, or 2)
     * @param j the column index (0, 1, or 2)
     * @return the value of the element at (i, j)
     * @throws IllegalArgumentException if either index isn't 0, 1, or 2
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
                }
            case 1:
                switch (j) {
                    case 0:
                        return m10;
                    case 1:
                        return m11;
                    case 2:
                        return m12;
                }
            case 2:
                switch (j) {
                    case 0:
                        return m20;
                    case 1:
                        return m21;
                    case 2:
                        return m22;
                }
        }

        logger.warning("Invalid matrix index.");
        throw new IllegalArgumentException("Invalid indices into matrix.");
    }

    /**
     * Copies the matrix to the specified array. The matrix is unaffected.
     *
     * <p>If the array has 16 elements, then the matrix is treated as if it
     * contained the 1st 3 rows and 1st 3 columns of a 4x4 matrix.
     *
     * @param data storage for the elements (not null, length=9 or 16)
     * @param rowMajor true to store the elements in row-major order (m00, m01,
     *     ...) or false to store them in column-major order (m00, m10, ...)
     * @throws IndexOutOfBoundsException if {@code data} doesn't have 9 or 16
     *     elements
     * @see #fillFloatArray(float[], boolean)
     */
    public void get(float[] data, boolean rowMajor) {
        if (data.length == 9) {
            if (rowMajor) {
                data[0] = m00;
                data[1] = m01;
                data[2] = m02;
                data[3] = m10;
                data[4] = m11;
                data[5] = m12;
                data[6] = m20;
                data[7] = m21;
                data[8] = m22;
            } else {
                data[0] = m00;
                data[1] = m10;
                data[2] = m20;
                data[3] = m01;
                data[4] = m11;
                data[5] = m21;
                data[6] = m02;
                data[7] = m12;
                data[8] = m22;
            }
        } else if (data.length == 16) {
            if (rowMajor) {
                data[0] = m00;
                data[1] = m01;
                data[2] = m02;
                data[4] = m10;
                data[5] = m11;
                data[6] = m12;
                data[8] = m20;
                data[9] = m21;
                data[10] = m22;
            } else {
                data[0] = m00;
                data[1] = m10;
                data[2] = m20;
                data[4] = m01;
                data[5] = m11;
                data[6] = m21;
                data[8] = m02;
                data[9] = m12;
                data[10] = m22;
            }
        } else {
            throw new IndexOutOfBoundsException("Array size must be 9 or 16 in Matrix3f.get().");
        }
    }

    /**
     * Normalizes the matrix and returns the result in the argument. The current
     * instance is unaffected, unless it's {@code store}.
     *
     * @param store storage for the result, or null for a new Matrix3f
     * @return either {@code store} or a new Matrix3f
     */
    public Matrix3f normalize(Matrix3f store) {
        if (store == null) {
            store = new Matrix3f();
        }

        float mag = 1.0f / FastMath.sqrt(
                  m00 * m00
                + m10 * m10
                + m20 * m20);

        store.m00 = m00 * mag;
        store.m10 = m10 * mag;
        store.m20 = m20 * mag;

        mag = 1.0f / FastMath.sqrt(
                  m01 * m01
                + m11 * m11
                + m21 * m21);

        store.m01 = m01 * mag;
        store.m11 = m11 * mag;
        store.m21 = m21 * mag;

        store.m02 = store.m10 * store.m21 - store.m11 * store.m20;
        store.m12 = store.m01 * store.m20 - store.m00 * store.m21;
        store.m22 = store.m00 * store.m11 - store.m01 * store.m10;
        return store;
    }

    /**
     * Normalizes the matrix and returns the (modified) current instance.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f normalizeLocal() {
        return normalize(this);
    }

    /**
     * Returns the specified column. The matrix is unaffected.
     *
     * <p>If the matrix is a pure rotation, each column contains one of the
     * basis vectors.
     *
     * @param i the column index (0, 1, or 2)
     * @return a new Vector3f
     * @throws IllegalArgumentException if {@code i} isn't 0, 1, or 2
     * @see Quaternion#getRotationColumn(int)
     */
    public Vector3f getColumn(int i) {
        return getColumn(i, null);
    }

    /**
     * Returns the specified column. The matrix is unaffected.
     *
     * <p>If the matrix is a pure rotation, each column contains one of the
     * basis vectors.
     *
     * @param i the column index (0, 1, or 2)
     * @param store storage for the result, or null for a new Vector3f
     * @return either {@code store} or a new Vector3f
     * @throws IllegalArgumentException if {@code i} isn't 0, 1, or 2
     * @see Quaternion#getRotationColumn(int, com.jme3.math.Vector3f)
     */
    public Vector3f getColumn(int i, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        switch (i) {
            case 0:
                store.x = m00;
                store.y = m10;
                store.z = m20;
                break;
            case 1:
                store.x = m01;
                store.y = m11;
                store.z = m21;
                break;
            case 2:
                store.x = m02;
                store.y = m12;
                store.z = m22;
                break;
            default:
                logger.warning("Invalid column index.");
                throw new IllegalArgumentException("Invalid column index. " + i);
        }
        return store;
    }

    /**
     * Returns the specified row. The matrix is unaffected.
     *
     * @param i the row index (0, 1, or 2)
     * @return a new Vector3f
     * @throws IllegalArgumentException if {@code i} isn't 0, 1, or 2
     */
    public Vector3f getRow(int i) {
        return getRow(i, null);
    }

    /**
     * Returns the specified row. The matrix is unaffected.
     *
     * @param i the row index (0, 1, or 2)
     * @param store storage for the result, or null for a new Vector3f
     * @return either {@code store} or a new Vector3f
     * @throws IllegalArgumentException if {@code i} isn't 0, 1, or 2
     */
    public Vector3f getRow(int i, Vector3f store) {
        if (store == null) {
            store = new Vector3f();
        }
        switch (i) {
            case 0:
                store.x = m00;
                store.y = m01;
                store.z = m02;
                break;
            case 1:
                store.x = m10;
                store.y = m11;
                store.z = m12;
                break;
            case 2:
                store.x = m20;
                store.y = m21;
                store.z = m22;
                break;
            default:
                logger.warning("Invalid row index.");
                throw new IllegalArgumentException("Invalid row index. " + i);
        }
        return store;
    }

    /**
     * Copies the matrix to a new FloatBuffer. The matrix is unaffected.
     *
     * @return a new, rewound FloatBuffer containing all 9 elements in row-major
     *     order (m00, m01, ...)
     */
    public FloatBuffer toFloatBuffer() {
        FloatBuffer fb = BufferUtils.createFloatBuffer(9);

        fb.put(m00).put(m01).put(m02);
        fb.put(m10).put(m11).put(m12);
        fb.put(m20).put(m21).put(m22);
        fb.rewind();
        return fb;
    }

    /**
     * Copies the matrix to the specified FloatBuffer, starting at its current
     * position. The matrix is unaffected.
     *
     * @param fb storage for the elements (not null, must have space to put 9
     *     more floats)
     * @param columnMajor true to store the elements in column-major order (m00,
     *     m10, ...) or false to store them in row-major order (m00, m01, ...)
     * @return {@code fb}, its position advanced by 9
     */
    public FloatBuffer fillFloatBuffer(FloatBuffer fb, boolean columnMajor) {
//        if (columnMajor){
//            fb.put(m00).put(m10).put(m20);
//            fb.put(m01).put(m11).put(m21);
//            fb.put(m02).put(m12).put(m22);
//        }else{
//            fb.put(m00).put(m01).put(m02);
//            fb.put(m10).put(m11).put(m12);
//            fb.put(m20).put(m21).put(m22);
//        }

        TempVars vars = TempVars.get();

        fillFloatArray(vars.matrixWrite, columnMajor);
        fb.put(vars.matrixWrite, 0, 9);

        vars.release();

        return fb;
    }

    /**
     * Copies the matrix to the 1st 9 elements of the specified array. The
     * matrix is unaffected.
     *
     * @param f storage for the elements (not null, length&ge;9)
     * @param columnMajor true to store the elements in column-major order (m00,
     *     m10, ...) or false to store them in row-major order (m00, m01, ...)
     * @see #get(float[], boolean)
     */
    public void fillFloatArray(float[] f, boolean columnMajor) {
        if (columnMajor) {
            f[0] = m00;
            f[1] = m10;
            f[2] = m20;
            f[3] = m01;
            f[4] = m11;
            f[5] = m21;
            f[6] = m02;
            f[7] = m12;
            f[8] = m22;
        } else {
            f[0] = m00;
            f[1] = m01;
            f[2] = m02;
            f[3] = m10;
            f[4] = m11;
            f[5] = m12;
            f[6] = m20;
            f[7] = m21;
            f[8] = m22;
        }
    }

    /**
     * Sets the specified column.
     *
     * @param i which column to set (0, 1, or 2)
     * @param column the desired element values (unaffected) or null for none
     * @return the (modified) current instance (for chaining)
     * @throws IllegalArgumentException if {@code i} isn't 0, 1, or 2
     */
    public Matrix3f setColumn(int i, Vector3f column) {
        if (column == null) {
            logger.warning("Column is null. Ignoring.");
            return this;
        }
        switch (i) {
            case 0:
                m00 = column.x;
                m10 = column.y;
                m20 = column.z;
                break;
            case 1:
                m01 = column.x;
                m11 = column.y;
                m21 = column.z;
                break;
            case 2:
                m02 = column.x;
                m12 = column.y;
                m22 = column.z;
                break;
            default:
                logger.warning("Invalid column index.");
                throw new IllegalArgumentException("Invalid column index. " + i);
        }
        return this;
    }

    /**
     * Sets the specified row.
     *
     * @param i which row to set (0, 1, or 2)
     * @param row the desired element values (unaffected) or null for none
     * @return the (modified) current instance (for chaining)
     * @throws IllegalArgumentException if {@code i} isn't 0, 1, or 2
     */
    public Matrix3f setRow(int i, Vector3f row) {
        if (row == null) {
            logger.warning("Row is null. Ignoring.");
            return this;
        }
        switch (i) {
            case 0:
                m00 = row.x;
                m01 = row.y;
                m02 = row.z;
                break;
            case 1:
                m10 = row.x;
                m11 = row.y;
                m12 = row.z;
                break;
            case 2:
                m20 = row.x;
                m21 = row.y;
                m22 = row.z;
                break;
            default:
                logger.warning("Invalid row index.");
                throw new IllegalArgumentException("Invalid row index. " + i);
        }
        return this;
    }

    /**
     * Sets the specified element.
     *
     * @param i the row index (0, 1, or 2)
     * @param j the column index (0, 1, or 2)
     * @param value desired value for the element at (i, j)
     * @return the (modified) current instance (for chaining)
     * @throws IllegalArgumentException if either index isn't 0, 1, or 2
     */
    @SuppressWarnings("fallthrough")
    public Matrix3f set(int i, int j, float value) {
        switch (i) {
            case 0:
                switch (j) {
                    case 0:
                        m00 = value;
                        return this;
                    case 1:
                        m01 = value;
                        return this;
                    case 2:
                        m02 = value;
                        return this;
                }
            case 1:
                switch (j) {
                    case 0:
                        m10 = value;
                        return this;
                    case 1:
                        m11 = value;
                        return this;
                    case 2:
                        m12 = value;
                        return this;
                }
            case 2:
                switch (j) {
                    case 0:
                        m20 = value;
                        return this;
                    case 1:
                        m21 = value;
                        return this;
                    case 2:
                        m22 = value;
                        return this;
                }
        }

        logger.warning("Invalid matrix index.");
        throw new IllegalArgumentException("Invalid indices into matrix.");
    }

    /**
     * Copies all 9 elements from the specified 2-dimensional array.
     *
     * @param matrix the input array (not null, length=3, the first element
     *     having length=3, the other elements having length&ge;3, unaffected)
     * @return the (modified) current instance (for chaining)
     * @throws IllegalArgumentException if the array is the wrong size
     */
    public Matrix3f set(float[][] matrix) {
        if (matrix.length != 3 || matrix[0].length != 3) {
            throw new IllegalArgumentException(
                    "Array must be of size 9.");
        }

        m00 = matrix[0][0];
        m01 = matrix[0][1];
        m02 = matrix[0][2];
        m10 = matrix[1][0];
        m11 = matrix[1][1];
        m12 = matrix[1][2];
        m20 = matrix[2][0];
        m21 = matrix[2][1];
        m22 = matrix[2][2];

        return this;
    }

    /**
     * Configures from the specified column vectors. If the vectors form an
     * orthonormal basis, the result will be a pure rotation matrix.
     *
     * @param uAxis the desired value for column 0 (not null, unaffected)
     * @param vAxis the desired value for column 1 (not null, unaffected)
     * @param wAxis the desired value for column 2 (not null, unaffected)
     * @see Quaternion#fromAxes(com.jme3.math.Vector3f[])
     */
    public void fromAxes(Vector3f uAxis, Vector3f vAxis, Vector3f wAxis) {
        m00 = uAxis.x;
        m10 = uAxis.y;
        m20 = uAxis.z;

        m01 = vAxis.x;
        m11 = vAxis.y;
        m21 = vAxis.z;

        m02 = wAxis.x;
        m12 = wAxis.y;
        m22 = wAxis.z;
    }

    /**
     * Copies all 9 elements from the array argument, in row-major order.
     *
     * @param matrix the input array (not null, length=9, unaffected)
     * @return the (modified) current instance (for chaining)
     * @throws IllegalArgumentException if the array has length != 9
     */
    public Matrix3f set(float[] matrix) {
        return set(matrix, true);
    }

    /**
     * Copies all 9 elements from the specified array.
     *
     * @param matrix the input array (not null, length=9, unaffected)
     * @param rowMajor true to read the elements in row-major order (m00, m01,
     *     ...) or false to read them in column-major order (m00, m10, ...)
     * @return the (modified) current instance (for chaining)
     * @throws IllegalArgumentException if the array has length != 9
     */
    public Matrix3f set(float[] matrix, boolean rowMajor) {
        if (matrix.length != 9) {
            throw new IllegalArgumentException(
                    "Array must be of size 9.");
        }

        if (rowMajor) {
            m00 = matrix[0];
            m01 = matrix[1];
            m02 = matrix[2];
            m10 = matrix[3];
            m11 = matrix[4];
            m12 = matrix[5];
            m20 = matrix[6];
            m21 = matrix[7];
            m22 = matrix[8];
        } else {
            m00 = matrix[0];
            m01 = matrix[3];
            m02 = matrix[6];
            m10 = matrix[1];
            m11 = matrix[4];
            m12 = matrix[7];
            m20 = matrix[2];
            m21 = matrix[5];
            m22 = matrix[8];
        }
        return this;
    }

    /**
     * Configures as a rotation matrix equivalent to the argument.
     *
     * @param quaternion the input quaternion (not null, unaffected)
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f set(Quaternion quaternion) {
        return quaternion.toRotationMatrix(this);
    }

    /**
     * Configures as an identity matrix (diagonals = 1, other elements = 0).
     */
    public void loadIdentity() {
        m01 = m02 = m10 = m12 = m20 = m21 = 0;
        m00 = m11 = m22 = 1;
    }

    /**
     * Tests for exact identity. The matrix is unaffected.
     *
     * @return true if all diagonals = 1 and all other elements = 0 or -0,
     * otherwise false
     */
    public boolean isIdentity() {
        return (m00 == 1 && m01 == 0 && m02 == 0)
                && (m10 == 0 && m11 == 1 && m12 == 0)
                && (m20 == 0 && m21 == 0 && m22 == 1);
    }

    /**
     * Sets all 9 elements to form a pure rotation matrix with the specified
     * rotation angle and axis of rotation. This method creates garbage, so use
     * {@link #fromAngleNormalAxis(float, com.jme3.math.Vector3f)} if the axis
     * is known to be normalized.
     *
     * @param angle the desired rotation angle (in radians)
     * @param axis the desired axis of rotation (not null, unaffected)
     */
    public void fromAngleAxis(float angle, Vector3f axis) {
        Vector3f normAxis = axis.normalize();
        fromAngleNormalAxis(angle, normAxis);
    }

    /**
     * Sets all 9 elements to form a rotation matrix with the specified rotation
     * angle and normalized axis of rotation. If the axis might not be
     * normalized, use {@link #fromAngleAxis(float, com.jme3.math.Vector3f)}
     * instead.
     *
     * @param angle the desired rotation angle (in radians)
     * @param axis the desired axis of rotation (not null, length=1, unaffected)
     */
    public void fromAngleNormalAxis(float angle, Vector3f axis) {
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
     * Multiplies with the argument matrix and returns the product as a new
     * instance. The current instance is unaffected.
     *
     * <p>Note that matrix multiplication is noncommutative, so generally
     * q * p != p * q.
     *
     * @param mat the right factor (not null, unaffected)
     * @return {@code this} times {@code mat} (a new Matrix3f)
     */
    public Matrix3f mult(Matrix3f mat) {
        return mult(mat, null);
    }

    /**
     * Multiplies with the specified matrix and returns the product in a 3rd
     * matrix. The current instance is unaffected unless it's {@code product}.
     *
     * <p>Note that matrix multiplication is noncommutative, so generally
     * q * p != p * q.
     *
     * <p>It is safe for {@code mat} and {@code product} to be the same object.
     *
     * @param mat the right factor (not null, unaffected unless it's {@code
     *     product})
     * @param product storage for the product, or null for a new Matrix3f
     * @return {@code this} times {@code mat} (either {@code product} or a new
     *     Matrix3f)
     */
    public Matrix3f mult(Matrix3f mat, Matrix3f product) {
        float temp00, temp01, temp02;
        float temp10, temp11, temp12;
        float temp20, temp21, temp22;

        if (product == null) {
            product = new Matrix3f();
        }
        temp00 = m00 * mat.m00 + m01 * mat.m10 + m02 * mat.m20;
        temp01 = m00 * mat.m01 + m01 * mat.m11 + m02 * mat.m21;
        temp02 = m00 * mat.m02 + m01 * mat.m12 + m02 * mat.m22;
        temp10 = m10 * mat.m00 + m11 * mat.m10 + m12 * mat.m20;
        temp11 = m10 * mat.m01 + m11 * mat.m11 + m12 * mat.m21;
        temp12 = m10 * mat.m02 + m11 * mat.m12 + m12 * mat.m22;
        temp20 = m20 * mat.m00 + m21 * mat.m10 + m22 * mat.m20;
        temp21 = m20 * mat.m01 + m21 * mat.m11 + m22 * mat.m21;
        temp22 = m20 * mat.m02 + m21 * mat.m12 + m22 * mat.m22;

        product.m00 = temp00;
        product.m01 = temp01;
        product.m02 = temp02;
        product.m10 = temp10;
        product.m11 = temp11;
        product.m12 = temp12;
        product.m20 = temp20;
        product.m21 = temp21;
        product.m22 = temp22;

        return product;
    }

    /**
     * Applies the linear transformation to the vector argument and returns the
     * result as a new vector. The matrix is unaffected.
     *
     * <p>This can also be described as multiplying the matrix by a column
     * vector.
     *
     * @param vec the coordinates to transform (not null, unaffected)
     * @return a new Vector3f
     */
    public Vector3f mult(Vector3f vec) {
        return mult(vec, null);
    }

    /**
     * Applies the linear transformation to specified vector and stores the
     * result in another vector. The matrix is unaffected.
     *
     * <p>This can also be described as multiplying the matrix by a column
     * vector.
     *
     * <p>It is safe for {@code vec} and {@code product} to be the same object.
     *
     * @param vec the coordinates to transform (not null, unaffected unless it's
     *     {@code product})
     * @param product storage for the result, or null for a new Vector3f
     * @return either {@code product} or a new Vector3f
     */
    public Vector3f mult(Vector3f vec, Vector3f product) {
        if (null == product) {
            product = new Vector3f();
        }

        float x = vec.x;
        float y = vec.y;
        float z = vec.z;

        product.x = m00 * x + m01 * y + m02 * z;
        product.y = m10 * x + m11 * y + m12 * z;
        product.z = m20 * x + m21 * y + m22 * z;
        return product;
    }

    /**
     * Multiplies by the scalar argument and returns the (modified) current
     * instance.
     *
     * @param scale the scaling factor
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f multLocal(float scale) {
        m00 *= scale;
        m01 *= scale;
        m02 *= scale;
        m10 *= scale;
        m11 *= scale;
        m12 *= scale;
        m20 *= scale;
        m21 *= scale;
        m22 *= scale;
        return this;
    }

    /**
     * Applies the linear transformation to the vector argument and returns the
     * (modified) argument. If the argument is null, null is returned.
     *
     * <p>Despite the name, the current instance is unaffected.
     *
     * @param vec the vector to transform (modified if not null)
     * @return {@code vec} or null
     */
    public Vector3f multLocal(Vector3f vec) {
        if (vec == null) {
            return null;
        }
        float x = vec.x;
        float y = vec.y;
        vec.x = m00 * x + m01 * y + m02 * vec.z;
        vec.y = m10 * x + m11 * y + m12 * vec.z;
        vec.z = m20 * x + m21 * y + m22 * vec.z;
        return vec;
    }

    /**
     * Multiplies by the matrix argument and returns the (modified) current
     * instance.
     *
     * <p>Note that matrix multiplication is noncommutative, so generally
     * q * p != p * q.
     *
     * @param mat the right factor (not null, unaffected unless it's
     *     {@code this})
     * @return the (modified) current instance
     */
    public Matrix3f multLocal(Matrix3f mat) {
        return mult(mat, this);
    }

    /**
     * Transposes the matrix and returns the (modified) current instance.
     *
     * @return the (modified) current instance
     */
    public Matrix3f transposeLocal() {
//        float[] tmp = new float[9];
//        get(tmp, false);
//        set(tmp, true);

        float tmp = m01;
        m01 = m10;
        m10 = tmp;

        tmp = m02;
        m02 = m20;
        m20 = tmp;

        tmp = m12;
        m12 = m21;
        m21 = tmp;

        return this;
    }

    /**
     * Returns the multiplicative inverse as a new matrix. If the current
     * instance is singular, an all-zero matrix is returned. In either case, the
     * current instance is unaffected.
     *
     * @return a new Matrix3f
     */
    public Matrix3f invert() {
        return invert(null);
    }

    /**
     * Returns the multiplicative inverse in the specified storage. If the
     * current instance is singular, an all-zero matrix is returned. In either
     * case, the current instance is unaffected.
     *
     * <p>If {@code this} and {@code store} are the same object, the result is
     * undefined. Use {@link #invertLocal()} instead.
     *
     * @param store storage for the result, or null for a new Matrix3f
     * @return either {@code store} or a new Matrix3f
     */
    public Matrix3f invert(Matrix3f store) {
        if (store == null) {
            store = new Matrix3f();
        }

        float det = determinant();
        if (FastMath.abs(det) <= FastMath.FLT_EPSILON) {
            return store.zero();
        }

        store.m00 = m11 * m22 - m12 * m21;
        store.m01 = m02 * m21 - m01 * m22;
        store.m02 = m01 * m12 - m02 * m11;
        store.m10 = m12 * m20 - m10 * m22;
        store.m11 = m00 * m22 - m02 * m20;
        store.m12 = m02 * m10 - m00 * m12;
        store.m20 = m10 * m21 - m11 * m20;
        store.m21 = m01 * m20 - m00 * m21;
        store.m22 = m00 * m11 - m01 * m10;

        store.multLocal(1f / det);
        return store;
    }

    /**
     * Inverts the matrix and returns the (modified) current instance. If the
     * current instance is singular, all elements will be set to zero.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f invertLocal() {
        float det = determinant();
        if (FastMath.abs(det) <= 0f) {
            return zero();
        }

        float f00 = m11 * m22 - m12 * m21;
        float f01 = m02 * m21 - m01 * m22;
        float f02 = m01 * m12 - m02 * m11;
        float f10 = m12 * m20 - m10 * m22;
        float f11 = m00 * m22 - m02 * m20;
        float f12 = m02 * m10 - m00 * m12;
        float f20 = m10 * m21 - m11 * m20;
        float f21 = m01 * m20 - m00 * m21;
        float f22 = m00 * m11 - m01 * m10;

        m00 = f00;
        m01 = f01;
        m02 = f02;
        m10 = f10;
        m11 = f11;
        m12 = f12;
        m20 = f20;
        m21 = f21;
        m22 = f22;

        multLocal(1f / det);
        return this;
    }

    /**
     * Returns the adjoint as a new matrix. The current instance is unaffected.
     *
     * @return a new Matrix3f
     */
    public Matrix3f adjoint() {
        return adjoint(null);
    }

    /**
     * Returns the adjoint in the specified storage. The current instance is
     * unaffected.
     *
     * <p>If {@code this} and {@code store} are the same object, the result is
     * undefined.
     *
     * @param store storage for the result, or null for a new Matrix3f
     * @return either {@code store} or a new Matrix3f
     */
    public Matrix3f adjoint(Matrix3f store) {
        if (store == null) {
            store = new Matrix3f();
        }

        store.m00 = m11 * m22 - m12 * m21;
        store.m01 = m02 * m21 - m01 * m22;
        store.m02 = m01 * m12 - m02 * m11;
        store.m10 = m12 * m20 - m10 * m22;
        store.m11 = m00 * m22 - m02 * m20;
        store.m12 = m02 * m10 - m00 * m12;
        store.m20 = m10 * m21 - m11 * m20;
        store.m21 = m01 * m20 - m00 * m21;
        store.m22 = m00 * m11 - m01 * m10;

        return store;
    }

    /**
     * Returns the determinant. The matrix is unaffected.
     *
     * @return the determinant
     */
    public float determinant() {
        float fCo00 = m11 * m22 - m12 * m21;
        float fCo10 = m12 * m20 - m10 * m22;
        float fCo20 = m10 * m21 - m11 * m20;
        float fDet = m00 * fCo00 + m01 * fCo10 + m02 * fCo20;
        return fDet;
    }

    /**
     * Sets all elements to zero.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f zero() {
        m00 = m01 = m02 = m10 = m11 = m12 = m20 = m21 = m22 = 0.0f;
        return this;
    }

    /**
     * Transposes the matrix and returns the (modified) current instance.
     *
     * <p>This method is inconsistent with JME naming conventions, but has been
     * preserved for backwards compatibility. To preserve the current instance,
     * {@link #transposeNew()}.
     *
     * <p>TODO deprecate in favor of transposeLocal()
     *
     * @return the (modified) current instance (for chaining)
     */
    public Matrix3f transpose() {
        return transposeLocal();
    }

    /**
     * Returns the transpose as a new instance. The current instance is
     * unaffected.
     *
     * @return a new Matrix3f
     */
    public Matrix3f transposeNew() {
        Matrix3f ret = new Matrix3f(m00, m10, m20, m01, m11, m21, m02, m12, m22);
        return ret;
    }

    /**
     * Returns a string representation of the matrix, which is unaffected. For
     * example, the identity matrix is represented by:
     * <pre>
     * Matrix3f
     * [
     *  1.0  0.0  0.0
     *  0.0  1.0  0.0
     *  0.0  0.0  1.0
     * ]
     * </pre>
     *
     * @return the string representation (not null, not empty)
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("Matrix3f\n[\n");
        result.append(" ");
        result.append(m00);
        result.append("  ");
        result.append(m01);
        result.append("  ");
        result.append(m02);
        result.append(" \n");
        result.append(" ");
        result.append(m10);
        result.append("  ");
        result.append(m11);
        result.append("  ");
        result.append(m12);
        result.append(" \n");
        result.append(" ");
        result.append(m20);
        result.append("  ");
        result.append(m21);
        result.append("  ");
        result.append(m22);
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

        hash = 37 * hash + Float.floatToIntBits(m10);
        hash = 37 * hash + Float.floatToIntBits(m11);
        hash = 37 * hash + Float.floatToIntBits(m12);

        hash = 37 * hash + Float.floatToIntBits(m20);
        hash = 37 * hash + Float.floatToIntBits(m21);
        hash = 37 * hash + Float.floatToIntBits(m22);

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

        Matrix3f comp = (Matrix3f) o;
        if (Float.compare(m00, comp.m00) != 0) {
            return false;
        }
        if (Float.compare(m01, comp.m01) != 0) {
            return false;
        }
        if (Float.compare(m02, comp.m02) != 0) {
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

        if (Float.compare(m20, comp.m20) != 0) {
            return false;
        }
        if (Float.compare(m21, comp.m21) != 0) {
            return false;
        }
        if (Float.compare(m22, comp.m22) != 0) {
            return false;
        }

        return true;
    }

    /**
     * Serializes to the specified exporter, for example when saving to a J3O
     * file. The current instance is unaffected.
     *
     * @param e the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter e) throws IOException {
        OutputCapsule cap = e.getCapsule(this);
        cap.write(m00, "m00", 1);
        cap.write(m01, "m01", 0);
        cap.write(m02, "m02", 0);
        cap.write(m10, "m10", 0);
        cap.write(m11, "m11", 1);
        cap.write(m12, "m12", 0);
        cap.write(m20, "m20", 0);
        cap.write(m21, "m21", 0);
        cap.write(m22, "m22", 1);
    }

    /**
     * De-serializes from the specified importer, for example when loading from a
     * J3O file.
     *
     * @param importer the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter importer) throws IOException {
        InputCapsule cap = importer.getCapsule(this);
        m00 = cap.readFloat("m00", 1);
        m01 = cap.readFloat("m01", 0);
        m02 = cap.readFloat("m02", 0);
        m10 = cap.readFloat("m10", 0);
        m11 = cap.readFloat("m11", 1);
        m12 = cap.readFloat("m12", 0);
        m20 = cap.readFloat("m20", 0);
        m21 = cap.readFloat("m21", 0);
        m22 = cap.readFloat("m22", 1);
    }

    /**
     * Configures a rotation matrix that rotates the specified start direction
     * to the specified end direction.
     *
     * <p>See Tomas Möller, John F. Hughes "Efficiently Building a Matrix to
     * Rotate One Vector to Another" Journal of Graphics Tools, 4(4):1-4, 1999.
     *
     * @param start the start direction (not null, length=1, unaffected)
     * @param end the end direction (not null, length=1, unaffected)
     *
     */
    public void fromStartEndVectors(Vector3f start, Vector3f end) {
        Vector3f v = new Vector3f();
        float e, h, f;

        start.cross(end, v);
        e = start.dot(end);
        f = (e < 0) ? -e : e;

        // if "from" and "to" vectors are nearly parallel
        if (f > 1.0f - FastMath.ZERO_TOLERANCE) {
            Vector3f u = new Vector3f();
            Vector3f x = new Vector3f();
            float c1, c2, c3; /* coefficients for later use */
            int i, j;

            x.x = (start.x > 0.0) ? start.x : -start.x;
            x.y = (start.y > 0.0) ? start.y : -start.y;
            x.z = (start.z > 0.0) ? start.z : -start.z;

            if (x.x < x.y) {
                if (x.x < x.z) {
                    x.x = 1.0f;
                    x.y = x.z = 0.0f;
                } else {
                    x.z = 1.0f;
                    x.x = x.y = 0.0f;
                }
            } else {
                if (x.y < x.z) {
                    x.y = 1.0f;
                    x.x = x.z = 0.0f;
                } else {
                    x.z = 1.0f;
                    x.x = x.y = 0.0f;
                }
            }

            u.x = x.x - start.x;
            u.y = x.y - start.y;
            u.z = x.z - start.z;
            v.x = x.x - end.x;
            v.y = x.y - end.y;
            v.z = x.z - end.z;

            c1 = 2.0f / u.dot(u);
            c2 = 2.0f / v.dot(v);
            c3 = c1 * c2 * u.dot(v);

            for (i = 0; i < 3; i++) {
                for (j = 0; j < 3; j++) {
                    float val = -c1 * u.get(i) * u.get(j) - c2 * v.get(i)
                            * v.get(j) + c3 * v.get(i) * u.get(j);
                    set(i, j, val);
                }
                float val = get(i, i);
                set(i, i, val + 1.0f);
            }
        } else {
            // the most common case, unless "start"="end", or "start"=-"end"
            float hvx, hvz, hvxy, hvxz, hvyz;
            h = 1.0f / (1.0f + e);
            hvx = h * v.x;
            hvz = h * v.z;
            hvxy = hvx * v.y;
            hvxz = hvx * v.z;
            hvyz = hvz * v.y;
            set(0, 0, e + hvx * v.x);
            set(0, 1, hvxy - v.z);
            set(0, 2, hvxz + v.y);

            set(1, 0, hvxy + v.z);
            set(1, 1, e + h * v.y * v.y);
            set(1, 2, hvyz - v.x);

            set(2, 0, hvxz - v.y);
            set(2, 1, hvyz + v.x);
            set(2, 2, e + hvz * v.z);
        }
    }

    /**
     * Scales each column by the corresponding element of the argument.
     *
     * @param scale the scale factors: X scales column 0, Y scales column 1, Z
     *     scales column 2 (not null, unaffected)
     */
    public void scale(Vector3f scale) {
        m00 *= scale.x;
        m10 *= scale.x;
        m20 *= scale.x;
        m01 *= scale.y;
        m11 *= scale.y;
        m21 *= scale.y;
        m02 *= scale.z;
        m12 *= scale.z;
        m22 *= scale.z;
    }

    /**
     * Tests for an identity matrix, with 0.0001 tolerance. The current instance
     * is unaffected.
     *
     * @return true if all elements are within 0.0001 of an identity matrix
     */
    static boolean equalIdentity(Matrix3f mat) {
        if (Math.abs(mat.m00 - 1) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m11 - 1) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m22 - 1) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m01) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m02) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m10) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m12) > 1e-4) {
            return false;
        }

        if (Math.abs(mat.m20) > 1e-4) {
            return false;
        }
        if (Math.abs(mat.m21) > 1e-4) {
            return false;
        }

        return true;
    }

    /**
     * Creates a copy. The current instance is unaffected.
     *
     * @return a new instance, equivalent to the current one
     */
    @Override
    public Matrix3f clone() {
        try {
            return (Matrix3f) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); // can not happen
        }
    }
}
