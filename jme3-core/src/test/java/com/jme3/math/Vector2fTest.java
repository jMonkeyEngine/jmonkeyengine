/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

public class Vector2fTest {

    @Test
    public void testAdd() {
        final Vector2f target = new Vector2f(1.0f, Float.NaN);
        final Vector2f vec = new Vector2f(-1.0f, 2.93f);

        final Vector2f retval = target.add(vec);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
    }

    @Test
    public void testAdd2() {
        final Vector2f target = new Vector2f(0.0f, -7.52f);
        final Vector2f other = new Vector2f(1.42f, 7.52f);
        final Vector2f result = new Vector2f();
        final Vector2f retval = target.add(other, result);

        assertNotNull(retval);
        assertEquals(retval, result);
        assertEquals(1.42f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testAdd3() {
        final Vector2f target = new Vector2f(0.0f, -7.52f);
        final Vector2f other = new Vector2f(1.42f, 7.52f);
        final Vector2f retval = target.add(other, null);

        assertNotNull(retval);
        assertEquals(1.42f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testAdd4() {
        final Vector2f target = new Vector2f(0.0f, -7.52f);
        final Vector2f retval = target.add(null);

        assertNull(retval);
    }

    @Test
    public void testAddLocal() {
        final Vector2f target = new Vector2f(0.0f, -7.52f);
        final Vector2f retval = target.addLocal(null);

        assertNull(retval);
    }

    @Test
    public void testAddLocal2() {
        final Vector2f target = new Vector2f(0.0f, -7.52f);
        final Vector2f retval = target.addLocal(new Vector2f(2.0f, 6.2f));

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(-1.32f, retval.y, 0.01f);
    }

    @Test
    public void testAddLocal3() {
        final Vector2f target = new Vector2f(0.0f, -7.52f);
        final Vector2f retval = target.addLocal(2.0f, 6.2f);

        assertNotNull(retval);
        assertEquals(2.0f, retval.x, 0.0f);
        assertEquals(-1.32f, retval.y, 0.01f);
    }

    @Test
    public void testDot() {
        final Vector2f target = new Vector2f(0.0f, -1.5f);
        assertEquals(-4.5f, target.dot(new Vector2f(2.0f, 3.0f)), 0.0f);
    }

    @Test
    public void testDot2() {
        final Vector2f target = new Vector2f(0.0f, -1.5f);
        assertEquals(0.0f, target.dot(null), 0.0f);
    }

    @Test
    public void testAngleBetween() {
        final Vector2f target = new Vector2f(Float.NaN, 6.08159e-39f);
        final Vector2f otherVector = new Vector2f(3.76643e-39f, -2.97033e+38f);

        assertEquals(Float.NaN, target.angleBetween(otherVector), 0.0f);
    }

    @Test
    public void testAngleBetween2() {
        final Vector2f target = new Vector2f(-0.779272f, -2.08408e+38f);
        final Vector2f otherVector = new Vector2f(4.50029e-39f, -1.7432f);

        assertEquals(0.0f, target.angleBetween(otherVector), 0.0f);
    }

    @Test
    public void testAngleBetween3() {
        final Vector2f target = new Vector2f(-8.57f, 5.93f);
        final Vector2f otherVector = new Vector2f(6.59f, -2.04f);

        assertEquals(-2.8364947f, target.angleBetween(otherVector), 0.01f);
    }

    @Test
    public void testAngleBetween4() {
        final Vector2f target = new Vector2f(0.0f, -1.0f);
        final Vector2f otherVector = new Vector2f(1.0f, 0.0f);

        assertEquals(1.57f, target.angleBetween(otherVector), 0.01f);
    }

    @Test
    public void testCross() {
        final Vector2f target = new Vector2f(-1.55f, 2.07f);
        final Vector2f v = new Vector2f(4.39f, 1.11f);

        final Vector3f retval = target.cross(v);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(-10.807f, retval.z, 0.01f);
    }

    @Test
    public void testCross2() {
        final Vector2f target = new Vector2f(Float.NaN, 0.042f);
        final Vector2f v = new Vector2f(0.0012f, 7.64f);

        final Vector3f retval = target.cross(v);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(Float.NaN, retval.z, 0.0f);
    }

    @Test
    public void testDeterminant() {
        final Vector2f target = new Vector2f(7.814f, 2.570f);
        assertEquals(-56.96825f, target.determinant(new Vector2f(1.607f, -6.762f)), 0.0001f);
    }

    /**
     * With very large components, {@code distanceSquared} overflows to infinity
     * in float, so {@code distance} returns infinity (unlike {@link Vector3f#distance}
     * which uses double-precision intermediates).
     */
    @Test
    public void testDistance() {
        final Vector2f target = new Vector2f(3.86405e+18f, 3.02146e+23f);
        final Vector2f v = new Vector2f(-2.0f, -1.61503e+19f);

        assertEquals(Float.POSITIVE_INFINITY, target.distance(v), 0f);
    }

    @Test
    public void testDistance2() {
        final Vector2f target = new Vector2f(5.0f, 4.0f);
        final Vector2f v = new Vector2f(-2.0f, -7.0f);

        assertEquals(13.038404f, target.distance(v), 0.0f);
    }

    @Test
    public void testDivide_byComponents() {
        final Vector2f target = new Vector2f(0.0f, 8.63998e+37f);
        final Vector2f retval = target.divide(0.0f, Float.POSITIVE_INFINITY);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testDivide_byScalar() {
        final Vector2f target = new Vector2f(2e+28f, 7e+19f);

        final Vector2f retval = target.divide(0.0f);

        assertNotNull(retval);
        assertEquals(Float.POSITIVE_INFINITY, retval.x, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.y, 0.0f);
    }

    @Test
    public void testDivide_byScalar2() {
        final Vector2f target = new Vector2f(1.98f, 7.43f);

        final Vector2f retval = target.divide(3.5f);

        assertNotNull(retval);
        assertEquals(0.5657f, retval.x, 0.001f);
        assertEquals(2.1228f, retval.y, 0.001f);
    }

    @Test
    public void testDivideLocal_byScalar() {
        final Vector2f target = new Vector2f(1.98f, 7.43f);

        final Vector2f retval = target.divideLocal(3.5f);

        assertNotNull(retval);
        assertEquals(0.5657f, retval.x, 0.001f);
        assertEquals(2.1228f, retval.y, 0.001f);
    }

    @Test
    public void testDivideLocal2_byComponents() {
        final Vector2f target = new Vector2f(1.98f, 7.43f);

        final Vector2f retval = target.divideLocal(1.2f, 2.5f);

        assertNotNull(retval);
        assertEquals(1.65f, retval.x, 0.001f);
        assertEquals(2.972f, retval.y, 0.001f);
    }

    @Test
    public void testInterpolateLocal() {
        final Vector2f target = new Vector2f();
        final Vector2f beginVec = new Vector2f(0.0f, -9.094f);
        final Vector2f finalVec = new Vector2f(-0.0f, 1.355f);

        final Vector2f retval = target.interpolateLocal(beginVec, finalVec, -4.056f);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(-51.475147f, retval.y, 0.01f);
    }

    @Test
    public void testInterpolateLocal2() {
        final Vector2f target = new Vector2f(1.5f, 3.5f);
        final Vector2f other = new Vector2f(5.0f, 1.5f);

        final Vector2f retval = target.interpolateLocal(other, 3.0f);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(12.0f, retval.x, 0.0f);
        assertEquals(-2.5f, retval.y, 0.01f);
    }

    @Test
    public void testIsSimilar() {
        final Vector2f target = new Vector2f(-1.14f, 8.50f);
        final Vector2f other = new Vector2f(-1.52f, 8.50f);
        assertTrue(target.isSimilar(other, 2.0f));

        final Vector2f target_2 = new Vector2f(-1.14f, 8.50f);
        final Vector2f other_2 = new Vector2f(-1.52f, 8.50f);
        assertFalse(target_2.isSimilar(other_2, 0.0f));

        final Vector2f target_3 = new Vector2f(-1.14f, 8.50f);
        final Vector2f other_3 = null;
        assertFalse(target_3.isSimilar(other_3, 0.0f));

        final Vector2f target_4 = new Vector2f(-1.14f, 1.88f);
        final Vector2f other_4 = new Vector2f(-1.52f, 3.76f);
        assertFalse(target_4.isSimilar(other_4, 1.2f));

        final Vector2f target_5 = new Vector2f(-1.14f, -1.14f);
        final Vector2f other_5 = new Vector2f(-1.52f, -1.52f);
        assertTrue(target_5.isSimilar(other_5, 1.2f));

        final Vector2f target_6 = new Vector2f(-1.14f, -11.14f);
        final Vector2f other_6 = new Vector2f(-1.1f, -1.52f);
        assertFalse(target_6.isSimilar(other_6, 1.2f));
    }

    @Test
    public void testIsValidVector() {
        assertFalse(Vector2f.isValidVector(new Vector2f(Float.NaN, 2.1f)));
        assertFalse(Vector2f.isValidVector(new Vector2f(Float.POSITIVE_INFINITY, 1.5f)));
        assertFalse(Vector2f.isValidVector(new Vector2f(Float.NEGATIVE_INFINITY, 2.5f)));
        assertFalse(Vector2f.isValidVector(null));

        assertTrue(Vector2f.isValidVector(new Vector2f()));
        assertTrue(Vector2f.isValidVector(new Vector2f(1.5f, 5.7f)));
    }

    @Test
    public void testLength() {
        /*
         * lengthSquared underflows in float, so length() is 0 (unlike
         * {@link Vector3f#length} which uses double-precision intermediates).
         */
        assertEquals(0.0f,
                new Vector2f(1.88079e-37f, 1.55077e-36f).length(), 0f);

        assertEquals(Float.NaN, new Vector2f(Float.NaN, 0.0f).length(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, new Vector2f(Float.POSITIVE_INFINITY, 1.0f).length(), 0.0f);

        assertEquals(3.7215588f, new Vector2f(1.9f, 3.2f).length(), 0.001f);
        /*
         * lengthSquared overflows in float, so length() is infinity.
         */
        assertEquals(Float.POSITIVE_INFINITY,
                new Vector2f(1.8e37f, 1.8e37f).length(), 0.0f);
    }

    @Test
    public void testMult() {
        final Vector2f target = new Vector2f(4.9e+27f, 3.1e-20f);
        final Vector2f retval = target.mult(0, 4.4e-29f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testMult2() {
        final Vector2f target = new Vector2f(1.12f, 1.21f);
        final Vector2f retval = target.mult(1.09f, 5.87f);

        assertNotNull(retval);
        assertEquals(1.2208f, retval.x, 0.0f);
        assertEquals(7.1027f, retval.y, 0.0f);
    }

    @Test
    public void testMult3() {
        final Vector2f retval = new Vector2f(3.24f, 6.63f).mult(1.5f);

        assertNotNull(retval);
        assertEquals(4.86f, retval.x, 0.0f);
        assertEquals(9.945f, retval.y, 0.0f);
    }

    @Test
    public void testMult4() {
        final Vector2f product = new Vector2f();
        final Vector2f retval = new Vector2f(3.24f, 6.63f).mult(1.5f, product);

        assertEquals(product, retval);

        assertNotNull(retval);
        assertEquals(4.86f, retval.x, 0.0f);
        assertEquals(9.945f, retval.y, 0.0f);
    }

    @Test
    public void testMult5() {
        final Vector2f retval = new Vector2f(3.24f, 6.63f).mult(1.5f, null);

        assertNotNull(retval);
        assertEquals(4.86f, retval.x, 0.0f);
        assertEquals(9.945f, retval.y, 0.0f);
    }

    @Test
    public void testMultLocal() {
        final Vector2f target = new Vector2f(1.26f, 4.95f);
        final Vector2f retval = target.multLocal(1.3f, 3.5f);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(1.6379999f, retval.x, 0.01f);
        assertEquals(17.324999f, retval.y, 0.01f);
    }

    @Test
    public void testMultLocal2() {
        final Vector2f target = new Vector2f(1.26f, 4.95f);
        final Vector2f retval = target.multLocal(new Vector2f(1.3f, 3.5f));

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(1.6379999f, retval.x, 0.01f);
        assertEquals(17.324999f, retval.y, 0.01f);
    }

    @Test
    public void testMultLocal3() {
        final Vector2f target = new Vector2f(1.26f, 4.95f);
        final Vector2f retval = target.multLocal(null);

        assertNull(retval);
    }

    @Test
    public void testNegate() {
        final Vector2f target = new Vector2f(-1.0f, 2.0f);
        final Vector2f retval = target.negate();

        assertNotNull(retval);
        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(-2.0f, retval.y, 0.0f);

        final Vector2f retval2 = retval.negate();

        assertEquals(retval2, target);
    }

    @Test
    public void testNegate2() {
        final Vector2f retval = new Vector2f(Float.NaN, Float.POSITIVE_INFINITY).negate();

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
    }

    @Test
    public void testNegateLocal() {
        final Vector2f target = new Vector2f(-4.5f, Float.POSITIVE_INFINITY);
        final Vector2f retval = target.negateLocal();

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(4.5f, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
    }

    @Test
    public void testNormalizeLocal() {
        final Vector2f target = new Vector2f(6.9282f, Float.NaN);

        final Vector2f retval = target.normalizeLocal();

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
    }

    @Test
    public void testNormalize() {
        final Vector2f retval = new Vector2f(2.071f, 2.45f).normalize();

        assertNotNull(retval);
        assertEquals(0.6454f, retval.x, 0.01f);
        assertEquals(0.7638f, retval.y, 0.01f);
    }

    @Test
    public void testNormalize2() {
        final Vector2f target = new Vector2f(1.0f, 0.0f);
        final Vector2f retval = target.normalize();

        assertNotNull(retval);

        assertEquals(retval, target);

        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testSet() {
        Vector2f target = new Vector2f(0.0f, 0.0f);
        target.set(5.0f, 3.0f);
        assertEquals(5.0f, target.x, 0.0f);
        assertEquals(3.0f, target.y, 0.0f);

        target = new Vector2f(0.0f, 0.0f);
        target.set(new Vector2f(8.0f, 2.0f));
        assertEquals(8.0f, target.x, 0.0f);
        assertEquals(2.0f, target.y, 0.0f);
    }

    @Test
    public void testSetX() {
        final Vector2f retval = new Vector2f(0.0f, 0.0f).setX(3.0f);

        assertNotNull(retval);
        assertEquals(3.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testSetY() {
        final Vector2f retval = new Vector2f(0.0f, 0.0f).setY(3.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(3.0f, retval.y, 0.0f);
    }

    @Test
    public void testGetX() {
        final Vector2f target = new Vector2f(0.0f, 0.5f);

        assertEquals(0.0f, target.getX(), 0.0f);
        assertEquals(0.5f, target.getY(), 0.0f);
    }

    @Test
    public void testSubtract() {
        final Vector2f retval = new Vector2f(12.0f, 8.0f).subtract(new Vector2f(7.0f, 4.0f));

        assertNotNull(retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
    }

    @Test
    public void testSubtract2() {
        final Vector2f target = new Vector2f(12.0f, 8.0f);
        final Vector2f other = new Vector2f();
        final Vector2f retval = target.subtract(new Vector2f(7.0f, 4.0f), other);

        assertEquals(other, retval);

        assertNotNull(retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
    }

    @Test
    public void testSubtract3() {
        final Vector2f other = null;
        final Vector2f target = new Vector2f(12.0f, 8.0f);
        final Vector2f retval = target.subtract(new Vector2f(7.0f, 4.0f), other);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
    }

    @Test
    public void testSubtract4() {
        final Vector2f target = new Vector2f(12.0f, 8.0f);
        final Vector2f retval = target.subtract(7.0f, 4.0f);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
    }

    @Test
    public void testSubtractLocal() {
        final Vector2f target = new Vector2f(12.0f, 8.0f);
        final Vector2f retval = target.subtractLocal(new Vector2f(7.0f, 4.0f));

        assertEquals(target, retval);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
    }

    @Test
    public void testSubtractLocal2() {
        final Vector2f target = new Vector2f(12.0f, 8.0f);
        final Vector2f retval = target.subtractLocal(7.0f, 4.0f);

        assertEquals(target, retval);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
    }

    @Test
    public void testSubtractLocal3() {
        final Vector2f target = new Vector2f(12.0f, 8.0f);
        final Vector2f retval = target.subtractLocal(null);

        assertNull(retval);
    }

    @Test
    public void testToArray() {
        final float[] store = {0.0f, 0.0f};
        final float[] retval = new Vector2f(1.0f, 2.0f).toArray(store);
        assertEquals(store, retval);
        assertArrayEquals(new float[] {1.0f, 2.0f}, retval, 0.0f);

        final float[] retval2 = new Vector2f(1.0f, 2.0f).toArray(new float[]{4.0f, 5.0f});
        assertArrayEquals(new float[] {1.0f, 2.0f}, retval2, 0.0f);

        final float[] retval3 = new Vector2f(1.0f, 2.0f).toArray(null);
        assertArrayEquals(new float[] {1.0f, 2.0f}, retval3, 0.0f);
    }

    @Test
    public void testZero() {
        final Vector2f target = new Vector2f(1.0f, 5.0f);
        final Vector2f retval = target.zero();

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
    }

    @Test
    public void testGetAngle() {
        assertEquals(0.0f, new Vector2f(1.0f, 0.0f).getAngle(), 0.001f);
        assertEquals(FastMath.HALF_PI, new Vector2f(0.0f, 1.0f).getAngle(), 0.001f);
        assertEquals(FastMath.PI, new Vector2f(-1.0f, 0.0f).getAngle(), 0.001f);
    }

    @Test
    public void testSmallestAngleBetween() {
        final Vector2f v1 = new Vector2f(1.0f, 0.0f);
        final Vector2f v2 = new Vector2f(0.0f, 1.0f);

        assertEquals(FastMath.HALF_PI, v1.smallestAngleBetween(v2), 0.001f);
    }

    @Test
    public void testSmallestAngleBetween_same() {
        final Vector2f v = new Vector2f(1.0f, 0.0f);

        assertEquals(0.0f, v.smallestAngleBetween(v), 0.001f);
    }

    @Test
    public void testRotateAroundOrigin() {
        final Vector2f target = new Vector2f(1.0f, 0.0f);

        target.rotateAroundOrigin(FastMath.HALF_PI, false);

        assertEquals(0.0f, target.x, 0.001f);
        assertEquals(1.0f, target.y, 0.001f);
    }

    @Test
    public void testRotateAroundOrigin_clockwise() {
        final Vector2f target = new Vector2f(1.0f, 0.0f);

        target.rotateAroundOrigin(FastMath.HALF_PI, true);

        assertEquals(0.0f, target.x, 0.001f);
        assertEquals(-1.0f, target.y, 0.001f);
    }

    @Test
    public void testEquals() {
        final Vector2f v1 = new Vector2f(1.0f, 2.0f);
        final Vector2f v2 = new Vector2f(1.0f, 2.0f);
        final Vector2f v3 = new Vector2f(1.0f, 3.0f);

        assertEquals(v1, v2);
        assertNotEquals(v1, v3);
        assertNotEquals(v1, null);
        assertNotEquals(v1, "not a vector");
    }

    @Test
    public void testHashCode() {
        final Vector2f v1 = new Vector2f(1.0f, 2.0f);
        final Vector2f v2 = new Vector2f(1.0f, 2.0f);

        assertEquals(v1.hashCode(), v2.hashCode());
    }
}
