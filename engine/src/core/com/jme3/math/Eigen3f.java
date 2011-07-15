/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

import java.util.logging.Level;
import java.util.logging.Logger;

public class Eigen3f implements java.io.Serializable {

    static final long serialVersionUID = 1;
    
    private static final Logger logger = Logger.getLogger(Eigen3f.class
            .getName());

    float[] eigenValues = new float[3];
    Vector3f[] eigenVectors = new Vector3f[3];

    static final double ONE_THIRD_DOUBLE = 1.0 / 3.0;
    static final double ROOT_THREE_DOUBLE = Math.sqrt(3.0);

    
    public Eigen3f() {
    	
    }
    
    public Eigen3f(Matrix3f data) {
        calculateEigen(data);
    }

	public void calculateEigen(Matrix3f data) {
		// prep work...
        eigenVectors[0] = new Vector3f();
        eigenVectors[1] = new Vector3f();
        eigenVectors[2] = new Vector3f();

        Matrix3f scaledData = new Matrix3f(data);
        float maxMagnitude = scaleMatrix(scaledData);

        // Compute the eigenvalues using double-precision arithmetic.
        double roots[] = new double[3];
        computeRoots(scaledData, roots);
        eigenValues[0] = (float) roots[0];
        eigenValues[1] = (float) roots[1];
        eigenValues[2] = (float) roots[2];

        float[] maxValues = new float[3];
        Vector3f[] maxRows = new Vector3f[3];
        maxRows[0] = new Vector3f();
        maxRows[1] = new Vector3f();
        maxRows[2] = new Vector3f();
        
        for (int i = 0; i < 3; i++) {
            Matrix3f tempMatrix = new Matrix3f(scaledData);
            tempMatrix.m00 -= eigenValues[i];
            tempMatrix.m11 -= eigenValues[i];
            tempMatrix.m22 -= eigenValues[i];
            float[] val = new float[1];
            val[0] = maxValues[i];
            if (!positiveRank(tempMatrix, val, maxRows[i])) {
                // Rank was 0 (or very close to 0), so just return.
                // Rescale back to the original size.
                if (maxMagnitude > 1f) {
                    for (int j = 0; j < 3; j++) {
                        eigenValues[j] *= maxMagnitude;
                    }
                }

                eigenVectors[0].set(Vector3f.UNIT_X);
                eigenVectors[1].set(Vector3f.UNIT_Y);
                eigenVectors[2].set(Vector3f.UNIT_Z);
                return;
            }
            maxValues[i] = val[0];
        }

        float maxCompare = maxValues[0];
        int i = 0;
        if (maxValues[1] > maxCompare) {
            maxCompare = maxValues[1];
            i = 1;
        }
        if (maxValues[2] > maxCompare) {
            i = 2;
        }

        // use the largest row to compute and order the eigen vectors.
        switch (i) {
            case 0:
                maxRows[0].normalizeLocal();
                computeVectors(scaledData, maxRows[0], 1, 2, 0);
                break;
            case 1:
                maxRows[1].normalizeLocal();
                computeVectors(scaledData, maxRows[1], 2, 0, 1);
                break;
            case 2:
                maxRows[2].normalizeLocal();
                computeVectors(scaledData, maxRows[2], 0, 1, 2);
                break;
        }

        // Rescale the values back to the original size.
        if (maxMagnitude > 1f) {
            for (i = 0; i < 3; i++) {
                eigenValues[i] *= maxMagnitude;
            }
        }
	}

    /**
     * Scale the matrix so its entries are in [-1,1]. The scaling is applied
     * only when at least one matrix entry has magnitude larger than 1.
     * 
     * @return the max magnitude in this matrix
     */
    private float scaleMatrix(Matrix3f mat) {

        float max = FastMath.abs(mat.m00);
        float abs = FastMath.abs(mat.m01);

        if (abs > max) {
            max = abs;
        }
        abs = FastMath.abs(mat.m02);
        if (abs > max) {
            max = abs;
        }
        abs = FastMath.abs(mat.m11);
        if (abs > max) {
            max = abs;
        }
        abs = FastMath.abs(mat.m12);
        if (abs > max) {
            max = abs;
        }
        abs = FastMath.abs(mat.m22);
        if (abs > max) {
            max = abs;
        }

        if (max > 1f) {
            float fInvMax = 1f / max;
            mat.multLocal(fInvMax);
        }

        return max;
    }

    /**
     * Compute the eigenvectors of the given Matrix, using the 
     * @param mat
     * @param vect
     * @param index1
     * @param index2
     * @param index3
     */
    private void computeVectors(Matrix3f mat, Vector3f vect, int index1,
            int index2, int index3) {
        Vector3f vectorU = new Vector3f(), vectorV = new Vector3f();
        Vector3f.generateComplementBasis(vectorU, vectorV, vect);

        Vector3f tempVect = mat.mult(vectorU);
        float p00 = eigenValues[index3] - vectorU.dot(tempVect);
        float p01 = vectorV.dot(tempVect);
        float p11 = eigenValues[index3] - vectorV.dot(mat.mult(vectorV));
        float invLength;
        float max = FastMath.abs(p00);
        int row = 0;
        float fAbs = FastMath.abs(p01);
        if (fAbs > max) {
            max = fAbs;
        }
        fAbs = FastMath.abs(p11);
        if (fAbs > max) {
            max = fAbs;
            row = 1;
        }

        if (max >= FastMath.ZERO_TOLERANCE) {
            if (row == 0) {
                invLength = FastMath.invSqrt(p00 * p00 + p01 * p01);
                p00 *= invLength;
                p01 *= invLength;
                vectorU.mult(p01, eigenVectors[index3])
                        .addLocal(vectorV.mult(p00));
            } else {
                invLength = FastMath.invSqrt(p11 * p11 + p01 * p01);
                p11 *= invLength;
                p01 *= invLength;
                vectorU.mult(p11, eigenVectors[index3])
                        .addLocal(vectorV.mult(p01));
            }
        } else {
            if (row == 0) {
                eigenVectors[index3] = vectorV;
            } else {
                eigenVectors[index3] = vectorU;
            }
        }

        Vector3f vectorS = vect.cross(eigenVectors[index3]);
        mat.mult(vect, tempVect);
        p00 = eigenValues[index1] - vect.dot(tempVect);
        p01 = vectorS.dot(tempVect);
        p11 = eigenValues[index1] - vectorS.dot(mat.mult(vectorS));
        max = FastMath.abs(p00);
        row = 0;
        fAbs = FastMath.abs(p01);
        if (fAbs > max) {
            max = fAbs;
        }
        fAbs = FastMath.abs(p11);
        if (fAbs > max) {
            max = fAbs;
            row = 1;
        }

        if (max >= FastMath.ZERO_TOLERANCE) {
            if (row == 0) {
                invLength = FastMath.invSqrt(p00 * p00 + p01 * p01);
                p00 *= invLength;
                p01 *= invLength;
                eigenVectors[index1] = vect.mult(p01).add(vectorS.mult(p00));
            } else {
                invLength = FastMath.invSqrt(p11 * p11 + p01 * p01);
                p11 *= invLength;
                p01 *= invLength;
                eigenVectors[index1] = vect.mult(p11).add(vectorS.mult(p01));
            }
        } else {
            if (row == 0) {
                eigenVectors[index1].set(vectorS);
            } else {
                eigenVectors[index1].set(vect);
            }
        }

         eigenVectors[index3].cross(eigenVectors[index1], eigenVectors[index2]);
    }

    /**
     * Check the rank of the given Matrix to determine if it is positive. While
     * doing so, store the max magnitude entry in the given float store and the
     * max row of the matrix in the Vector store.
     * 
     * @param matrix
     *            the Matrix3f to analyze.
     * @param maxMagnitudeStore
     *            a float array in which to store (in the 0th position) the max
     *            magnitude entry of the matrix.
     * @param maxRowStore
     *            a Vector3f to store the values of the row of the matrix
     *            containing the max magnitude entry.
     * @return true if the given matrix has a non 0 rank.
     */
    private boolean positiveRank(Matrix3f matrix, float[] maxMagnitudeStore, Vector3f maxRowStore) {
        // Locate the maximum-magnitude entry of the matrix.
        maxMagnitudeStore[0] = -1f;
        int iRow, iCol, iMaxRow = -1;
        for (iRow = 0; iRow < 3; iRow++) {
            for (iCol = iRow; iCol < 3; iCol++) {
                float fAbs = FastMath.abs(matrix.get(iRow, iCol));
                if (fAbs > maxMagnitudeStore[0]) {
                    maxMagnitudeStore[0] = fAbs;
                    iMaxRow = iRow;
                }
            }
        }

        // Return the row containing the maximum, to be used for eigenvector
        // construction.
        maxRowStore.set(matrix.getRow(iMaxRow));

        return maxMagnitudeStore[0] >= FastMath.ZERO_TOLERANCE;
    }

    /**
     * Generate the base eigen values of the given matrix using double precision
     * math.
     * 
     * @param mat
     *            the Matrix3f to analyze.
     * @param rootsStore
     *            a double array to store the results in. Must be at least
     *            length 3.
     */
    private void computeRoots(Matrix3f mat, double[] rootsStore) {
        // Convert the unique matrix entries to double precision.
        double a = mat.m00, b = mat.m01, c = mat.m02,
                            d = mat.m11, e = mat.m12,
                                         f = mat.m22;

        // The characteristic equation is x^3 - c2*x^2 + c1*x - c0 = 0. The
        // eigenvalues are the roots to this equation, all guaranteed to be
        // real-valued, because the matrix is symmetric.
        double char0 = a * d * f + 2.0 * b * c * e - a
                * e * e - d * c * c - f * b * b;

        double char1 = a * d - b * b + a * f - c * c
                + d * f - e * e;

        double char2 = a + d + f;

        // Construct the parameters used in classifying the roots of the
        // equation and in solving the equation for the roots in closed form.
        double char2Div3 = char2 * ONE_THIRD_DOUBLE;
        double abcDiv3 = (char1 - char2 * char2Div3) * ONE_THIRD_DOUBLE;
        if (abcDiv3 > 0.0) {
            abcDiv3 = 0.0;
        }

        double mbDiv2 = 0.5 * (char0 + char2Div3 * (2.0 * char2Div3 * char2Div3 - char1));

        double q = mbDiv2 * mbDiv2 + abcDiv3 * abcDiv3 * abcDiv3;
        if (q > 0.0) {
            q = 0.0;
        }

        // Compute the eigenvalues by solving for the roots of the polynomial.
        double magnitude = Math.sqrt(-abcDiv3);
        double angle = Math.atan2(Math.sqrt(-q), mbDiv2) * ONE_THIRD_DOUBLE;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double root0 = char2Div3 + 2.0 * magnitude * cos;
        double root1 = char2Div3 - magnitude
                * (cos + ROOT_THREE_DOUBLE * sin);
        double root2 = char2Div3 - magnitude
                * (cos - ROOT_THREE_DOUBLE * sin);

        // Sort in increasing order.
        if (root1 >= root0) {
            rootsStore[0] = root0;
            rootsStore[1] = root1;
        } else {
            rootsStore[0] = root1;
            rootsStore[1] = root0;
        }

        if (root2 >= rootsStore[1]) {
            rootsStore[2] = root2;
        } else {
            rootsStore[2] = rootsStore[1];
            if (root2 >= rootsStore[0]) {
                rootsStore[1] = root2;
            } else {
                rootsStore[1] = rootsStore[0];
                rootsStore[0] = root2;
            }
        }
    }

    public static void main(String[] args) {
        Matrix3f mat = new Matrix3f(2, 1, 1, 1, 2, 1, 1, 1, 2);
        Eigen3f eigenSystem = new Eigen3f(mat);

        logger.info("eigenvalues = ");
        for (int i = 0; i < 3; i++)
            logger.log(Level.INFO, "{0} ", eigenSystem.getEigenValue(i));

        logger.info("eigenvectors = ");
        for (int i = 0; i < 3; i++) {
            Vector3f vector = eigenSystem.getEigenVector(i);
            logger.info(vector.toString());
            mat.setColumn(i, vector);
        }
        logger.info(mat.toString());

        // eigenvalues =
        // 1.000000 1.000000 4.000000
        // eigenvectors =
        // 0.411953 0.704955 0.577350
        // 0.404533 -0.709239 0.577350
        // -0.816485 0.004284 0.577350
    }

    public float getEigenValue(int i) {
        return eigenValues[i];
    }

    public Vector3f getEigenVector(int i) {
        return eigenVectors[i];
    }

    public float[] getEigenValues() {
        return eigenValues;
    }

    public Vector3f[] getEigenVectors() {
        return eigenVectors;
    }
}
