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
 * Verifies that the {@link Matrix4f} class works correctly.
 */
public class Matrix4fTest {

    private static final float TOLERANCE = 1e-5f;

    /** Helper: compare two Matrix4f element-by-element. */
    private static void assertMatricesEqual(Matrix4f expected, Matrix4f actual, float delta) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Assert.assertEquals(
                        "element (" + i + "," + j + ")",
                        expected.get(i, j), actual.get(i, j), delta);
            }
        }
    }

    /** Helper: build identity-like check (each diagonal = 1, off-diagonal = 0). */
    private static void assertIsIdentity(Matrix4f m, float delta) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                float expected = (i == j) ? 1f : 0f;
                Assert.assertEquals(
                        "element (" + i + "," + j + ")",
                        expected, m.get(i, j), delta);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultConstructorIsIdentity() {
        Matrix4f m = new Matrix4f();
        assertIsIdentity(m, 0f);
    }

    @Test
    public void testCopyConstructor() {
        Matrix4f original = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f copy = new Matrix4f(original);
        assertMatricesEqual(original, copy, 0f);
        Assert.assertNotSame(original, copy);
    }

    @Test
    public void testCopyConstructorNull() {
        Matrix4f m = new Matrix4f((Matrix4f) null);
        assertIsIdentity(m, 0f);
    }

    @Test
    public void testParameterizedConstructor() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Assert.assertEquals(1f, m.get(0, 0), 0f);
        Assert.assertEquals(4f, m.get(0, 3), 0f);
        Assert.assertEquals(16f, m.get(3, 3), 0f);
    }

    // -----------------------------------------------------------------------
    // loadIdentity / zero
    // -----------------------------------------------------------------------

    @Test
    public void testLoadIdentity() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        m.loadIdentity();
        assertIsIdentity(m, 0f);
    }

    @Test
    public void testZero() {
        Matrix4f m = new Matrix4f();
        Matrix4f result = m.zero();
        Assert.assertSame(m, result);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Assert.assertEquals(0f, m.get(i, j), 0f);
            }
        }
    }

    // -----------------------------------------------------------------------
    // set / copy
    // -----------------------------------------------------------------------

    @Test
    public void testSetMatrix() {
        Matrix4f source = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f target = new Matrix4f();
        Matrix4f result = target.set(source);
        Assert.assertSame(target, result);
        assertMatricesEqual(source, target, 0f);
    }

    @Test
    public void testCopyNull() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        m.copy(null);
        assertIsIdentity(m, 0f);
    }

    // -----------------------------------------------------------------------
    // Determinant
    // -----------------------------------------------------------------------

    @Test
    public void testDeterminantIdentity() {
        Matrix4f m = new Matrix4f();
        Assert.assertEquals(1f, m.determinant(), TOLERANCE);
    }

    @Test
    public void testDeterminantDiagonal() {
        // diag(2,3,4,5) -> det = 120
        Matrix4f m = new Matrix4f(
                2f, 0f, 0f, 0f,
                0f, 3f, 0f, 0f,
                0f, 0f, 4f, 0f,
                0f, 0f, 0f, 5f);
        Assert.assertEquals(120f, m.determinant(), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Transpose
    // -----------------------------------------------------------------------

    @Test
    public void testTranspose() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f t = m.transpose();
        // m[i][j] should equal t[j][i]
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Assert.assertEquals(m.get(i, j), t.get(j, i), 0f);
            }
        }
    }

    @Test
    public void testTransposeLocal() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f original = new Matrix4f(m);
        Matrix4f result = m.transposeLocal();
        Assert.assertSame(m, result);
        // Verify transpose
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Assert.assertEquals(original.get(i, j), m.get(j, i), 0f);
            }
        }
    }

    @Test
    public void testDoubleTransposeIsOriginal() {
        Matrix4f original = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f copy = new Matrix4f(original);
        copy.transposeLocal().transposeLocal();
        assertMatricesEqual(original, copy, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Multiply
    // -----------------------------------------------------------------------

    @Test
    public void testMultIdentity() {
        Matrix4f a = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f identity = new Matrix4f();
        Matrix4f result = a.mult(identity);
        assertMatricesEqual(a, result, TOLERANCE);
    }

    @Test
    public void testMultScalar() {
        Matrix4f m = new Matrix4f();
        Matrix4f result = m.mult(2f);
        Assert.assertEquals(2f, result.get(0, 0), TOLERANCE);
        Assert.assertEquals(2f, result.get(1, 1), TOLERANCE);
        Assert.assertEquals(0f, result.get(0, 1), TOLERANCE);
    }

    @Test
    public void testMultVector3f() {
        Matrix4f m = new Matrix4f(); // identity
        Vector3f v = new Vector3f(1f, 2f, 3f);
        Vector3f result = m.mult(v);
        Assert.assertEquals(1f, result.x, TOLERANCE);
        Assert.assertEquals(2f, result.y, TOLERANCE);
        Assert.assertEquals(3f, result.z, TOLERANCE);
    }

    @Test
    public void testMultVector4f() {
        Matrix4f m = new Matrix4f(); // identity
        Vector4f v = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = m.mult(v);
        Assert.assertEquals(1f, result.x, TOLERANCE);
        Assert.assertEquals(2f, result.y, TOLERANCE);
        Assert.assertEquals(3f, result.z, TOLERANCE);
        Assert.assertEquals(4f, result.w, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Invert
    // -----------------------------------------------------------------------

    @Test
    public void testInvertIdentity() {
        Matrix4f m = new Matrix4f();
        Matrix4f inv = m.invert();
        assertIsIdentity(inv, TOLERANCE);
    }

    @Test
    public void testInvertTimesOriginalIsIdentity() {
        // Simple non-trivial invertible matrix
        Matrix4f m = new Matrix4f(
                1f, 0f, 0f, 5f,
                0f, 1f, 0f, 3f,
                0f, 0f, 1f, 2f,
                0f, 0f, 0f, 1f);
        Matrix4f inv = m.invert();
        Matrix4f product = m.mult(inv);
        assertIsIdentity(product, TOLERANCE);
    }

    @Test(expected = ArithmeticException.class)
    public void testInvertSingularThrows() {
        Matrix4f m = new Matrix4f();
        m.zero(); // all zeros is singular
        m.invert();
    }

    // -----------------------------------------------------------------------
    // add / addLocal
    // -----------------------------------------------------------------------

    @Test
    public void testAdd() {
        Matrix4f a = new Matrix4f();   // identity
        Matrix4f b = new Matrix4f();   // identity
        Matrix4f result = a.add(b);
        // result should be identity + identity = 2*identity
        Assert.assertEquals(2f, result.get(0, 0), TOLERANCE);
        Assert.assertEquals(0f, result.get(0, 1), TOLERANCE);
        Assert.assertEquals(2f, result.get(3, 3), TOLERANCE);
    }

    @Test
    public void testAddLocal() {
        Matrix4f a = new Matrix4f();   // identity
        Matrix4f b = new Matrix4f();   // identity
        a.addLocal(b);
        Assert.assertEquals(2f, a.get(0, 0), TOLERANCE);
        Assert.assertEquals(0f, a.get(0, 1), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Translation
    // -----------------------------------------------------------------------

    @Test
    public void testToTranslationVector() {
        Matrix4f m = new Matrix4f();
        m.setTranslation(new Vector3f(1f, 2f, 3f));
        Vector3f translation = m.toTranslationVector();
        Assert.assertEquals(1f, translation.x, TOLERANCE);
        Assert.assertEquals(2f, translation.y, TOLERANCE);
        Assert.assertEquals(3f, translation.z, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // get(float[]) round-trip
    // -----------------------------------------------------------------------

    @Test
    public void testGetFloatArrayRowMajor() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        float[] data = new float[16];
        m.get(data, true);
        Assert.assertEquals(1f, data[0], 0f);
        Assert.assertEquals(2f, data[1], 0f);
        Assert.assertEquals(4f, data[3], 0f);
        Assert.assertEquals(16f, data[15], 0f);
    }

    @Test
    public void testGetFloatArrayColumnMajor() {
        Matrix4f m = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        float[] data = new float[16];
        m.get(data, false);
        // column-major: first column is (m00, m10, m20, m30) = (1, 5, 9, 13)
        Assert.assertEquals(1f, data[0], 0f);
        Assert.assertEquals(5f, data[1], 0f);
        Assert.assertEquals(9f, data[2], 0f);
        Assert.assertEquals(13f, data[3], 0f);
    }

    // -----------------------------------------------------------------------
    // fromAngleAxis
    // -----------------------------------------------------------------------

    @Test
    public void testFromAngleAxisRotation90DegreesAroundY() {
        Matrix4f m = new Matrix4f();
        m.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        // Rotating (1,0,0) by 90 degrees around Y should give ~(0,0,-1)
        Vector3f v = new Vector3f(1f, 0f, 0f);
        Vector3f result = m.mult(v);
        Assert.assertEquals(0f, result.x, TOLERANCE);
        Assert.assertEquals(0f, result.y, TOLERANCE);
        Assert.assertEquals(-1f, result.z, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Clone and equals
    // -----------------------------------------------------------------------

    @Test
    public void testClone() {
        Matrix4f original = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        assertMatricesEqual(original, cloned, 0f);
    }

    @Test
    public void testEqualsAndHashCode() {
        Matrix4f a = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f b = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 16f);
        Matrix4f c = new Matrix4f(
                1f, 2f, 3f, 4f,
                5f, 6f, 7f, 8f,
                9f, 10f, 11f, 12f,
                13f, 14f, 15f, 0f);
        Assert.assertEquals(a, b);
        Assert.assertNotEquals(a, c);
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    // -----------------------------------------------------------------------
    // Static constants
    // -----------------------------------------------------------------------

    @Test
    public void testStaticConstants() {
        assertIsIdentity(Matrix4f.IDENTITY, 0f);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                Assert.assertEquals(0f, Matrix4f.ZERO.get(i, j), 0f);
            }
        }
    }
}
