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
 * Verifies that the {@link Vector4f} class works correctly.
 */
public class Vector4fTest {

    private static final float TOLERANCE = 1e-6f;

    // -----------------------------------------------------------------------
    // Constructors and set
    // -----------------------------------------------------------------------

    @Test
    public void testDefaultConstructor() {
        Vector4f v = new Vector4f();
        Assert.assertEquals(0f, v.x, 0f);
        Assert.assertEquals(0f, v.y, 0f);
        Assert.assertEquals(0f, v.z, 0f);
        Assert.assertEquals(0f, v.w, 0f);
    }

    @Test
    public void testParameterizedConstructor() {
        Vector4f v = new Vector4f(1f, 2f, 3f, 4f);
        Assert.assertEquals(1f, v.x, 0f);
        Assert.assertEquals(2f, v.y, 0f);
        Assert.assertEquals(3f, v.z, 0f);
        Assert.assertEquals(4f, v.w, 0f);
    }

    @Test
    public void testCopyConstructor() {
        Vector4f original = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f copy = new Vector4f(original);
        Assert.assertEquals(original.x, copy.x, 0f);
        Assert.assertEquals(original.y, copy.y, 0f);
        Assert.assertEquals(original.z, copy.z, 0f);
        Assert.assertEquals(original.w, copy.w, 0f);
        Assert.assertNotSame(original, copy);
    }

    @Test
    public void testSet() {
        Vector4f v = new Vector4f();
        Vector4f result = v.set(1f, 2f, 3f, 4f);
        Assert.assertSame(v, result);
        Assert.assertEquals(1f, v.x, 0f);
        Assert.assertEquals(2f, v.y, 0f);
        Assert.assertEquals(3f, v.z, 0f);
        Assert.assertEquals(4f, v.w, 0f);
    }

    @Test
    public void testSetVector() {
        Vector4f v = new Vector4f();
        Vector4f other = new Vector4f(5f, 6f, 7f, 8f);
        Vector4f result = v.set(other);
        Assert.assertSame(v, result);
        Assert.assertEquals(5f, v.x, 0f);
        Assert.assertEquals(6f, v.y, 0f);
        Assert.assertEquals(7f, v.z, 0f);
        Assert.assertEquals(8f, v.w, 0f);
    }

    // -----------------------------------------------------------------------
    // Add
    // -----------------------------------------------------------------------

    @Test
    public void testAdd() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(5f, 6f, 7f, 8f);
        Vector4f result = a.add(b);
        Assert.assertNotSame(a, result);
        Assert.assertEquals(6f, result.x, 0f);
        Assert.assertEquals(8f, result.y, 0f);
        Assert.assertEquals(10f, result.z, 0f);
        Assert.assertEquals(12f, result.w, 0f);
        Assert.assertEquals(1f, a.x, 0f); // unaffected
    }

    @Test
    public void testAddReturnsNullForNullArg() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Assert.assertNull(a.add((Vector4f) null));
    }

    @Test
    public void testAddWithStore() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(5f, 6f, 7f, 8f);
        Vector4f store = new Vector4f();
        Vector4f result = a.add(b, store);
        Assert.assertSame(store, result);
        Assert.assertEquals(6f, store.x, 0f);
        Assert.assertEquals(8f, store.y, 0f);
        Assert.assertEquals(10f, store.z, 0f);
        Assert.assertEquals(12f, store.w, 0f);
    }

    @Test
    public void testAddScalars() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = a.add(5f, 6f, 7f, 8f);
        Assert.assertEquals(6f, result.x, 0f);
        Assert.assertEquals(8f, result.y, 0f);
        Assert.assertEquals(10f, result.z, 0f);
        Assert.assertEquals(12f, result.w, 0f);
    }

    @Test
    public void testAddLocal() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(5f, 6f, 7f, 8f);
        Vector4f result = a.addLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(6f, a.x, 0f);
        Assert.assertEquals(8f, a.y, 0f);
        Assert.assertEquals(10f, a.z, 0f);
        Assert.assertEquals(12f, a.w, 0f);
    }

    @Test
    public void testAddLocalScalars() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = a.addLocal(5f, 6f, 7f, 8f);
        Assert.assertSame(a, result);
        Assert.assertEquals(6f, a.x, 0f);
        Assert.assertEquals(8f, a.y, 0f);
        Assert.assertEquals(10f, a.z, 0f);
        Assert.assertEquals(12f, a.w, 0f);
    }

    // -----------------------------------------------------------------------
    // Subtract
    // -----------------------------------------------------------------------

    @Test
    public void testSubtract() {
        Vector4f a = new Vector4f(5f, 8f, 11f, 14f);
        Vector4f b = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = a.subtract(b);
        Assert.assertNotSame(a, result);
        Assert.assertEquals(4f, result.x, 0f);
        Assert.assertEquals(6f, result.y, 0f);
        Assert.assertEquals(8f, result.z, 0f);
        Assert.assertEquals(10f, result.w, 0f);
        Assert.assertEquals(5f, a.x, 0f); // unaffected
    }

    @Test
    public void testSubtractLocal() {
        Vector4f a = new Vector4f(5f, 8f, 11f, 14f);
        Vector4f b = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = a.subtractLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(4f, a.x, 0f);
        Assert.assertEquals(6f, a.y, 0f);
        Assert.assertEquals(8f, a.z, 0f);
        Assert.assertEquals(10f, a.w, 0f);
    }

    @Test
    public void testSubtractScalars() {
        Vector4f a = new Vector4f(5f, 8f, 11f, 14f);
        Vector4f result = a.subtract(1f, 2f, 3f, 4f);
        Assert.assertEquals(4f, result.x, 0f);
        Assert.assertEquals(6f, result.y, 0f);
        Assert.assertEquals(8f, result.z, 0f);
        Assert.assertEquals(10f, result.w, 0f);
    }

    // -----------------------------------------------------------------------
    // Multiply
    // -----------------------------------------------------------------------

    @Test
    public void testMultScalar() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = a.mult(2f);
        Assert.assertNotSame(a, result);
        Assert.assertEquals(2f, result.x, 0f);
        Assert.assertEquals(4f, result.y, 0f);
        Assert.assertEquals(6f, result.z, 0f);
        Assert.assertEquals(8f, result.w, 0f);
        Assert.assertEquals(1f, a.x, 0f); // unaffected
    }

    @Test
    public void testMultScalarWithStore() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f store = new Vector4f();
        Vector4f result = a.mult(2f, store);
        Assert.assertSame(store, result);
        Assert.assertEquals(2f, store.x, 0f);
        Assert.assertEquals(4f, store.y, 0f);
        Assert.assertEquals(6f, store.z, 0f);
        Assert.assertEquals(8f, store.w, 0f);
    }

    @Test
    public void testMultLocalScalar() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = a.multLocal(2f);
        Assert.assertSame(a, result);
        Assert.assertEquals(2f, a.x, 0f);
        Assert.assertEquals(4f, a.y, 0f);
        Assert.assertEquals(6f, a.z, 0f);
        Assert.assertEquals(8f, a.w, 0f);
    }

    @Test
    public void testMultLocalVector() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(2f, 3f, 4f, 5f);
        Vector4f result = a.multLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(2f, a.x, 0f);
        Assert.assertEquals(6f, a.y, 0f);
        Assert.assertEquals(12f, a.z, 0f);
        Assert.assertEquals(20f, a.w, 0f);
    }

    @Test
    public void testMultVector() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(2f, 3f, 4f, 5f);
        Vector4f result = a.mult(b);
        Assert.assertNotSame(a, result);
        Assert.assertEquals(2f, result.x, 0f);
        Assert.assertEquals(6f, result.y, 0f);
        Assert.assertEquals(12f, result.z, 0f);
        Assert.assertEquals(20f, result.w, 0f);
    }

    // -----------------------------------------------------------------------
    // Divide
    // -----------------------------------------------------------------------

    @Test
    public void testDivideScalar() {
        Vector4f a = new Vector4f(2f, 4f, 6f, 8f);
        Vector4f result = a.divide(2f);
        Assert.assertNotSame(a, result);
        Assert.assertEquals(1f, result.x, TOLERANCE);
        Assert.assertEquals(2f, result.y, TOLERANCE);
        Assert.assertEquals(3f, result.z, TOLERANCE);
        Assert.assertEquals(4f, result.w, TOLERANCE);
        Assert.assertEquals(2f, a.x, 0f); // unaffected
    }

    @Test
    public void testDivideLocalScalar() {
        Vector4f a = new Vector4f(2f, 4f, 6f, 8f);
        Vector4f result = a.divideLocal(2f);
        Assert.assertSame(a, result);
        Assert.assertEquals(1f, a.x, TOLERANCE);
        Assert.assertEquals(2f, a.y, TOLERANCE);
        Assert.assertEquals(3f, a.z, TOLERANCE);
        Assert.assertEquals(4f, a.w, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Negate
    // -----------------------------------------------------------------------

    @Test
    public void testNegate() {
        Vector4f a = new Vector4f(1f, -2f, 3f, -4f);
        Vector4f result = a.negate();
        Assert.assertNotSame(a, result);
        Assert.assertEquals(-1f, result.x, 0f);
        Assert.assertEquals(2f, result.y, 0f);
        Assert.assertEquals(-3f, result.z, 0f);
        Assert.assertEquals(4f, result.w, 0f);
        Assert.assertEquals(1f, a.x, 0f); // unaffected
    }

    @Test
    public void testNegateLocal() {
        Vector4f a = new Vector4f(1f, -2f, 3f, -4f);
        Vector4f result = a.negateLocal();
        Assert.assertSame(a, result);
        Assert.assertEquals(-1f, a.x, 0f);
        Assert.assertEquals(2f, a.y, 0f);
        Assert.assertEquals(-3f, a.z, 0f);
        Assert.assertEquals(4f, a.w, 0f);
    }

    // -----------------------------------------------------------------------
    // Dot product
    // -----------------------------------------------------------------------

    @Test
    public void testDot() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(5f, 6f, 7f, 8f);
        // 1*5 + 2*6 + 3*7 + 4*8 = 5 + 12 + 21 + 32 = 70
        Assert.assertEquals(70f, a.dot(b), TOLERANCE);
    }

    @Test
    public void testDotReturnsZeroForNullArg() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Assert.assertEquals(0f, a.dot(null), 0f);
    }

    // -----------------------------------------------------------------------
    // Length / distance
    // -----------------------------------------------------------------------

    @Test
    public void testLength() {
        // 1^2 + 0 + 0 + 0 = 1
        Vector4f a = new Vector4f(1f, 0f, 0f, 0f);
        Assert.assertEquals(1f, a.length(), TOLERANCE);

        // 1^2 + 2^2 + 2^2 + 0 = 9, sqrt = 3
        Vector4f b = new Vector4f(1f, 2f, 2f, 0f);
        Assert.assertEquals(3f, b.length(), TOLERANCE);
    }

    @Test
    public void testLengthSquared() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        // 1 + 4 + 9 + 16 = 30
        Assert.assertEquals(30f, a.lengthSquared(), TOLERANCE);
    }

    @Test
    public void testDistance() {
        Vector4f a = new Vector4f(0f, 0f, 0f, 0f);
        Vector4f b = new Vector4f(1f, 0f, 0f, 0f);
        Assert.assertEquals(1f, a.distance(b), TOLERANCE);
    }

    @Test
    public void testDistanceSquared() {
        Vector4f a = new Vector4f(0f, 0f, 0f, 0f);
        Vector4f b = new Vector4f(2f, 2f, 1f, 0f);
        // 4 + 4 + 1 + 0 = 9
        Assert.assertEquals(9f, a.distanceSquared(b), TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Normalize
    // -----------------------------------------------------------------------

    @Test
    public void testNormalize() {
        Vector4f a = new Vector4f(0f, 3f, 0f, 4f);
        Vector4f result = a.normalize();
        Assert.assertNotSame(a, result);
        Assert.assertEquals(1f, result.length(), TOLERANCE);
        // original unaffected
        Assert.assertEquals(3f, a.y, 0f);
    }

    @Test
    public void testNormalizeLocal() {
        Vector4f a = new Vector4f(0f, 3f, 0f, 4f);
        Vector4f result = a.normalizeLocal();
        Assert.assertSame(a, result);
        Assert.assertEquals(1f, a.length(), TOLERANCE);
    }

    @Test
    public void testIsUnitVector() {
        Vector4f a = new Vector4f(1f, 0f, 0f, 0f);
        Assert.assertTrue(a.isUnitVector());

        Vector4f b = new Vector4f(2f, 0f, 0f, 0f);
        Assert.assertFalse(b.isUnitVector());
    }

    // -----------------------------------------------------------------------
    // scaleAdd
    // -----------------------------------------------------------------------

    @Test
    public void testScaleAdd() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f add = new Vector4f(1f, 1f, 1f, 1f);
        Vector4f result = a.scaleAdd(2f, add);
        Assert.assertSame(a, result);
        Assert.assertEquals(3f, a.x, TOLERANCE); // 1*2 + 1
        Assert.assertEquals(5f, a.y, TOLERANCE); // 2*2 + 1
        Assert.assertEquals(7f, a.z, TOLERANCE); // 3*2 + 1
        Assert.assertEquals(9f, a.w, TOLERANCE); // 4*2 + 1
    }

    @Test
    public void testScaleAddWithMult() {
        Vector4f v = new Vector4f();
        Vector4f mult = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f add = new Vector4f(10f, 10f, 10f, 10f);
        Vector4f result = v.scaleAdd(3f, mult, add);
        Assert.assertSame(v, result);
        Assert.assertEquals(13f, v.x, TOLERANCE); // 1*3 + 10
        Assert.assertEquals(16f, v.y, TOLERANCE); // 2*3 + 10
        Assert.assertEquals(19f, v.z, TOLERANCE); // 3*3 + 10
        Assert.assertEquals(22f, v.w, TOLERANCE); // 4*3 + 10
    }

    // -----------------------------------------------------------------------
    // project
    // -----------------------------------------------------------------------

    @Test
    public void testProject() {
        // projecting (2,0,0,0) onto (1,0,0,0) should return (2,0,0,0)
        Vector4f a = new Vector4f(2f, 0f, 0f, 0f);
        Vector4f b = new Vector4f(1f, 0f, 0f, 0f);
        Vector4f proj = a.project(b);
        Assert.assertEquals(2f, proj.x, TOLERANCE);
        Assert.assertEquals(0f, proj.y, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // min/max
    // -----------------------------------------------------------------------

    @Test
    public void testMinLocal() {
        Vector4f a = new Vector4f(5f, 2f, 8f, 1f);
        Vector4f b = new Vector4f(3f, 6f, 4f, 7f);
        Vector4f result = a.minLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(3f, a.x, 0f);
        Assert.assertEquals(2f, a.y, 0f);
        Assert.assertEquals(4f, a.z, 0f);
        Assert.assertEquals(1f, a.w, 0f);
    }

    @Test
    public void testMaxLocal() {
        Vector4f a = new Vector4f(5f, 2f, 8f, 1f);
        Vector4f b = new Vector4f(3f, 6f, 4f, 7f);
        Vector4f result = a.maxLocal(b);
        Assert.assertSame(a, result);
        Assert.assertEquals(5f, a.x, 0f);
        Assert.assertEquals(6f, a.y, 0f);
        Assert.assertEquals(8f, a.z, 0f);
        Assert.assertEquals(7f, a.w, 0f);
    }

    // -----------------------------------------------------------------------
    // Zero / clone / equals
    // -----------------------------------------------------------------------

    @Test
    public void testZero() {
        Vector4f v = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f result = v.zero();
        Assert.assertSame(v, result);
        Assert.assertEquals(0f, v.x, 0f);
        Assert.assertEquals(0f, v.y, 0f);
        Assert.assertEquals(0f, v.z, 0f);
        Assert.assertEquals(0f, v.w, 0f);
    }

    @Test
    public void testClone() {
        Vector4f original = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.x, cloned.x, 0f);
        Assert.assertEquals(original.y, cloned.y, 0f);
        Assert.assertEquals(original.z, cloned.z, 0f);
        Assert.assertEquals(original.w, cloned.w, 0f);
    }

    @Test
    public void testEqualsAndHashCode() {
        Vector4f a = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f b = new Vector4f(1f, 2f, 3f, 4f);
        Vector4f c = new Vector4f(1f, 2f, 3f, 5f);
        Assert.assertEquals(a, b);
        Assert.assertNotEquals(a, c);
        Assert.assertEquals(a.hashCode(), b.hashCode());
    }

    // -----------------------------------------------------------------------
    // Static constants
    // -----------------------------------------------------------------------

    @Test
    public void testStaticConstants() {
        Assert.assertEquals(0f, Vector4f.ZERO.x, 0f);
        Assert.assertEquals(0f, Vector4f.ZERO.w, 0f);
        Assert.assertEquals(1f, Vector4f.UNIT_X.x, 0f);
        Assert.assertEquals(0f, Vector4f.UNIT_X.y, 0f);
        Assert.assertEquals(1f, Vector4f.UNIT_W.w, 0f);
        Assert.assertEquals(0f, Vector4f.UNIT_W.x, 0f);
        Assert.assertTrue(Float.isNaN(Vector4f.NAN.x));
    }
}
