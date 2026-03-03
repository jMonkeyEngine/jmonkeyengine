/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link Matrix3f} class works correctly.
 */
public class Matrix3fTest {

    private static final float TOLERANCE = 1e-6f;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultConstructorIsIdentity() {
        Matrix3f m = new Matrix3f();
        Assert.assertTrue(m.isIdentity());
    }

    @Test
    public void testParameterizedConstructor() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Assert.assertEquals(1f, m.get(0, 0), 0f);
        Assert.assertEquals(2f, m.get(0, 1), 0f);
        Assert.assertEquals(3f, m.get(0, 2), 0f);
        Assert.assertEquals(4f, m.get(1, 0), 0f);
        Assert.assertEquals(5f, m.get(1, 1), 0f);
        Assert.assertEquals(6f, m.get(1, 2), 0f);
        Assert.assertEquals(7f, m.get(2, 0), 0f);
        Assert.assertEquals(8f, m.get(2, 1), 0f);
        Assert.assertEquals(9f, m.get(2, 2), 0f);
    }

    @Test
    public void testCopyConstructor() {
        Matrix3f original = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f copy = new Matrix3f(original);
        Assert.assertEquals(original, copy);
        Assert.assertNotSame(original, copy);
    }

    @Test
    public void testCopyConstructorNull() {
        Matrix3f m = new Matrix3f(null);
        Assert.assertTrue(m.isIdentity());
    }

    // -----------------------------------------------------------------------
    // Identity / zero
    // -----------------------------------------------------------------------

    @Test
    public void testLoadIdentity() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        m.loadIdentity();
        Assert.assertTrue(m.isIdentity());
    }

    @Test
    public void testZero() {
        Matrix3f m = new Matrix3f();
        m.zero();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0f, m.get(i, j), 0f);
            }
        }
        Assert.assertFalse(m.isIdentity());
    }

    // -----------------------------------------------------------------------
    // set(Matrix3f) and set(int, int, float)
    // -----------------------------------------------------------------------

    @Test
    public void testSetMatrix() {
        Matrix3f source = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f target = new Matrix3f();
        Matrix3f result = target.set(source);
        Assert.assertSame(target, result);
        Assert.assertEquals(source, target);
    }

    @Test
    public void testSetMatrixNull() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        m.set((Matrix3f) null);
        Assert.assertTrue(m.isIdentity());
    }

    @Test
    public void testSetElement() {
        Matrix3f m = new Matrix3f();
        Matrix3f result = m.set(1, 2, 99f);
        Assert.assertSame(m, result);
        Assert.assertEquals(99f, m.get(1, 2), 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInvalidRow() {
        Matrix3f m = new Matrix3f();
        m.get(3, 0);
    }

    // -----------------------------------------------------------------------
    // absoluteLocal
    // -----------------------------------------------------------------------

    @Test
    public void testAbsoluteLocal() {
        Matrix3f m = new Matrix3f(
                -1f, 2f, -3f,
                4f, -5f, 6f,
                -7f, 8f, -9f);
        m.absoluteLocal();
        Assert.assertEquals(1f, m.get(0, 0), 0f);
        Assert.assertEquals(2f, m.get(0, 1), 0f);
        Assert.assertEquals(3f, m.get(0, 2), 0f);
        Assert.assertEquals(4f, m.get(1, 0), 0f);
        Assert.assertEquals(5f, m.get(1, 1), 0f);
        Assert.assertEquals(6f, m.get(1, 2), 0f);
        Assert.assertEquals(7f, m.get(2, 0), 0f);
        Assert.assertEquals(8f, m.get(2, 1), 0f);
        Assert.assertEquals(9f, m.get(2, 2), 0f);
    }

    // -----------------------------------------------------------------------
    // Determinant
    // -----------------------------------------------------------------------

    @Test
    public void testDeterminantIdentity() {
        Matrix3f m = new Matrix3f();
        Assert.assertEquals(1f, m.determinant(), TOLERANCE);
    }

    @Test
    public void testDeterminant() {
        // Simple scaling matrix: diag(2, 3, 4) -> det = 24
        Matrix3f m = new Matrix3f(
                2f, 0f, 0f,
                0f, 3f, 0f,
                0f, 0f, 4f);
        Assert.assertEquals(24f, m.determinant(), TOLERANCE);
    }

    @Test
    public void testDeterminantSingular() {
        // Row of zeros -> singular
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Assert.assertEquals(0f, m.determinant(), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Transpose
    // -----------------------------------------------------------------------

    @Test
    public void testTransposeNew() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f t = m.transposeNew();
        Assert.assertNotSame(m, t);
        // column i of m == row i of t
        Assert.assertEquals(m.get(0, 1), t.get(1, 0), 0f);
        Assert.assertEquals(m.get(1, 2), t.get(2, 1), 0f);
        Assert.assertEquals(m.get(2, 0), t.get(0, 2), 0f);
        // original unaffected
        Assert.assertEquals(2f, m.get(0, 1), 0f);
    }

    @Test
    public void testTransposeLocal() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f result = m.transposeLocal();
        Assert.assertSame(m, result);
        Assert.assertEquals(4f, m.get(0, 1), 0f);
        Assert.assertEquals(2f, m.get(1, 0), 0f);
        Assert.assertEquals(7f, m.get(0, 2), 0f);
        Assert.assertEquals(3f, m.get(2, 0), 0f);
    }

    @Test
    public void testDoubleTransposeIsOriginal() {
        Matrix3f original = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f copy = new Matrix3f(original);
        copy.transposeLocal().transposeLocal();
        Assert.assertEquals(original, copy);
    }

    // -----------------------------------------------------------------------
    // Invert
    // -----------------------------------------------------------------------

    @Test
    public void testInvertIdentity() {
        Matrix3f m = new Matrix3f();
        Matrix3f inv = m.invert();
        Assert.assertTrue(inv.isIdentity());
    }

    @Test
    public void testInvert() {
        // Diagonal matrix: inv = diag(1/2, 1/3, 1/4)
        Matrix3f m = new Matrix3f(
                2f, 0f, 0f,
                0f, 3f, 0f,
                0f, 0f, 4f);
        Matrix3f inv = m.invert();
        Assert.assertEquals(0.5f, inv.get(0, 0), TOLERANCE);
        Assert.assertEquals(1f / 3f, inv.get(1, 1), TOLERANCE);
        Assert.assertEquals(0.25f, inv.get(2, 2), TOLERANCE);
    }

    @Test
    public void testInvertTimesOriginalIsIdentity() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 0f,
                0f, 1f, 3f,
                0f, 0f, 1f);
        Matrix3f inv = m.invert();
        Matrix3f product = m.mult(inv);
        Assert.assertTrue(product.isIdentity());
    }

    @Test
    public void testInvertSingular() {
        // Row of zeros -> singular, should return all-zero matrix
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f inv = m.invert();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0f, inv.get(i, j), 0f);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Multiply
    // -----------------------------------------------------------------------

    @Test
    public void testMultIdentity() {
        Matrix3f a = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f identity = new Matrix3f();
        Matrix3f result = a.mult(identity);
        Assert.assertEquals(a, result);
    }

    @Test
    public void testMult() {
        // [[1,0],[0,2]] * [[3,0],[0,4]] = [[3,0],[0,8]]
        Matrix3f a = new Matrix3f(
                1f, 0f, 0f,
                0f, 2f, 0f,
                0f, 0f, 1f);
        Matrix3f b = new Matrix3f(
                3f, 0f, 0f,
                0f, 4f, 0f,
                0f, 0f, 1f);
        Matrix3f result = a.mult(b);
        Assert.assertEquals(3f, result.get(0, 0), TOLERANCE);
        Assert.assertEquals(8f, result.get(1, 1), TOLERANCE);
        Assert.assertEquals(1f, result.get(2, 2), TOLERANCE);
        Assert.assertEquals(0f, result.get(0, 1), TOLERANCE);
    }

    @Test
    public void testMultVector() {
        Matrix3f m = new Matrix3f();  // identity
        Vector3f v = new Vector3f(1f, 2f, 3f);
        Vector3f result = m.mult(v);
        Assert.assertEquals(1f, result.x, TOLERANCE);
        Assert.assertEquals(2f, result.y, TOLERANCE);
        Assert.assertEquals(3f, result.z, TOLERANCE);
    }

    @Test
    public void testMultVectorScaling() {
        Matrix3f m = new Matrix3f(
                2f, 0f, 0f,
                0f, 3f, 0f,
                0f, 0f, 4f);
        Vector3f v = new Vector3f(1f, 1f, 1f);
        Vector3f result = m.mult(v);
        Assert.assertEquals(2f, result.x, TOLERANCE);
        Assert.assertEquals(3f, result.y, TOLERANCE);
        Assert.assertEquals(4f, result.z, TOLERANCE);
    }

    @Test
    public void testMultLocalScalar() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f result = m.multLocal(2f);
        Assert.assertSame(m, result);
        Assert.assertEquals(2f, m.get(0, 0), TOLERANCE);
        Assert.assertEquals(4f, m.get(0, 1), TOLERANCE);
        Assert.assertEquals(18f, m.get(2, 2), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Columns and rows
    // -----------------------------------------------------------------------

    @Test
    public void testGetColumn() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Vector3f col0 = m.getColumn(0);
        Assert.assertEquals(1f, col0.x, 0f);
        Assert.assertEquals(4f, col0.y, 0f);
        Assert.assertEquals(7f, col0.z, 0f);
    }

    @Test
    public void testGetRow() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Vector3f row1 = m.getRow(1);
        Assert.assertEquals(4f, row1.x, 0f);
        Assert.assertEquals(5f, row1.y, 0f);
        Assert.assertEquals(6f, row1.z, 0f);
    }

    @Test
    public void testSetColumn() {
        Matrix3f m = new Matrix3f();
        Vector3f col = new Vector3f(1f, 2f, 3f);
        m.setColumn(0, col);
        Assert.assertEquals(1f, m.get(0, 0), 0f);
        Assert.assertEquals(2f, m.get(1, 0), 0f);
        Assert.assertEquals(3f, m.get(2, 0), 0f);
    }

    @Test
    public void testSetRow() {
        Matrix3f m = new Matrix3f();
        Vector3f row = new Vector3f(7f, 8f, 9f);
        m.setRow(2, row);
        Assert.assertEquals(7f, m.get(2, 0), 0f);
        Assert.assertEquals(8f, m.get(2, 1), 0f);
        Assert.assertEquals(9f, m.get(2, 2), 0f);
    }

    // -----------------------------------------------------------------------
    // fromAngleAxis
    // -----------------------------------------------------------------------

    @Test
    public void testFromAngleAxisRotation90DegreesAroundZ() {
        Matrix3f m = new Matrix3f();
        m.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
        // Rotating (1,0,0) by 90 degrees around Z should give ~(0,1,0)
        Vector3f result = m.mult(new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(0f, result.x, TOLERANCE);
        Assert.assertEquals(1f, result.y, TOLERANCE);
        Assert.assertEquals(0f, result.z, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // get/set float arrays
    // -----------------------------------------------------------------------

    @Test
    public void testGetFloatArrayRowMajor() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        float[] data = new float[9];
        m.get(data, true);
        Assert.assertEquals(1f, data[0], 0f);
        Assert.assertEquals(2f, data[1], 0f);
        Assert.assertEquals(3f, data[2], 0f);
        Assert.assertEquals(4f, data[3], 0f);
        Assert.assertEquals(9f, data[8], 0f);
    }

    @Test
    public void testGetFloatArrayColumnMajor() {
        Matrix3f m = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        float[] data = new float[9];
        m.get(data, false);
        // column-major: first column (m00, m10, m20) = (1, 4, 7)
        Assert.assertEquals(1f, data[0], 0f);
        Assert.assertEquals(4f, data[1], 0f);
        Assert.assertEquals(7f, data[2], 0f);
    }

    // -----------------------------------------------------------------------
    // Clone and equals
    // -----------------------------------------------------------------------

    @Test
    public void testClone() {
        Matrix3f original = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original, cloned);
    }

    @Test
    public void testEqualsAndHashCode() {
        Matrix3f a = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f b = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 9f);
        Matrix3f c = new Matrix3f(
                1f, 2f, 3f,
                4f, 5f, 6f,
                7f, 8f, 0f);
        Assert.assertEquals(a, b);
        Assert.assertNotEquals(a, c);
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    // -----------------------------------------------------------------------
    // Static constants
    // -----------------------------------------------------------------------

    @Test
    public void testStaticConstants() {
        Assert.assertTrue(Matrix3f.IDENTITY.isIdentity());
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Assert.assertEquals(0f, Matrix3f.ZERO.get(i, j), 0f);
            }
        }
    }
}
