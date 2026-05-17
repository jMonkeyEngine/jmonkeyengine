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



import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class Vector4fTest {

    @Test
    public void testAdd() {
        final Vector4f target = new Vector4f(1.0f, Float.NaN, 5.36f, 2.0f);
        final Vector4f vec = new Vector4f(-1.0f, 2.93f, -5.36f, -2.0f);

        final Vector4f retval = target.add(vec);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testAdd_null() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.add(null);

        assertNull(retval);
    }

    @Test
    public void testAdd_withResult() {
        final Vector4f target = new Vector4f(0.0f, -7.52f, 3.1f, 1.0f);
        final Vector4f other = new Vector4f(1.42f, 7.52f, 1.1f, -1.0f);
        final Vector4f result = new Vector4f();

        final Vector4f retval = target.add(other, result);

        assertNotNull(retval);
        assertEquals(retval, result);
        assertEquals(1.42f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(4.2f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testAdd_floats() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.add(1.0f, 2.0f, 3.0f, 4.0f);

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(6.0f, retval.z, 0.0f);
        assertEquals(8.0f, retval.w, 0.0f);
    }

    @Test
    public void testAddLocal_null() {
        final Vector4f target = new Vector4f(0.0f, -7.52f, 3.1f, 1.5f);

        final Vector4f retval = target.addLocal(null);

        assertNull(retval);
    }

    @Test
    public void testAddLocal() {
        final Vector4f target = new Vector4f(0.0f, -7.52f, 3.1f, 1.5f);

        final Vector4f retval = target.addLocal(new Vector4f(2.0f, 6.2f, 8.3f, -1.5f));

        assertNotNull(retval);
        assertEquals(retval, target);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(-1.32f, retval.y, 0.01f);
        assertEquals(11.4f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testAddLocal_floats() {
        final Vector4f target = new Vector4f(0.0f, -7.52f, 3.1f, 1.5f);

        final Vector4f retval = target.addLocal(2.0f, 6.2f, 8.3f, -1.5f);

        assertNotNull(retval);
        assertEquals(retval, target);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(-1.32f, retval.y, 0.01f);
        assertEquals(11.4f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testDot() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        assertEquals(30.0f, target.dot(new Vector4f(1.0f, 2.0f, 3.0f, 4.0f)), 0.0f);
    }

    @Test
    public void testDot_null() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        assertEquals(0.0f, target.dot(null), 0.0f);
    }

    @Test
    public void testDot_perpendicular() {
        final Vector4f target = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);

        assertEquals(0.0f, target.dot(new Vector4f(0.0f, 1.0f, 0.0f, 0.0f)), 0.0f);
    }

    @Test
    public void testSubtract() {
        final Vector4f retval = new Vector4f(12.0f, 8.0f, 5.0f, 3.0f)
                .subtract(new Vector4f(7.0f, 4.0f, -2.0f, 1.0f));

        assertNotNull(retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(2.0f, retval.w, 0.0f);
    }

    @Test
    public void testSubtract_withResult() {
        final Vector4f target = new Vector4f(12.0f, 8.0f, 5.0f, 3.0f);
        final Vector4f result = new Vector4f();

        final Vector4f retval = target.subtract(new Vector4f(7.0f, 4.0f, -2.0f, 1.0f), result);

        assertEquals(result, retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(2.0f, retval.w, 0.0f);
    }

    @Test
    public void testSubtract_floats() {
        final Vector4f target = new Vector4f(12.0f, 8.0f, 5.0f, 3.0f);

        final Vector4f retval = target.subtract(7.0f, 4.0f, -2.0f, 1.0f);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(2.0f, retval.w, 0.0f);
    }

    @Test
    public void testSubtractLocal() {
        final Vector4f target = new Vector4f(12.0f, 8.0f, 5.0f, 3.0f);

        final Vector4f retval = target.subtractLocal(new Vector4f(7.0f, 4.0f, -2.0f, 1.0f));

        assertEquals(target, retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(2.0f, retval.w, 0.0f);
    }

    @Test
    public void testSubtractLocal_null() {
        final Vector4f target = new Vector4f(12.0f, 8.0f, 5.0f, 3.0f);

        final Vector4f retval = target.subtractLocal(null);

        assertNull(retval);
    }

    @Test
    public void testSubtractLocal_floats() {
        final Vector4f target = new Vector4f(12.0f, 8.0f, 5.0f, 3.0f);

        final Vector4f retval = target.subtractLocal(7.0f, 4.0f, -2.0f, 1.0f);

        assertEquals(target, retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(2.0f, retval.w, 0.0f);
    }

    @Test
    public void testMult_scalar() {
        final Vector4f retval = new Vector4f(3.0f, 6.0f, 9.0f, 12.0f).mult(2.0f);

        assertNotNull(retval);
        assertEquals(6.0f, retval.x, 0.0f);
        assertEquals(12.0f, retval.y, 0.0f);
        assertEquals(18.0f, retval.z, 0.0f);
        assertEquals(24.0f, retval.w, 0.0f);
    }

    @Test
    public void testMult_scalarWithProduct() {
        final Vector4f product = new Vector4f();
        final Vector4f retval = new Vector4f(3.0f, 6.0f, 9.0f, 12.0f).mult(2.0f, product);

        assertEquals(product, retval);
        assertEquals(6.0f, retval.x, 0.0f);
        assertEquals(12.0f, retval.y, 0.0f);
        assertEquals(18.0f, retval.z, 0.0f);
        assertEquals(24.0f, retval.w, 0.0f);
    }

    @Test
    public void testMult_scalarNullProduct() {
        final Vector4f retval = new Vector4f(3.0f, 6.0f, 9.0f, 12.0f).mult(2.0f, null);

        assertNotNull(retval);
        assertEquals(6.0f, retval.x, 0.0f);
        assertEquals(12.0f, retval.y, 0.0f);
        assertEquals(18.0f, retval.z, 0.0f);
        assertEquals(24.0f, retval.w, 0.0f);
    }

    @Test
    public void testMult_floats() {
        final Vector4f retval = new Vector4f(2.0f, 3.0f, 4.0f, 5.0f).mult(1.0f, 2.0f, 3.0f, 4.0f);

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(6.0f, retval.y, 0.0f);
        assertEquals(12.0f, retval.z, 0.0f);
        assertEquals(20.0f, retval.w, 0.0f);
    }

    @Test
    public void testMult_vector() {
        final Vector4f target = new Vector4f(2.0f, 3.0f, 4.0f, 5.0f);
        final Vector4f vec = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.mult(vec);

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(6.0f, retval.y, 0.0f);
        assertEquals(12.0f, retval.z, 0.0f);
        assertEquals(20.0f, retval.w, 0.0f);
    }

    @Test
    public void testMult_vectorNull() {
        final Vector4f target = new Vector4f(2.0f, 3.0f, 4.0f, 5.0f);

        assertNull(target.mult(null));
    }

    @Test
    public void testMult_vectorNullStore() {
        final Vector4f target = new Vector4f(2.0f, 3.0f, 4.0f, 5.0f);

        assertNull(target.mult(null, new Vector4f()));
    }

    @Test
    public void testMultLocal_scalar() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.multLocal(2.0f);

        assertEquals(target, retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(6.0f, retval.z, 0.0f);
        assertEquals(8.0f, retval.w, 0.0f);
    }

    @Test
    public void testMultLocal_vector() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.multLocal(new Vector4f(2.0f, 3.0f, 4.0f, 5.0f));

        assertEquals(target, retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(6.0f, retval.y, 0.0f);
        assertEquals(12.0f, retval.z, 0.0f);
        assertEquals(20.0f, retval.w, 0.0f);
    }

    @Test
    public void testMultLocal_vectorNull() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        assertNull(target.multLocal(null));
    }

    @Test
    public void testMultLocal_floats() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.multLocal(2.0f, 3.0f, 4.0f, 5.0f);

        assertEquals(target, retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(6.0f, retval.y, 0.0f);
        assertEquals(12.0f, retval.z, 0.0f);
        assertEquals(20.0f, retval.w, 0.0f);
    }

    @Test
    public void testDivide_byScalar() {
        final Vector4f target = new Vector4f(6.0f, 9.0f, 12.0f, 15.0f);

        final Vector4f retval = target.divide(3.0f);

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.001f);
        assertEquals(3.0f, retval.y, 0.001f);
        assertEquals(4.0f, retval.z, 0.001f);
        assertEquals(5.0f, retval.w, 0.001f);
    }

    @Test
    public void testDivide_byZero() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.divide(0.0f);

        assertNotNull(retval);
        assertEquals(Float.POSITIVE_INFINITY, retval.x, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.z, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.w, 0.0f);
    }

    @Test
    public void testDivide_byVector() {
        final Vector4f target = new Vector4f(6.0f, 9.0f, 12.0f, 15.0f);

        final Vector4f retval = target.divide(new Vector4f(3.0f, 3.0f, 4.0f, 5.0f));

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.001f);
        assertEquals(3.0f, retval.y, 0.001f);
        assertEquals(3.0f, retval.z, 0.001f);
        assertEquals(3.0f, retval.w, 0.001f);
    }

    @Test
    public void testDivide_byFloats() {
        final Vector4f target = new Vector4f(6.0f, 9.0f, 12.0f, 15.0f);

        final Vector4f retval = target.divide(3.0f, 3.0f, 4.0f, 5.0f);

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.001f);
        assertEquals(3.0f, retval.y, 0.001f);
        assertEquals(3.0f, retval.z, 0.001f);
        assertEquals(3.0f, retval.w, 0.001f);
    }

    @Test
    public void testDivideLocal_byScalar() {
        final Vector4f target = new Vector4f(6.0f, 9.0f, 12.0f, 15.0f);

        final Vector4f retval = target.divideLocal(3.0f);

        assertEquals(target, retval);
        assertEquals(2.0f, retval.x, 0.001f);
        assertEquals(3.0f, retval.y, 0.001f);
        assertEquals(4.0f, retval.z, 0.001f);
        assertEquals(5.0f, retval.w, 0.001f);
    }

    @Test
    public void testDivideLocal_byVector() {
        final Vector4f target = new Vector4f(6.0f, 9.0f, 12.0f, 15.0f);

        final Vector4f retval = target.divideLocal(new Vector4f(3.0f, 3.0f, 4.0f, 5.0f));

        assertEquals(target, retval);
        assertEquals(2.0f, retval.x, 0.001f);
        assertEquals(3.0f, retval.y, 0.001f);
        assertEquals(3.0f, retval.z, 0.001f);
        assertEquals(3.0f, retval.w, 0.001f);
    }

    @Test
    public void testDivideLocal_byFloats() {
        final Vector4f target = new Vector4f(6.0f, 9.0f, 12.0f, 15.0f);

        final Vector4f retval = target.divideLocal(3.0f, 3.0f, 4.0f, 5.0f);

        assertEquals(target, retval);
        assertEquals(2.0f, retval.x, 0.001f);
        assertEquals(3.0f, retval.y, 0.001f);
        assertEquals(3.0f, retval.z, 0.001f);
        assertEquals(3.0f, retval.w, 0.001f);
    }

    @Test
    public void testNegate() {
        final Vector4f target = new Vector4f(-1.0f, 2.0f, -3.0f, 4.0f);

        final Vector4f retval = target.negate();

        assertNotNull(retval);
        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(-2.0f, retval.y, 0.0f);
        assertEquals(3.0f, retval.z, 0.0f);
        assertEquals(-4.0f, retval.w, 0.0f);
    }

    @Test
    public void testNegate2() {
        final Vector4f retval = new Vector4f(Float.NaN, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, 0.0f).negate();

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.z, 0.0f);
        assertEquals(-0.0f, retval.w, 0.0f);
    }

    @Test
    public void testNegateLocal() {
        final Vector4f target = new Vector4f(-4.5f, 3.0f, Float.POSITIVE_INFINITY, -1.0f);

        final Vector4f retval = target.negateLocal();

        assertEquals(target, retval);
        assertEquals(4.5f, retval.x, 0.0f);
        assertEquals(-3.0f, retval.y, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.z, 0.0f);
        assertEquals(1.0f, retval.w, 0.0f);
    }

    @Test
    public void testLength() {
        assertEquals(Float.NaN, new Vector4f(Float.NaN, 0.0f, 0.0f, 0.0f).length(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY,
                new Vector4f(Float.POSITIVE_INFINITY, 0.0f, 0.0f, 0.0f).length(), 0.0f);
        assertEquals(2.0f, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f).length(), 0.001f);
        assertEquals(5.477f, new Vector4f(1.0f, 2.0f, 3.0f, 4.0f).length(), 0.001f);
    }

    @Test
    public void testLengthSquared() {
        assertEquals(0.0f, new Vector4f(0.0f, 0.0f, 0.0f, 0.0f).lengthSquared(), 0.0f);
        assertEquals(4.0f, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f).lengthSquared(), 0.0f);
        assertEquals(30.0f, new Vector4f(1.0f, 2.0f, 3.0f, 4.0f).lengthSquared(), 0.0f);
    }

    @Test
    public void testDistance() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f other = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        assertEquals(0.0f, target.distance(other), 0.0f);
    }

    @Test
    public void testDistance2() {
        final Vector4f target = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f other = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

        assertEquals(2.0f, target.distance(other), 0.001f);
    }

    @Test
    public void testDistanceSquared() {
        final Vector4f target = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f other = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

        assertEquals(4.0f, target.distanceSquared(other), 0.0f);
    }

    @Test
    public void testNormalize() {
        final Vector4f retval = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f).normalize();

        assertNotNull(retval);
        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testNormalize2() {
        final Vector4f retval = new Vector4f(2.0f, 0.0f, 0.0f, 0.0f).normalize();

        assertNotNull(retval);
        assertEquals(1.0f, retval.x, 0.001f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testNormalize_zero() {
        final Vector4f retval = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f).normalize();

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testNormalizeLocal() {
        final Vector4f target = new Vector4f(2.0f, 0.0f, 0.0f, 0.0f);

        final Vector4f retval = target.normalizeLocal();

        assertEquals(target, retval);
        assertEquals(1.0f, retval.x, 0.001f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testIsUnitVector() {
        assertTrue(new Vector4f(1.0f, 0.0f, 0.0f, 0.0f).isUnitVector());
        assertTrue(new Vector4f(0.0f, 1.0f, 0.0f, 0.0f).isUnitVector());
        assertTrue(new Vector4f(0.0f, 0.0f, 1.0f, 0.0f).isUnitVector());
        assertTrue(new Vector4f(0.0f, 0.0f, 0.0f, 1.0f).isUnitVector());
        assertFalse(new Vector4f(1.0f, 1.0f, 0.0f, 0.0f).isUnitVector());
        assertFalse(new Vector4f(0.0f, 0.0f, 0.0f, 0.0f).isUnitVector());
        assertFalse(new Vector4f(Float.NaN, 0.0f, 0.0f, 0.0f).isUnitVector());
    }

    @Test
    public void testIsValidVector() {
        assertFalse(Vector4f.isValidVector(null));
        assertFalse(Vector4f.isValidVector(new Vector4f(Float.NaN, 0.0f, 0.0f, 0.0f)));
        assertFalse(Vector4f.isValidVector(new Vector4f(0.0f, Float.NaN, 0.0f, 0.0f)));
        assertFalse(Vector4f.isValidVector(new Vector4f(0.0f, 0.0f, Float.NaN, 0.0f)));
        assertFalse(Vector4f.isValidVector(new Vector4f(0.0f, 0.0f, 0.0f, Float.NaN)));
        assertFalse(Vector4f.isValidVector(new Vector4f(Float.POSITIVE_INFINITY, 0.0f, 0.0f, 0.0f)));
        assertFalse(Vector4f.isValidVector(new Vector4f(0.0f, Float.NEGATIVE_INFINITY, 0.0f, 0.0f)));
        assertTrue(Vector4f.isValidVector(new Vector4f()));
        assertTrue(Vector4f.isValidVector(new Vector4f(1.0f, 2.0f, 3.0f, 4.0f)));
    }

    @Test
    public void testIsSimilar() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f other = new Vector4f(1.1f, 2.1f, 3.1f, 4.1f);

        assertTrue(target.isSimilar(other, 0.2f));
        assertFalse(target.isSimilar(other, 0.0f));
        assertFalse(target.isSimilar(null, 1.0f));
    }

    @Test
    public void testMaxLocal() {
        final Vector4f target = new Vector4f(1.0f, 5.0f, 3.0f, 7.0f);

        final Vector4f retval = target.maxLocal(new Vector4f(4.0f, 2.0f, 6.0f, 1.0f));

        assertEquals(target, retval);
        assertEquals(4.0f, retval.x, 0.0f);
        assertEquals(5.0f, retval.y, 0.0f);
        assertEquals(6.0f, retval.z, 0.0f);
        assertEquals(7.0f, retval.w, 0.0f);
    }

    @Test
    public void testMinLocal() {
        final Vector4f target = new Vector4f(1.0f, 5.0f, 3.0f, 7.0f);

        final Vector4f retval = target.minLocal(new Vector4f(4.0f, 2.0f, 6.0f, 1.0f));

        assertEquals(target, retval);
        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(2.0f, retval.y, 0.0f);
        assertEquals(3.0f, retval.z, 0.0f);
        assertEquals(1.0f, retval.w, 0.0f);
    }

    @Test
    public void testScaleAdd() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f add = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

        final Vector4f retval = target.scaleAdd(2.0f, add);

        assertEquals(target, retval);
        assertEquals(3.0f, retval.x, 0.0f);
        assertEquals(5.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(9.0f, retval.w, 0.0f);
    }

    @Test
    public void testScaleAdd_withMult() {
        final Vector4f target = new Vector4f();
        final Vector4f mult = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f add = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);

        final Vector4f retval = target.scaleAdd(2.0f, mult, add);

        assertEquals(target, retval);
        assertEquals(3.0f, retval.x, 0.0f);
        assertEquals(5.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
        assertEquals(9.0f, retval.w, 0.0f);
    }

    @Test
    public void testInterpolateLocal() {
        final Vector4f target = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f finalVec = new Vector4f(10.0f, 10.0f, 10.0f, 10.0f);

        final Vector4f retval = target.interpolateLocal(finalVec, 0.5f);

        assertEquals(target, retval);
        assertEquals(5.0f, retval.x, 0.001f);
        assertEquals(5.0f, retval.y, 0.001f);
        assertEquals(5.0f, retval.z, 0.001f);
        assertEquals(5.0f, retval.w, 0.001f);
    }

    @Test
    public void testInterpolateLocal_beginAndFinal() {
        final Vector4f target = new Vector4f();
        final Vector4f beginVec = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f finalVec = new Vector4f(10.0f, 10.0f, 10.0f, 10.0f);

        final Vector4f retval = target.interpolateLocal(beginVec, finalVec, 0.5f);

        assertEquals(target, retval);
        assertEquals(5.0f, retval.x, 0.001f);
        assertEquals(5.0f, retval.y, 0.001f);
        assertEquals(5.0f, retval.z, 0.001f);
        assertEquals(5.0f, retval.w, 0.001f);
    }

    @Test
    public void testZero() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        final Vector4f retval = target.zero();

        assertEquals(target, retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testGet() {
        final Vector4f target = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        assertEquals(1.0f, target.get(0), 0.0f);
        assertEquals(2.0f, target.get(1), 0.0f);
        assertEquals(3.0f, target.get(2), 0.0f);
        assertEquals(4.0f, target.get(3), 0.0f);
    }

    @Test
    public void testGet_illegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Vector4f(1.0f, 2.0f, 3.0f, 4.0f).get(4));
    }

    @Test
    public void testSet_byIndex() {
        final Vector4f target = new Vector4f();
        target.set(0, 1.0f);
        assertEquals(1.0f, target.x, 0.0f);

        target.set(1, 2.0f);
        assertEquals(2.0f, target.y, 0.0f);

        target.set(2, 3.0f);
        assertEquals(3.0f, target.z, 0.0f);

        target.set(3, 4.0f);
        assertEquals(4.0f, target.w, 0.0f);
    }

    @Test
    public void testSet_byIndex_illegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> new Vector4f().set(4, 1.0f));
    }

    @Test
    public void testToArray() {
        final float[] store = {0.0f, 0.0f, 0.0f, 0.0f};
        final float[] retval = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f).toArray(store);

        assertEquals(store, retval);
        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f}, retval, 0.0f);
    }

    @Test
    public void testToArray_null() {
        final float[] retval = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f).toArray(null);

        assertArrayEquals(new float[]{1.0f, 2.0f, 3.0f, 4.0f}, retval, 0.0f);
    }

    @Test
    public void testSetX() {
        final Vector4f retval = new Vector4f().setX(5.0f);

        assertNotNull(retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testSetY() {
        final Vector4f retval = new Vector4f().setY(5.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(5.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testSetZ() {
        final Vector4f retval = new Vector4f().setZ(5.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(5.0f, retval.z, 0.0f);
        assertEquals(0.0f, retval.w, 0.0f);
    }

    @Test
    public void testSetW() {
        final Vector4f retval = new Vector4f().setW(5.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
        assertEquals(5.0f, retval.w, 0.0f);
    }

    @Test
    public void testProject() {
        final Vector4f target = new Vector4f(3.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f other = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);

        final Vector4f retval = target.project(other);

        assertNotNull(retval);
        assertEquals(3.0f, retval.x, 0.001f);
        assertEquals(0.0f, retval.y, 0.001f);
        assertEquals(0.0f, retval.z, 0.001f);
        assertEquals(0.0f, retval.w, 0.001f);
    }

    @Test
    public void testProject2() {
        final Vector4f target = new Vector4f(1.0f, 1.0f, 0.0f, 0.0f);
        final Vector4f other = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);

        final Vector4f retval = target.project(other);

        assertNotNull(retval);
        assertEquals(1.0f, retval.x, 0.001f);
        assertEquals(0.0f, retval.y, 0.001f);
        assertEquals(0.0f, retval.z, 0.001f);
        assertEquals(0.0f, retval.w, 0.001f);
    }

    @Test
    public void testAngleBetween() {
        final Vector4f v1 = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f v2 = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);

        assertEquals(0.0f, v1.angleBetween(v2), 0.001f);
    }

    @Test
    public void testAngleBetween2() {
        final Vector4f v1 = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);
        final Vector4f v2 = new Vector4f(0.0f, 1.0f, 0.0f, 0.0f);

        assertEquals(FastMath.HALF_PI, v1.angleBetween(v2), 0.001f);
    }

    @Test
    public void testEquals() {
        final Vector4f v1 = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f v2 = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f v3 = new Vector4f(1.0f, 2.0f, 3.0f, 5.0f);

        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        assertNotEquals(v1, null);
        assertNotEquals(v1, "not a vector");
    }

    @Test
    public void testHashCode() {
        final Vector4f v1 = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);
        final Vector4f v2 = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        assertEquals(v1.hashCode(), v2.hashCode());
    }
}
