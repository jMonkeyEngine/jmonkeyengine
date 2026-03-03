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
 * Verifies that the {@link Vector2f} class works correctly.
 */
public class Vector2fTest {

    private static final float TOLERANCE = 1e-6f;

    // -----------------------------------------------------------------------
    // Constructors and set
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultConstructor() {
        Vector2f v = new Vector2f();
        Assert.assertEquals(0f, v.x, 0f);
        Assert.assertEquals(0f, v.y, 0f);
    }

    @Test
    public void testParameterizedConstructor() {
        Vector2f v = new Vector2f(3f, -5f);
        Assert.assertEquals(3f, v.x, 0f);
        Assert.assertEquals(-5f, v.y, 0f);
    }

    @Test
    public void testCopyConstructor() {
        Vector2f original = new Vector2f(2f, 7f);
        Vector2f copy = new Vector2f(original);
        Assert.assertEquals(original.x, copy.x, 0f);
        Assert.assertEquals(original.y, copy.y, 0f);
        // Ensure it is a distinct object
        Assert.assertNotSame(original, copy);
    }

    @Test
    public void testSet() {
        Vector2f v = new Vector2f();
        Vector2f result = v.set(4f, -2f);
        Assert.assertSame(v, result);
        Assert.assertEquals(4f, v.x, 0f);
        Assert.assertEquals(-2f, v.y, 0f);
    }

    @Test
    public void testSetVector() {
        Vector2f v = new Vector2f();
        Vector2f other = new Vector2f(9f, 1f);
        Vector2f result = v.set(other);
        Assert.assertSame(v, result);
        Assert.assertEquals(9f, v.x, 0f);
        Assert.assertEquals(1f, v.y, 0f);
    }

    // -----------------------------------------------------------------------
    // Add
    // -----------------------------------------------------------------------

    @Test
    public void testAdd() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f b = new Vector2f(3f, 4f);
        Vector2f result = a.add(b);
        Assert.assertNotSame(a, result);
        Assert.assertEquals(4f, result.x, 0f);
        Assert.assertEquals(6f, result.y, 0f);
        // original unaffected
        Assert.assertEquals(1f, a.x, 0f);
    }

    @Test
    public void testAddReturnsNullForNullArg() {
        Vector2f a = new Vector2f(1f, 2f);
        Assert.assertNull(a.add((Vector2f) null));
    }

    @Test
    public void testAddLocal() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f b = new Vector2f(3f, 4f);
        Vector2f result = a.addLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(4f, a.x, 0f);
        Assert.assertEquals(6f, a.y, 0f);
    }

    @Test
    public void testAddLocalScalars() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f result = a.addLocal(3f, 4f);
        Assert.assertSame(a, result);
        Assert.assertEquals(4f, a.x, 0f);
        Assert.assertEquals(6f, a.y, 0f);
    }

    @Test
    public void testAddWithStore() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f b = new Vector2f(3f, 4f);
        Vector2f store = new Vector2f();
        Vector2f result = a.add(b, store);
        Assert.assertSame(store, result);
        Assert.assertEquals(4f, store.x, 0f);
        Assert.assertEquals(6f, store.y, 0f);
    }

    @Test
    public void testAddWithNullStore() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f b = new Vector2f(3f, 4f);
        Vector2f result = a.add(b, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(4f, result.x, 0f);
        Assert.assertEquals(6f, result.y, 0f);
    }

    // -----------------------------------------------------------------------
    // Subtract
    // -----------------------------------------------------------------------

    @Test
    public void testSubtract() {
        Vector2f a = new Vector2f(5f, 8f);
        Vector2f b = new Vector2f(3f, 2f);
        Vector2f result = a.subtract(b);
        Assert.assertEquals(2f, result.x, 0f);
        Assert.assertEquals(6f, result.y, 0f);
        Assert.assertEquals(5f, a.x, 0f); // unaffected
    }

    @Test
    public void testSubtractScalars() {
        Vector2f a = new Vector2f(5f, 8f);
        Vector2f result = a.subtract(1f, 3f);
        Assert.assertEquals(4f, result.x, 0f);
        Assert.assertEquals(5f, result.y, 0f);
    }

    @Test
    public void testSubtractLocal() {
        Vector2f a = new Vector2f(5f, 8f);
        Vector2f b = new Vector2f(3f, 2f);
        Vector2f result = a.subtractLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(2f, a.x, 0f);
        Assert.assertEquals(6f, a.y, 0f);
    }

    @Test
    public void testSubtractLocalScalars() {
        Vector2f a = new Vector2f(5f, 8f);
        Vector2f result = a.subtractLocal(1f, 3f);
        Assert.assertSame(a, result);
        Assert.assertEquals(4f, a.x, 0f);
        Assert.assertEquals(5f, a.y, 0f);
    }

    // -----------------------------------------------------------------------
    // Multiply
    // -----------------------------------------------------------------------

    @Test
    public void testMultScalar() {
        Vector2f a = new Vector2f(2f, 3f);
        Vector2f result = a.mult(4f);
        Assert.assertEquals(8f, result.x, 0f);
        Assert.assertEquals(12f, result.y, 0f);
        Assert.assertEquals(2f, a.x, 0f); // unaffected
    }

    @Test
    public void testMultScalarWithStore() {
        Vector2f a = new Vector2f(2f, 3f);
        Vector2f store = new Vector2f();
        Vector2f result = a.mult(4f, store);
        Assert.assertSame(store, result);
        Assert.assertEquals(8f, store.x, 0f);
        Assert.assertEquals(12f, store.y, 0f);
    }

    @Test
    public void testMultLocalScalar() {
        Vector2f a = new Vector2f(2f, 3f);
        Vector2f result = a.multLocal(4f);
        Assert.assertSame(a, result);
        Assert.assertEquals(8f, a.x, 0f);
        Assert.assertEquals(12f, a.y, 0f);
    }

    @Test
    public void testMultLocalVector() {
        Vector2f a = new Vector2f(2f, 3f);
        Vector2f b = new Vector2f(4f, 5f);
        Vector2f result = a.multLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(8f, a.x, 0f);
        Assert.assertEquals(15f, a.y, 0f);
    }

    @Test
    public void testMultComponents() {
        Vector2f a = new Vector2f(2f, 3f);
        Vector2f result = a.mult(4f, 5f);
        Assert.assertEquals(8f, result.x, 0f);
        Assert.assertEquals(15f, result.y, 0f);
    }

    // -----------------------------------------------------------------------
    // Divide
    // -----------------------------------------------------------------------

    @Test
    public void testDivideScalar() {
        Vector2f a = new Vector2f(8f, 12f);
        Vector2f result = a.divide(4f);
        Assert.assertEquals(2f, result.x, TOLERANCE);
        Assert.assertEquals(3f, result.y, TOLERANCE);
        Assert.assertEquals(8f, a.x, 0f); // unaffected
    }

    @Test
    public void testDivideLocalScalar() {
        Vector2f a = new Vector2f(8f, 12f);
        Vector2f result = a.divideLocal(4f);
        Assert.assertSame(a, result);
        Assert.assertEquals(2f, a.x, TOLERANCE);
        Assert.assertEquals(3f, a.y, TOLERANCE);
    }

    @Test
    public void testDivideComponents() {
        Vector2f a = new Vector2f(8f, 12f);
        Vector2f result = a.divide(4f, 3f);
        Assert.assertEquals(2f, result.x, TOLERANCE);
        Assert.assertEquals(4f, result.y, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Negate
    // -----------------------------------------------------------------------

    @Test
    public void testNegate() {
        Vector2f a = new Vector2f(3f, -5f);
        Vector2f result = a.negate();
        Assert.assertNotSame(a, result);
        Assert.assertEquals(-3f, result.x, 0f);
        Assert.assertEquals(5f, result.y, 0f);
        Assert.assertEquals(3f, a.x, 0f); // unaffected
    }

    @Test
    public void testNegateLocal() {
        Vector2f a = new Vector2f(3f, -5f);
        Vector2f result = a.negateLocal();
        Assert.assertSame(a, result);
        Assert.assertEquals(-3f, a.x, 0f);
        Assert.assertEquals(5f, a.y, 0f);
    }

    // -----------------------------------------------------------------------
    // Dot product
    // -----------------------------------------------------------------------

    @Test
    public void testDot() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f b = new Vector2f(3f, 4f);
        float dot = a.dot(b);
        Assert.assertEquals(11f, dot, TOLERANCE);
    }

    @Test
    public void testDotReturnsZeroForNullArg() {
        Vector2f a = new Vector2f(1f, 2f);
        Assert.assertEquals(0f, a.dot(null), 0f);
    }

    @Test
    public void testDotOrthogonal() {
        Vector2f a = new Vector2f(1f, 0f);
        Vector2f b = new Vector2f(0f, 1f);
        Assert.assertEquals(0f, a.dot(b), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Cross product (determinant)
    // -----------------------------------------------------------------------

    @Test
    public void testDeterminant() {
        Vector2f a = new Vector2f(1f, 0f);
        Vector2f b = new Vector2f(0f, 1f);
        Assert.assertEquals(1f, a.determinant(b), TOLERANCE);
    }

    @Test
    public void testCross() {
        Vector2f a = new Vector2f(1f, 0f);
        Vector2f b = new Vector2f(0f, 1f);
        Vector3f result = a.cross(b);
        Assert.assertEquals(0f, result.x, TOLERANCE);
        Assert.assertEquals(0f, result.y, TOLERANCE);
        Assert.assertEquals(1f, result.z, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Length / distance
    // -----------------------------------------------------------------------

    @Test
    public void testLength() {
        Vector2f a = new Vector2f(3f, 4f);
        Assert.assertEquals(5f, a.length(), TOLERANCE);
    }

    @Test
    public void testLengthSquared() {
        Vector2f a = new Vector2f(3f, 4f);
        Assert.assertEquals(25f, a.lengthSquared(), TOLERANCE);
    }

    @Test
    public void testDistance() {
        Vector2f a = new Vector2f(0f, 0f);
        Vector2f b = new Vector2f(3f, 4f);
        Assert.assertEquals(5f, a.distance(b), TOLERANCE);
    }

    @Test
    public void testDistanceSquared() {
        Vector2f a = new Vector2f(0f, 0f);
        Vector2f b = new Vector2f(3f, 4f);
        Assert.assertEquals(25f, a.distanceSquared(b), TOLERANCE);
    }

    @Test
    public void testDistanceSquaredComponents() {
        Vector2f a = new Vector2f(0f, 0f);
        Assert.assertEquals(25f, a.distanceSquared(3f, 4f), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Normalize
    // -----------------------------------------------------------------------

    @Test
    public void testNormalize() {
        Vector2f a = new Vector2f(3f, 4f);
        Vector2f result = a.normalize();
        Assert.assertNotSame(a, result);
        Assert.assertEquals(1f, result.length(), TOLERANCE);
        Assert.assertEquals(3f / 5f, result.x, TOLERANCE);
        Assert.assertEquals(4f / 5f, result.y, TOLERANCE);
        // original unaffected
        Assert.assertEquals(3f, a.x, 0f);
    }

    @Test
    public void testNormalizeLocal() {
        Vector2f a = new Vector2f(3f, 4f);
        Vector2f result = a.normalizeLocal();
        Assert.assertSame(a, result);
        Assert.assertEquals(1f, a.length(), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Interpolation
    // -----------------------------------------------------------------------

    @Test
    public void testInterpolateLocalFinalVec() {
        Vector2f a = new Vector2f(0f, 0f);
        Vector2f b = new Vector2f(10f, 20f);
        Vector2f result = a.interpolateLocal(b, 0.5f);
        Assert.assertSame(a, result);
        Assert.assertEquals(5f, a.x, TOLERANCE);
        Assert.assertEquals(10f, a.y, TOLERANCE);
    }

    @Test
    public void testInterpolateLocalBeginEnd() {
        Vector2f v = new Vector2f();
        Vector2f begin = new Vector2f(0f, 0f);
        Vector2f end = new Vector2f(10f, 20f);
        Vector2f result = v.interpolateLocal(begin, end, 0.25f);
        Assert.assertSame(v, result);
        Assert.assertEquals(2.5f, v.x, TOLERANCE);
        Assert.assertEquals(5f, v.y, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Validation
    // -----------------------------------------------------------------------

    @Test
    public void testIsValidVector() {
        Assert.assertTrue(Vector2f.isValidVector(new Vector2f(1f, 2f)));
        Assert.assertFalse(Vector2f.isValidVector(null));
        Assert.assertFalse(Vector2f.isValidVector(new Vector2f(Float.NaN, 0f)));
        Assert.assertFalse(Vector2f.isValidVector(new Vector2f(0f, Float.NaN)));
        Assert.assertFalse(Vector2f.isValidVector(new Vector2f(Float.POSITIVE_INFINITY, 0f)));
        Assert.assertFalse(Vector2f.isValidVector(new Vector2f(0f, Float.NEGATIVE_INFINITY)));
    }

    // -----------------------------------------------------------------------
    // Zero / getters / setters
    // -----------------------------------------------------------------------

    @Test
    public void testZero() {
        Vector2f v = new Vector2f(3f, 4f);
        Vector2f result = v.zero();
        Assert.assertSame(v, result);
        Assert.assertEquals(0f, v.x, 0f);
        Assert.assertEquals(0f, v.y, 0f);
    }

    @Test
    public void testGettersAndSetters() {
        Vector2f v = new Vector2f();
        v.setX(5f);
        v.setY(7f);
        Assert.assertEquals(5f, v.getX(), 0f);
        Assert.assertEquals(7f, v.getY(), 0f);
    }

    @Test
    public void testGetAngle() {
        Vector2f v = new Vector2f(1f, 0f);
        Assert.assertEquals(0f, v.getAngle(), TOLERANCE);

        Vector2f v2 = new Vector2f(0f, 1f);
        Assert.assertEquals(FastMath.HALF_PI, v2.getAngle(), TOLERANCE);
    }

    @Test
    public void testSmallestAngleBetween() {
        Vector2f a = new Vector2f(1f, 0f);
        Vector2f b = new Vector2f(0f, 1f);
        Assert.assertEquals(FastMath.HALF_PI, a.smallestAngleBetween(b), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Clone and equals
    // -----------------------------------------------------------------------

    @Test
    public void testClone() {
        Vector2f original = new Vector2f(3f, -4f);
        Vector2f cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.x, cloned.x, 0f);
        Assert.assertEquals(original.y, cloned.y, 0f);
    }

    @Test
    public void testEqualsAndHashCode() {
        Vector2f a = new Vector2f(1f, 2f);
        Vector2f b = new Vector2f(1f, 2f);
        Vector2f c = new Vector2f(1f, 3f);
        Assert.assertEquals(a, b);
        Assert.assertNotEquals(a, c);
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToArray() {
        Vector2f v = new Vector2f(3f, 4f);
        float[] arr = v.toArray(null);
        Assert.assertNotNull(arr);
        Assert.assertEquals(2, arr.length);
        Assert.assertEquals(3f, arr[0], 0f);
        Assert.assertEquals(4f, arr[1], 0f);

        float[] provided = new float[2];
        float[] result = v.toArray(provided);
        Assert.assertSame(provided, result);
        Assert.assertEquals(3f, provided[0], 0f);
        Assert.assertEquals(4f, provided[1], 0f);
    }

    // -----------------------------------------------------------------------
    // Static constants
    // -----------------------------------------------------------------------

    @Test
    public void testStaticConstants() {
        Assert.assertEquals(0f, Vector2f.ZERO.x, 0f);
        Assert.assertEquals(0f, Vector2f.ZERO.y, 0f);
        Assert.assertEquals(1f, Vector2f.UNIT_X.x, 0f);
        Assert.assertEquals(0f, Vector2f.UNIT_X.y, 0f);
        Assert.assertEquals(0f, Vector2f.UNIT_Y.x, 0f);
        Assert.assertEquals(1f, Vector2f.UNIT_Y.y, 0f);
        Assert.assertTrue(Float.isNaN(Vector2f.NAN.x));
        Assert.assertTrue(Float.isNaN(Vector2f.NAN.y));
    }
}
