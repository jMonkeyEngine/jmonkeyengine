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

import org.junit.Test;

import static org.junit.Assert.*;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class Vector3fTest {

    @Rule public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testAdd() {
        final Vector3f target = new Vector3f(1.0f, Float.NaN, 5.36f);
        final Vector3f vec = new Vector3f(-1.0f, 2.93f, -5.36f);

        final Vector3f retval = target.add(vec);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testAdd2() {
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f retval = target.add(1.42f, 7.52f, 1.1f);

        assertNotNull(retval);
        assertEquals(1.42f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(4.2f, retval.z, 0.0f);
    }

    @Test
    public void testAdd3() {
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f other = new Vector3f(1.42f, 7.52f, 1.1f);
        final Vector3f result = new Vector3f();
        final Vector3f retval = target.add(other, result);

        assertNotNull(retval);
        assertEquals(retval, result);
        assertEquals(1.42f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(4.2f, retval.z, 0.0f);
    }

    @Test
    public void testAdd4() {
        thrown.expect(NullPointerException.class);
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f other = new Vector3f(1.42f, 7.52f, 1.1f);
        final Vector3f result = null;
        target.add(other, result);
    }


    @Test
    public void testAdd5() {
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f retval = target.add(null);

        assertNull(retval);
    }

    @Test
    public void testAddLocal() {
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f retval = target.addLocal(null);

        assertNull(retval);
    }

    @Test
    public void testAddLocal2() {
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f retval = target.addLocal(new Vector3f(2.0f, 6.2f, 8.3f));

        assertNotNull(retval);
        assertEquals(retval.x, 2.0f, 0.0f);
        assertEquals(retval.y, -1.32f, 0.01f);
        assertEquals(retval.z, 11.4f, 0.0f);
    }

    @Test
    public void testAddLocal3() {
        final Vector3f target = new Vector3f(0.0f, -7.52f, 3.1f);
        final Vector3f retval = target.addLocal(2.0f, 6.2f, 8.3f);

        assertNotNull(retval);
        assertEquals(retval.x, 2.0f, 0.0f);
        assertEquals(retval.y, -1.32f, 0.01f);
        assertEquals(retval.z, 11.4f, 0.0f);
    }

    @Test
    public void testDot() {
        final Vector3f target = new Vector3f(0.0f, -1.5f, 3.0f);
        assertEquals(19.5f, target.dot(new Vector3f(2.0f, 3.0f, 8.0f)), 0.0f);
    }

    @Test
    public void testDot2() {
        final Vector3f target = new Vector3f(0.0f, -1.5f, 3.0f);
        assertEquals(0.0f, target.dot(null), 0.0f);
    }

    @Test
    public void testAngleBetween() {
        final Vector3f target = new Vector3f(Float.NaN, 6.08159e-39f, 5.33333f);
        final Vector3f otherVector = new Vector3f(3.76643e-39f, -2.97033e+38f, 0.09375f);

        assertEquals(3.141f, target.angleBetween(otherVector), 0.001f);
    }

    @Test
    public void testAngleBetween2() {
        final Vector3f target = new Vector3f(-0.779272f, -2.08408e+38f, 5.33333f);
        final Vector3f otherVector = new Vector3f(4.50029e-39f, -1.7432f, 0.09375f);

        assertEquals(0.0f, target.angleBetween(otherVector), 0.0f);
    }

    @Test
    public void testAngleBetween3() {
        final Vector3f target = new Vector3f(-8.57f, 5.93f, 5.33f);
        final Vector3f otherVector = new Vector3f(6.59f, -2.04f, -0.09f);

        assertEquals(3.141f, target.angleBetween(otherVector), 0.01f);
    }

    @Test
    public void testAngleBetween4() {
        final Vector3f target = new Vector3f(0.0f, -1.0f, 0.0f);
        final Vector3f otherVector = new Vector3f(1.0f, 0.0f, 0.0f);

        assertEquals(1.57f, target.angleBetween(otherVector), 0.01f);
    }

    @Test
    public void testCross() {
        final Vector3f target = new Vector3f(-1.55f, 2.07f, -0.0f);
        final Vector3f v = new Vector3f(4.39f, 1.11f, 0.0f);
        final Vector3f result = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = target.cross(v, result);

        assertEquals(retval, result);

        assertNotNull(result);
        assertEquals(0.0f, result.x, 0.0f);
        assertEquals(0.0f, result.y, 0.0f);
        assertEquals(-10.807f, result.z, 0.01f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(-10.807f, retval.z, 0.01f);
    }

    @Test
    public void testCross2() {
        final Vector3f target = new Vector3f(Float.NaN, 0.042f, 1.76f);
        final Vector3f v = new Vector3f(0.0012f, 7.64f, 4.50f);

        final Vector3f retval = target.cross(v);

        assertNotNull(retval);
        assertEquals(-13.257f, retval.x, 0.001f);
        assertEquals(Float.NaN, retval.y, 0.0f);
        assertEquals(Float.NaN, retval.z, 0.0f);
    }

    @Test
    public void testCross3() {
        final Vector3f target = new Vector3f(7.814f, 2.570f, 1.320f);
        final Vector3f result = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = target.cross(1.607f, -6.762f, -0.007f, result);

        assertEquals(result, retval);

        assertNotNull(retval);
        assertEquals(8.90785f, retval.x, 0.0001f);
        assertEquals(2.17593f, retval.y, 0.0001f);
        assertEquals(-56.96825f, retval.z, 0.0001f);
    }

    @Test
    public void testCrossLocal() {
        final Vector3f target = new Vector3f(-1.80144e+16f, 0.0f, 8.4323e+06f);
        final Vector3f v = new Vector3f(8.9407e-08f, 0.0f, -1.05324e-35f);

        final Vector3f retval = target.crossLocal(v);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(-0.0f, retval.x, 0.0f);
        assertEquals(0.753f, retval.y, 0.01f);
        assertEquals(-0.0f, retval.z, 0.0f);
    }

    /**
     * Verify that distance() doesn't always overflow when distanceSquared >
     * Float.MAX_VALUE .
     */
    @Test
    public void testDistance() {
        final Vector3f target = new Vector3f(3.86405e+18f, 3.02146e+23f, 0.171875f);
        final Vector3f v = new Vector3f(-2.0f, -1.61503e+19f, 0.171875f);
   
        assertEquals(3.0216215e23f, target.distance(v), 0f);
    }

    @Test
    public void testDistance2() {
        final Vector3f target = new Vector3f(5.0f, 4.0f, 6.0f);
        final Vector3f v = new Vector3f(-2.0f, -7.0f, 0.5f);

        assertEquals(14.150971f, target.distance(v), 0.0f);
    }

    @Test
    public void testDivide_byVector() {
        final Vector3f target = new Vector3f(0.0f, 8.63998e+37f, 3.23117e-27f);
        final Vector3f divideBy = new Vector3f(0.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        final Vector3f retval = target.divide(divideBy);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testDivide_byScalar() {
        final Vector3f target = new Vector3f(2e+28f, 7e+19f, 3.e+23f);

        final Vector3f retval = target.divide(0.0f);

        assertNotNull(retval);
        assertEquals(Float.POSITIVE_INFINITY, retval.x, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.z, 0.0f);
    }

    @Test
    public void testDivide_byScalar2() {
        final Vector3f target = new Vector3f(1.98f, 7.43f, 9.61f);

        final Vector3f retval = target.divide(3.5f);

        assertNotNull(retval);
        assertEquals(0.5657f, retval.x, 0.001f);
        assertEquals(2.1228f, retval.y, 0.001f);
        assertEquals(2.7457f, retval.z, 0.001f);
    }

    @Test
    public void testDivideLocal_byScalar() {
        final Vector3f target = new Vector3f(1.98f, 7.43f, 9.61f);

        final Vector3f retval = target.divideLocal(3.5f);

        assertNotNull(retval);
        assertEquals(0.5657f, retval.x, 0.001f);
        assertEquals(2.1228f, retval.y, 0.001f);
        assertEquals(2.7457f, retval.z, 0.001f);
    }

    @Test
    public void testDivideLocal2_byVector() {
        final Vector3f target = new Vector3f(1.98f, 7.43f, 9.61f);

        final Vector3f retval = target.divideLocal(new Vector3f(1.2f, 2.5f, 6.3f));

        assertNotNull(retval);
        assertEquals(1.65f, retval.x, 0.001f);
        assertEquals(2.972f, retval.y, 0.001f);
        assertEquals(1.5253967f, retval.z, 0.001f);
    }

    @Test
    public void testGenerateComplementBasis() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(-7.0f, Float.NaN, Float.NaN);

        Vector3f.generateComplementBasis(u, v, w);

        assertNotNull(v);
        assertEquals(Float.NaN, v.x, 0.0f);
        assertEquals(Float.NaN, v.y, 0.0f);
        assertEquals(Float.NaN, v.z, 0.0f);
        assertNotNull(u);
        assertEquals(0.0f, u.x, 0.0f);
        assertEquals(Float.NaN, u.y, 0.0f);
        assertEquals(Float.NaN, u.z, 0.0f);
    }

    @Test
    public void testGenerateComplementBasis2() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(-7.0f, 1.075f, Float.NaN);

        Vector3f.generateComplementBasis(u, v, w);

        assertNotNull(v);
        assertEquals(Float.NaN, v.x, 0.0f);
        assertEquals(Float.NaN, v.y, 0.0f);
        assertEquals(Float.NaN, v.z, 0.0f);
        assertNotNull(u);
        assertEquals(Float.NaN, u.x, 0.0f);
        assertEquals(0.0f, u.y, 0.0f);
        assertEquals(Float.NaN, u.z, 0.0f);
    }

    @Test
    public void testGenerateComplementBasis3() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(-7.0f, 1.075f, 4.3f);

        Vector3f.generateComplementBasis(u, v, w);

        assertNotNull(v);
        assertEquals(-0.9159, v.x, 0.001f);
        assertEquals(-8.2152, v.y, 0.001f);
        assertEquals(0.5626, v.z, 0.001f);
        assertNotNull(u);
        assertEquals(-0.5234f, u.x, 0.001f);
        assertEquals(0.0f, u.y, 0.0f);
        assertEquals(-0.8520f, u.z, 0.001f);
    }

    @Test
    public void testGenerateOrthonormalBasis() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(1.6e-37f, -2.24e-44f, -2.08e-36f);

        Vector3f.generateOrthonormalBasis(u, v, w);

        assertNotNull(v);
        assertEquals(Float.NEGATIVE_INFINITY, v.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, v.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, v.z, 0.0f);
        assertNotNull(u);
        assertEquals(Float.POSITIVE_INFINITY, u.x, 0.0f);
        assertEquals(0.0f, u.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, u.z, 0.0f);
    }

    @Test
    public void testGenerateOrthonormalBasis2() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(2e+20f, -5e-20f, -14e+20f);

        Vector3f.generateOrthonormalBasis(u, v, w);

        assertNotNull(v);
        assertEquals(Float.NaN, v.x, 0.0f);
        assertEquals(Float.NaN, v.y, 0.0f);
        assertEquals(Float.NaN, v.z, 0.0f);
        assertNotNull(u);
        assertEquals(Float.NaN, u.x, 0.0f);
        assertEquals(0.0f, u.y, 0.0f);
        assertEquals(Float.NaN, u.z, 0.0f);
        assertNotNull(w);
        assertEquals(0.0f, w.x, 0.0f);
        assertEquals(-0.0f, w.y, 0.0f);
        assertEquals(-0.0f, w.z, 0.0f);
    }

    @Test
    public void testGenerateOrthonormalBasis3() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(-1.24672e-39f, -1.25343e-39f, -2.08336e-36f);

        Vector3f.generateOrthonormalBasis(u, v, w);

        assertNotNull(v);
        assertEquals(Float.NEGATIVE_INFINITY, v.x, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, v.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, v.z, 0.0f);
        assertNotNull(u);
        assertEquals(0.0f, u.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, u.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, u.z, 0.0f);
    }

    @Test
    public void testGenerateOrthonormalBasis4() {
        final Vector3f u = new Vector3f();
        final Vector3f v = new Vector3f();
        final Vector3f w = new Vector3f(-7.0f, 1.075f, 4.3f);

        Vector3f.generateOrthonormalBasis(u, v, w);

        assertNotNull(v);
        assertEquals(-0.1105, v.x, 0.001f);
        assertEquals(-0.9915, v.y, 0.001f);
        assertEquals(0.0679, v.z, 0.001f);
        assertNotNull(u);
        assertEquals(-0.5234f, u.x, 0.001f);
        assertEquals(0.0f, u.y, 0.0f);
        assertEquals(-0.8520f, u.z, 0.001f);
    }

    @Test
    public void testGet_illegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        new Vector3f(0.0f, 0.0f, 0.0f).get(536_870_914);
    }

    @Test
    public void testGet() {
        final Vector3f target = new Vector3f(0.0f, 0.5f, 1.5f);

        assertEquals(0.0f, target.get(0), 0.0f);
        assertEquals(0.5f, target.get(1), 0.0f);
        assertEquals(1.5f, target.get(2), 0.0f);
    }

    @Test
    public void testInterpolateLocal() {
        final Vector3f target = new Vector3f();
        final Vector3f beginVec = new Vector3f(0.0f, -9.094f, 0.0f);
        final Vector3f finalVec = new Vector3f(-0.0f, 1.355f, 1.414f);

        final Vector3f retval = target.interpolateLocal(beginVec, finalVec, -4.056f);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(-51.475147f, retval.y, 0.01f);
        assertEquals(-5.736f, retval.z, 0.001f);
    }

    @Test
    public void testInterpolateLocal2() {
        final Vector3f target = new Vector3f(1.5f, 3.5f, 8.2f);
        final Vector3f other = new Vector3f(5.0f, 1.5f, 2.0f);

        final Vector3f retval = target.interpolateLocal(other, 3.0f);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(12.0f, retval.x, 0.0f);
        assertEquals(-2.5f, retval.y, 0.01f);
        assertEquals(-10.4f, retval.z, 0.001f);
    }

    @Test
    public void testIsSimilar() {
        final Vector3f target = new Vector3f(-1.14f, 8.50f, 1.88f);
        final Vector3f other = new Vector3f(-1.52f, 8.50f, 3.76f);
        assertTrue(target.isSimilar(other, 2.0f));

        final Vector3f target_2 = new Vector3f(-1.14f, 8.50f, 1.88f);
        final Vector3f other_2 = new Vector3f(-1.52f, 8.50f, 3.76f);
        assertFalse(target_2.isSimilar(other_2, 0.0f));

        final Vector3f target_3 = new Vector3f(-1.14f, 8.50f, 1.88f);
        final Vector3f other_3 = null;
        assertFalse(target_3.isSimilar(other_3, 0.0f));

        final Vector3f target_4 = new Vector3f(-1.14f, -1.14f, 1.88f);
        final Vector3f other_4 = new Vector3f(-1.52f, -1.52f, 3.76f);
        assertFalse(target_4.isSimilar(other_4, 1.2f));

        final Vector3f target_5 = new Vector3f(-1.14f, -1.14f, 1.88f);
        final Vector3f other_5 = new Vector3f(-1.52f, -1.52f, 3.76f);
        assertFalse(target_5.isSimilar(other_5, 1.2f));

        final Vector3f target_6 = new Vector3f(-1.14f, -11.14f, 1.0f);
        final Vector3f other_6 = new Vector3f(-1.1f, -1.52f, 1.0f);
        assertFalse(target_6.isSimilar(other_6, 1.2f));
    }

    @Test
    public void testIsUnitVector() {
        assertFalse(new Vector3f(1.07f, 2.12f, 3.32f).isUnitVector());
        assertFalse(new Vector3f(1.07f, 2.12f, Float.NaN).isUnitVector());
        assertTrue(new Vector3f(1.0f, 0.0f, 0.0f).isUnitVector());
        assertTrue(new Vector3f(0.0f, 1.0f, 0.0f).isUnitVector());
        assertTrue(new Vector3f(0.0f, 0.0f, 1.0f).isUnitVector());
        assertTrue(new Vector3f(0.0f, 0.0f, -1.0f).isUnitVector());
        assertTrue(new Vector3f(-1.0f, 0.0f, 0.0f).isUnitVector());
        assertTrue(new Vector3f(0.0f, -1.0f, 0.0f).isUnitVector());
    }

    @Test
    public void testIsValidVector() {
        assertFalse(Vector3f.isValidVector(new Vector3f(Float.NaN, 2.1f, 3.0f)));
        assertFalse(Vector3f.isValidVector(new Vector3f(Float.POSITIVE_INFINITY, 1.5f, 1.9f)));
        assertFalse(Vector3f.isValidVector(new Vector3f(Float.NEGATIVE_INFINITY, 2.5f, 8.2f)));
        assertFalse(Vector3f.isValidVector(null));

        assertTrue(Vector3f.isValidVector(new Vector3f()));
        assertTrue(Vector3f.isValidVector(new Vector3f(1.5f, 5.7f, 8.2f)));
    }

    @Test
    public void testLength() {
        /*
         * avoid underflow when lengthSquared is < Float.MIN_VALUE
         */
        assertEquals(1.5621336e-36f,
                new Vector3f(1.88079e-37f, 0.0f, 1.55077e-36f).length(), 0f);

        assertEquals(Float.NaN, new Vector3f(Float.NaN, 0.0f, 1.55077e-36f).length(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, new Vector3f(Float.POSITIVE_INFINITY, 0.0f, 1.0f).length(), 0.0f);

        assertEquals(4.0124f, new Vector3f(1.9f, 3.2f, 1.5f).length(), 0.001f);
        /*
         * avoid overflow when lengthSquared > Float.MAX_VALUE
         */
        assertEquals(2.5499999e37f,
                new Vector3f(1.8e37f, 1.8e37f, 1.5e36f).length(), 0.0f);
    }

    @Test
    public void testMaxLocal() {
        final Vector3f target = new Vector3f();

        final Vector3f retval = target.maxLocal(new Vector3f(-0.0f, -0.0f, -0.0f));

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testMaxLocal2() {
        final Vector3f target = new Vector3f(0.0f, 0.0f, -1.32931e+36f);

        final Vector3f retval = target.maxLocal(new Vector3f(-0.0f, -0.0f, 1.32923e+36f));

        assertEquals(target, retval);

        assertEquals(1.32923e+36f, target.z, 0.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(1.32923e+36f, retval.z, 0.0f);
    }


    @Test
    public void testMaxLocal3() {
        final Vector3f target = new Vector3f(0.0f, Float.NEGATIVE_INFINITY, -1.32931e+36f);

        final Vector3f retval = target.maxLocal(new Vector3f(-0.0f, Float.POSITIVE_INFINITY, 1.32923e+36f));

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.y, 0.0f);
        assertEquals(1.32923e+36f, retval.z, 0.0f);
    }


    @Test
    public void testMaxLocal4() {
        final Vector3f target = new Vector3f(-2.24208e-44f, Float.NEGATIVE_INFINITY, -1.32f);
        final Vector3f other = new Vector3f(0.0f, Float.POSITIVE_INFINITY, 1.35f);

        final Vector3f retval = target.maxLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0f, retval.x, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.y, 0.0f);
        assertEquals(1.35f, retval.z, 0.0f);
    }

    @Test
    public void testMinLocal() {
        final Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f other = new Vector3f(-0.0f, -0.0f, -0.0f);

        final Vector3f retval = target.minLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testMinLocal2() {
        final Vector3f target = new Vector3f(0.0f, 0.0f, Float.POSITIVE_INFINITY);
        final Vector3f other = new Vector3f(-0.0f, -0.0f, -0.0f);

        final Vector3f retval = target.minLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(-0.0f, retval.z, 0.0f);
    }

    @Test
    public void testMinLocal3() {
        final Vector3f target = new Vector3f(0.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        final Vector3f other = new Vector3f(-0.0f, Float.NEGATIVE_INFINITY, -0.0f);

        final Vector3f retval = target.minLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
        assertEquals(-0.0f, retval.z, 0.0f);
    }

    @Test
    public void testMinLocal4() {
        final Vector3f target = new Vector3f(1.43493e-42f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        final Vector3f other = new Vector3f(-0.0f, Float.NEGATIVE_INFINITY, -0.0f);

        final Vector3f retval = target.minLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(-0.0f, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
        assertEquals(-0.0f, retval.z, 0.0f);
    }


    @Test
    public void testMult() {
        final Vector3f target = new Vector3f(4.9e+27f, 3.1e-20f, 1.9e-31f);
        final Vector3f vec = new Vector3f(0, 4.4e-29f, 0);
        final Vector3f store = new Vector3f();

        final Vector3f retval = target.mult(vec, store);

        assertNotNull(retval);
        assertEquals(0, retval.x, 0.0f);
        assertEquals(0, retval.y, 0.0f);
        assertEquals(0, retval.z, 0.0f);
    }

    @Test
    public void testMult2() {
        final Vector3f target = new Vector3f(1.12f, 1.21f, 0.0f);
        final Vector3f vec = new Vector3f(1.09f, 5.87f, -5.2f);

        final Vector3f retval = target.mult(vec);

        assertNotNull(retval);
        assertEquals(1.2208f, retval.x, 0.0f);
        assertEquals(7.1027f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testMult3() {
        final Vector3f target = new Vector3f(1.12f, 1.21f, 0.0f);

        assertNull(target.mult(null));
    }

    @Test
    public void testMult4() {
        final Vector3f target = new Vector3f(1.12f, 1.21f, 0.0f);
        final Vector3f store = new Vector3f();

        assertNull(target.mult(null, store));
    }

    @Test
    public void testMult5() {
        final Vector3f retval = new Vector3f(3.24f, 6.63f, 7.81f).mult(1.5f);

        assertNotNull(retval);
        assertEquals(4.86f, retval.x, 0.0f);
        assertEquals(9.945f, retval.y, 0.0f);
        assertEquals(11.715f, retval.z, 0.0f);
    }

    @Test
    public void testMult6() {
        final Vector3f product = new Vector3f();
        final Vector3f retval = new Vector3f(3.24f, 6.63f, 7.81f).mult(1.5f, product);

        assertEquals(product, retval);

        assertNotNull(retval);
        assertEquals(4.86f, retval.x, 0.0f);
        assertEquals(9.945f, retval.y, 0.0f);
        assertEquals(11.715f, retval.z, 0.0f);
    }

    @Test
    public void testMult7() {
        final Vector3f retval = new Vector3f(3.24f, 6.63f, 7.81f).mult(1.5f, null);

        assertNotNull(retval);
        assertEquals(4.86f, retval.x, 0.0f);
        assertEquals(9.945f, retval.y, 0.0f);
        assertEquals(11.715f, retval.z, 0.0f);
    }

    @Test
    public void testMultLocal() {
        final Vector3f target = new Vector3f(1.26f, 4.95f, 5.90f);
        final Vector3f retval = target.multLocal(1.3f, 3.5f, 2.2f);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(1.6379999f, retval.x, 0.01f);
        assertEquals(17.324999f, retval.y, 0.01f);
        assertEquals(12.9800005f, retval.z, 0.01f);
    }

    @Test
    public void testMultLocal2() {
        final Vector3f target = new Vector3f(1.26f, 4.95f, 5.90f);
        final Vector3f retval = target.multLocal(new Vector3f(1.3f, 3.5f, 2.2f));

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(1.6379999f, retval.x, 0.01f);
        assertEquals(17.324999f, retval.y, 0.01f);
        assertEquals(12.9800005f, retval.z, 0.01f);
    }

    @Test
    public void testMultLocal3() {
        final Vector3f target = new Vector3f(1.26f, 4.95f, 5.90f);
        final Vector3f retval = target.multLocal(null);

        assertNull(retval);
    }

    @Test
    public void testNegate() {
        final Vector3f target = new Vector3f(-1.0f, 2.0f, -0.0f);
        final Vector3f retval = target.negate();

        assertNotNull(retval);
        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(-2.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);

        final Vector3f retval2 = retval.negate();

        assertEquals(retval2, target);
    }

    @Test
    public void testNegate2() {
        final Vector3f retval = new Vector3f(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).negate();

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.z, 0.0f);
    }

    @Test
    public void testNegateLocal() {
        final Vector3f target = new Vector3f(-4.5f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
        final Vector3f retval = target.negateLocal();

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(4.5f, retval.x, 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, retval.y, 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.z, 0.0f);
    }

    @Test
    public void testNormalizeLocal() {
        final Vector3f target = new Vector3f(6.9282f, Float.NaN, 4.694f);

        final Vector3f retval = target.normalizeLocal();

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
        assertEquals(Float.NaN, retval.z, 0.0f);
    }

    @Test
    public void testNormalize() {
        final Vector3f retval = new Vector3f(2.071f, 2.45f, 1.35f).normalize();

        assertNotNull(retval);
        assertEquals(0.5950255f, retval.x, 0.01f);
        assertEquals(0.70391715f, retval.y, 0.0f);
        assertEquals(0.3878727f, retval.z, 0.0f);
    }

    @Test
    public void testNormalize2() {
        final Vector3f target = new Vector3f(1.0f, 0.0f, 0.0f);
        final Vector3f retval = target.normalize();

        assertNotNull(retval);

        assertEquals(retval, target);

        assertEquals(1.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testProject() {
        final Vector3f target = new Vector3f(3.8e+15f, 2.1e-25f, 0.0f);
        final Vector3f other = new Vector3f(2e-28f, -3.6e+12f, Float.POSITIVE_INFINITY);

        final Vector3f retval = target.project(other);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
        assertEquals(Float.NaN, retval.z, 0.0f);
    }

    @Test
    public void testProject2() {
        final Vector3f target = new Vector3f(7.32f, 1.44f, 3.37f);
        final Vector3f other = new Vector3f(9.12f, -3.64f, 5.19f);

        final Vector3f retval = target.project(other);

        assertNotNull(retval);
        assertEquals(5.84f, retval.x, 0.01f);
        assertEquals(-2.33f, retval.y, 0.01f);
        assertEquals(3.32f, retval.z, 0.01f);
    }

    @Test
    public void testProjectLocal() {
        final Vector3f target = new Vector3f(-2.9e+17f, 3.9e-34f, 3.8e+20f);
        final Vector3f other = new Vector3f(5.4e-20f, -2.6e+36f, Float.NaN);

        final Vector3f retval = target.projectLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.z, 0.0f);
        assertEquals(Float.NaN, retval.x, 0.0f);
        assertEquals(Float.NaN, retval.y, 0.0f);
    }

    @Test
    public void testProjectLocal2() {
        final Vector3f target = new Vector3f(7.32f, 1.44f, 3.37f);
        final Vector3f other = new Vector3f(9.12f, -3.64f, 5.19f);

        final Vector3f retval = target.projectLocal(other);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(5.8409867f, retval.x, 0.01f);
        assertEquals(-2.331271f, retval.y, 0.01f);
        assertEquals(3.3239825f, retval.z, 0.01f);
    }

    @Test
    public void testScaleAdd() {
        final Vector3f target = new Vector3f();
        final Vector3f mult = new Vector3f(Float.POSITIVE_INFINITY, 5.60f, -1.74f);
        final Vector3f add = new Vector3f(-0.0f, -0.0f, 3.79f);

        final Vector3f retval = target.scaleAdd(1.70f, mult, add);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(Float.POSITIVE_INFINITY, retval.x, 0.0f);
        assertEquals(9.52f, retval.y, 0.0f);
        assertEquals(0.8319998f, retval.z, 0.0f);
    }

    @Test
    public void testScaleAdd2() {
        final Vector3f target = new Vector3f(4.86f, 6.10f, -1.74f);
        final Vector3f add = new Vector3f(-0.16f, -0.51f, 1.03f);

        final Vector3f retval = target.scaleAdd(1.99f, add);

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(9.5114f, retval.x, 0.001f);
        assertEquals(11.629f, retval.y, 0.001f);
        assertEquals(-2.4326f, retval.z, 0.001f);
    }

    @Test
    public void testSet_OutputIllegalArgumentException() {
        thrown.expect(IllegalArgumentException.class);
        new Vector3f(1.5f, 2.3f, 4.7f).set(5, 1.5f);
    }

    @Test
    public void testSet() {
        Vector3f target = new Vector3f(0.0f, 0.0f, 0.0f);
        target.set(0, 5.0f);
        assertEquals(target.x, 5.0, 0.0f);

        target = new Vector3f(0.0f, 0.0f, 0.0f);
        target.set(1, 3.0f);
        assertEquals(target.y, 3.0, 0.0f);

        target = new Vector3f(0.0f, 0.0f, 0.0f);
        target.set(2, 8.0f);
        assertEquals(target.z, 8.0, 0.0f);
    }

    @Test
    public void testSetX() {
        final Vector3f retval = new Vector3f(0.0f, 0.0f, 0.0f).setX(3.0f);

        assertNotNull(retval);
        assertEquals(3.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testSetY() {
        final Vector3f retval = new Vector3f(0.0f, 0.0f, 0.0f).setY(3.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(3.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }

    @Test
    public void testSetZ() {
        final Vector3f retval = new Vector3f(0.0f, 0.0f, 0.0f).setZ(3.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(3.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtract() {
        final Vector3f retval = new Vector3f(12.0f, 8.0f, 5.0f).subtract(new Vector3f(7.0f, 4.0f, -2.0f));

        assertNotNull(retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtract2() {
        final Vector3f target = new Vector3f(12.0f, 8.0f, 5.0f);
        final Vector3f other = new Vector3f();
        final Vector3f retval = target.subtract(new Vector3f(7.0f, 4.0f, -2.0f), other);

        assertEquals(other, retval);

        assertNotNull(retval);
        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtract3() {
        final Vector3f other = null;
        final Vector3f target = new Vector3f(12.0f, 8.0f, 5.0f);
        final Vector3f retval = target.subtract(new Vector3f(7.0f, 4.0f, -2.0f), other);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtract4() {
        final Vector3f target = new Vector3f(12.0f, 8.0f, 5.0f);
        final Vector3f retval = target.subtract(7.0f, 4.0f, -2.0f);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtractLocal() {
        final Vector3f target = new Vector3f(12.0f, 8.0f, 5.0f);
        final Vector3f retval = target.subtractLocal(new Vector3f(7.0f, 4.0f, -2.0f));

        assertEquals(target, retval);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtractLocal2() {
        final Vector3f target = new Vector3f(12.0f, 8.0f, 5.0f);
        final Vector3f retval = target.subtractLocal(7.0f, 4.0f, -2.0f);

        assertEquals(target, retval);

        assertEquals(5.0f, retval.x, 0.0f);
        assertEquals(4.0f, retval.y, 0.0f);
        assertEquals(7.0f, retval.z, 0.0f);
    }

    @Test
    public void testSubtractLocal3() {
        final Vector3f target = new Vector3f(12.0f, 8.0f, 5.0f);
        final Vector3f retval = target.subtractLocal(null);

        assertNull(retval);
    }

    @Test
    public void testToArray() {
        final float[] store = {0.0f, 0.0f, 0.0f};
        final float[] retval = new Vector3f(1.0f, 2.0f, 3.0f).toArray(store);
        assertEquals(store, retval);
        assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, retval, 0.0f);

        final float[] retval2 = new Vector3f(1.0f, 2.0f, 3.0f).toArray(new float[]{4.0f, 5.0f, 6.0f});
        assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, retval2, 0.0f);

        final float[] retval3 = new Vector3f(1.0f, 2.0f, 3.0f).toArray(null);
        assertArrayEquals(new float[] {1.0f, 2.0f, 3.0f}, retval3, 0.0f);
    }

    @Test
    public void testZero() {
        final Vector3f target = new Vector3f(1.0f, 5.0f, 9.0f);
        final Vector3f retval = target.zero();

        assertEquals(target, retval);

        assertNotNull(retval);
        assertEquals(0.0f, retval.x, 0.0f);
        assertEquals(0.0f, retval.y, 0.0f);
        assertEquals(0.0f, retval.z, 0.0f);
    }
}
