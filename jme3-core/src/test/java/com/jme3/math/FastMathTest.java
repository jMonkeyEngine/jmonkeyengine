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
import org.junit.Ignore;

import java.lang.Math;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * Verifies that algorithms in {@link FastMath} are working correctly.
 * 
 * @author Kirill Vainer
 */
public class FastMathTest {

    @Rule public ExpectedException thrown = ExpectedException.none();
    
    private int nearestPowerOfTwoSlow(int number) {
        return (int) Math.pow(2, Math.ceil(Math.log(number) / Math.log(2)));
    }
    
    @Test
    public void testNearestPowerOfTwo() {
        for (int i = -100; i < 1; i++) {
            assert FastMath.nearestPowerOfTwo(i) == 1;
        }
        for (int i = 1; i < 10000; i++) {
            int nextPowerOf2 = FastMath.nearestPowerOfTwo(i);
            assert i <= nextPowerOf2;
            assert FastMath.isPowerOfTwo(nextPowerOf2);
            assert nextPowerOf2 == nearestPowerOfTwoSlow(i);
        }
    }
    
    private static int fastCounterClockwise(Vector2f p0, Vector2f p1, Vector2f p2) {
        float result = (p1.x - p0.x) * (p2.y - p1.y) - (p1.y - p0.y) * (p2.x - p1.x);
        return (int) Math.signum(result);
    }
    
    private static Vector2f randomVector() {
        return new Vector2f(FastMath.nextRandomFloat(),
                            FastMath.nextRandomFloat());
    }

    @Ignore
    @Test
    public void testCounterClockwise() {
        for (int i = 0; i < 100; i++) {
            Vector2f p0 = randomVector();
            Vector2f p1 = randomVector();
            Vector2f p2 = randomVector();

            int fastResult = fastCounterClockwise(p0, p1, p2);
            int slowResult = FastMath.counterClockwise(p0, p1, p2);
            
            assert fastResult == slowResult;
        }
        
        // duplicate test
        Vector2f p0 = new Vector2f(0,0);
        Vector2f p1 = new Vector2f(0,0);
        Vector2f p2 = new Vector2f(0,1);
        
        int fastResult = fastCounterClockwise(p0, p1, p2);
        int slowResult = FastMath.counterClockwise(p0, p1, p2);
        
        assertEquals(slowResult, fastResult);
    }

    @Test
    public void testAcos() {
        assertEquals((float)Math.PI, FastMath.acos(-2.0f), 0.01f);
        assertEquals(0.0f, FastMath.acos(2.0f), 0.0f);
        assertEquals(1.57f, FastMath.acos(0.0f), 0.01f);
        assertEquals(1.047f, FastMath.acos(0.5f), 0.01f);
        assertEquals(0.0f, FastMath.acos(Float.POSITIVE_INFINITY), 0.0f);
        assertEquals(0x1.921fb6p+1f, FastMath.acos(-0x1p+113f), 0.01f);
    }

    @Test
    public void testApproximateEquals() {
        assertTrue(FastMath.approximateEquals(1000.0f, 1000.0f));
        assertTrue(FastMath.approximateEquals(100000.0f, 100001.0f));
        assertTrue(FastMath.approximateEquals(0.0f, -0.0f));

        assertFalse(FastMath.approximateEquals(10000.0f, 10001.0f));
        assertFalse(FastMath.approximateEquals(149.0f, 0.0f));
    }

    @Test
    public void testAsin() {
        final float HALF_PI = 0.5f * (float)Math.PI;

        assertEquals(-HALF_PI, FastMath.asin(-2.0f), 0.01f);
        assertEquals(HALF_PI, FastMath.asin(2.0f), 0.01f);
        assertEquals(HALF_PI, FastMath.asin(Float.POSITIVE_INFINITY), 0.0f);
        assertEquals(0.0f, FastMath.asin(0.0f), 0.0f);
        assertEquals(0.523f, FastMath.asin(0.5f), 0.01f);
        assertEquals(-1.570f, FastMath.asin(-0x1p+113f), 0.01f);
    }

    @Test
    public void testAtan() {
        assertEquals(0.0f, FastMath.atan2(0.0f, 0.0f), 0.0f);
        assertEquals(0.076f, FastMath.atan2(1.0f, 13.0f), 0.01f);
    }

    @Test
    public void testCartesianToSpherical() {
        final Vector3f cartCoords = new Vector3f(1.1f, 5.8f, 8.1f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.cartesianToSpherical(cartCoords, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(10.022974f, store.getX(), 0.0f);
        assertEquals(1.4358196f, store.getY(), 0.01f);
        assertEquals(0.61709767f, store.getZ(), 0.0f);

        assertNotNull(retval);
        assertEquals(10.022974f, retval.getX(), 0.0f);
        assertEquals(1.4358196f, retval.getY(), 0.01f);
        assertEquals(0.61709767f, retval.getZ(), 0.0f);
        /*
         * ensure that the transformation is reversible in Octant I
         */
        final Vector3f out1 = FastMath.sphericalToCartesian(retval, null);
        assertEquals(cartCoords.x, out1.x, 1e-5f);
        assertEquals(cartCoords.y, out1.y, 1e-5f);
        assertEquals(cartCoords.z, out1.z, 1e-5f);
        /*
         * test reversibility in the other 7 octants
         */
        final Vector3f in2 = new Vector3f(-1.9f, +5.8f, +8.1f);
        final Vector3f spherical2 = FastMath.cartesianToSpherical(in2, null);
        final Vector3f out2 = FastMath.sphericalToCartesian(spherical2, null);
        assertEquals(in2.x, out2.x, 1e-5f);
        assertEquals(in2.y, out2.y, 1e-5f);
        assertEquals(in2.z, out2.z, 1e-5f);

        final Vector3f in3 = new Vector3f(+1.7f, -3.8f, +8.6f);
        final Vector3f spherical3 = FastMath.cartesianToSpherical(in3, null);
        final Vector3f out3 = FastMath.sphericalToCartesian(spherical3, null);
        assertEquals(in3.x, out3.x, 1e-5f);
        assertEquals(in3.y, out3.y, 1e-5f);
        assertEquals(in3.z, out3.z, 1e-5f);

        final Vector3f in4 = new Vector3f(-1.5f, -3.2f, +4.1f);
        final Vector3f spherical4 = FastMath.cartesianToSpherical(in4, null);
        final Vector3f out4 = FastMath.sphericalToCartesian(spherical4, null);
        assertEquals(in4.x, out4.x, 1e-5f);
        assertEquals(in4.y, out4.y, 1e-5f);
        assertEquals(in4.z, out4.z, 1e-5f);

        final Vector3f in5 = new Vector3f(+3.5f, +7.2f, -4.3f);
        final Vector3f spherical5 = FastMath.cartesianToSpherical(in5, null);
        final Vector3f out5 = FastMath.sphericalToCartesian(spherical5, null);
        assertEquals(in5.x, out5.x, 1e-5f);
        assertEquals(in5.y, out5.y, 1e-5f);
        assertEquals(in5.z, out5.z, 1e-5f);

        final Vector3f in6 = new Vector3f(-6.9f, +5.8f, -2.1f);
        final Vector3f spherical6 = FastMath.cartesianToSpherical(in6, null);
        final Vector3f out6 = FastMath.sphericalToCartesian(spherical6, null);
        assertEquals(in6.x, out6.x, 1e-5f);
        assertEquals(in6.y, out6.y, 1e-5f);
        assertEquals(in6.z, out6.z, 1e-5f);

        final Vector3f in7 = new Vector3f(+1.1f, -3.0f, -8.6f);
        final Vector3f spherical7 = FastMath.cartesianToSpherical(in7, null);
        final Vector3f out7 = FastMath.sphericalToCartesian(spherical7, null);
        assertEquals(in7.x, out7.x, 1e-5f);
        assertEquals(in7.y, out7.y, 1e-5f);
        assertEquals(in7.z, out7.z, 1e-5f);

        final Vector3f in8 = new Vector3f(-6.2f, -2.2f, -4.1f);
        final Vector3f spherical8 = FastMath.cartesianToSpherical(in8, null);
        final Vector3f out8 = FastMath.sphericalToCartesian(spherical8, null);
        assertEquals(in8.x, out8.x, 1e-5f);
        assertEquals(in8.y, out8.y, 1e-5f);
        assertEquals(in8.z, out8.z, 1e-5f);
        /*
         * test reversibility on the origin
         */
        final Vector3f in0 = new Vector3f(0f, 0f, 0f);
        final Vector3f spherical0 = FastMath.cartesianToSpherical(in0, null);
        final Vector3f out0 = FastMath.sphericalToCartesian(spherical0, null);
        assertEquals(in0.x, out0.x, 1e-5f);
        assertEquals(in0.y, out0.y, 1e-5f);
        assertEquals(in0.z, out0.z, 1e-5f);
    }

    @Test
    public void testCartesianZToSpherical() {
        final Vector3f cartCoords = new Vector3f(1.1f, 5.8f, 8.1f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.cartesianZToSpherical(cartCoords, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(10.022974f, store.getX(), 0.01f);
        assertEquals(0.61709767f, store.getY(), 0.01f);
        assertEquals(1.4358196f, store.getZ(), 0.01f);

        assertNotNull(retval);
        assertEquals(10.022974f, retval.getX(), 0.01f);
        assertEquals(0.61709767f, retval.getY(), 0.01f);
        assertEquals(1.4358196f, retval.getZ(), 0.01f);
        /*
         * ensure that the transformation is reversible in Octant I
         */
        final Vector3f out1 = FastMath.sphericalToCartesianZ(retval, null);
        assertEquals(cartCoords.x, out1.x, 1e-5f);
        assertEquals(cartCoords.y, out1.y, 1e-5f);
        assertEquals(cartCoords.z, out1.z, 1e-5f);
        /*
         * test reversibility in the other 7 octants
         */
        final Vector3f in2 = new Vector3f(-1.9f, +5.8f, +8.1f);
        final Vector3f spherical2 = FastMath.cartesianZToSpherical(in2, null);
        final Vector3f out2 = FastMath.sphericalToCartesianZ(spherical2, null);
        assertEquals(in2.x, out2.x, 1e-5f);
        assertEquals(in2.y, out2.y, 1e-5f);
        assertEquals(in2.z, out2.z, 1e-5f);

        final Vector3f in3 = new Vector3f(+1.7f, -3.8f, +8.6f);
        final Vector3f spherical3 = FastMath.cartesianZToSpherical(in3, null);
        final Vector3f out3 = FastMath.sphericalToCartesianZ(spherical3, null);
        assertEquals(in3.x, out3.x, 1e-5f);
        assertEquals(in3.y, out3.y, 1e-5f);
        assertEquals(in3.z, out3.z, 1e-5f);

        final Vector3f in4 = new Vector3f(-1.5f, -3.2f, +4.1f);
        final Vector3f spherical4 = FastMath.cartesianZToSpherical(in4, null);
        final Vector3f out4 = FastMath.sphericalToCartesianZ(spherical4, null);
        assertEquals(in4.x, out4.x, 1e-5f);
        assertEquals(in4.y, out4.y, 1e-5f);
        assertEquals(in4.z, out4.z, 1e-5f);

        final Vector3f in5 = new Vector3f(+3.5f, +7.2f, -4.3f);
        final Vector3f spherical5 = FastMath.cartesianZToSpherical(in5, null);
        final Vector3f out5 = FastMath.sphericalToCartesianZ(spherical5, null);
        assertEquals(in5.x, out5.x, 1e-5f);
        assertEquals(in5.y, out5.y, 1e-5f);
        assertEquals(in5.z, out5.z, 1e-5f);

        final Vector3f in6 = new Vector3f(-6.9f, +5.8f, -2.1f);
        final Vector3f spherical6 = FastMath.cartesianZToSpherical(in6, null);
        final Vector3f out6 = FastMath.sphericalToCartesianZ(spherical6, null);
        assertEquals(in6.x, out6.x, 1e-5f);
        assertEquals(in6.y, out6.y, 1e-5f);
        assertEquals(in6.z, out6.z, 1e-5f);

        final Vector3f in7 = new Vector3f(+1.1f, -3.0f, -8.6f);
        final Vector3f spherical7 = FastMath.cartesianZToSpherical(in7, null);
        final Vector3f out7 = FastMath.sphericalToCartesianZ(spherical7, null);
        assertEquals(in7.x, out7.x, 1e-5f);
        assertEquals(in7.y, out7.y, 1e-5f);
        assertEquals(in7.z, out7.z, 1e-5f);

        final Vector3f in8 = new Vector3f(-6.2f, -2.2f, -4.1f);
        final Vector3f spherical8 = FastMath.cartesianZToSpherical(in8, null);
        final Vector3f out8 = FastMath.sphericalToCartesianZ(spherical8, null);
        assertEquals(in8.x, out8.x, 1e-5f);
        assertEquals(in8.y, out8.y, 1e-5f);
        assertEquals(in8.z, out8.z, 1e-5f);
        /*
         * test reversibility on the origin
         */
        final Vector3f in0 = new Vector3f(0f, 0f, 0f);
        final Vector3f spherical0 = FastMath.cartesianZToSpherical(in0, null);
        final Vector3f out0 = FastMath.sphericalToCartesianZ(spherical0, null);
        assertEquals(in0.x, out0.x, 1e-5f);
        assertEquals(in0.y, out0.y, 1e-5f);
        assertEquals(in0.z, out0.z, 1e-5f);
    }

    @Test
    public void testComputeNormal() {
        final Vector3f v1 = new Vector3f(1.1f, 9.5f, -7.2f);
        final Vector3f v2 = new Vector3f(Float.NaN, -0.2f, 6.1f);
        final Vector3f v3 = new Vector3f(-0.5f, -0.14f, -1.8f);

        final Vector3f retval = FastMath.computeNormal(v1, v2, v3);

        assertNotNull(retval);
        assertEquals(Float.NaN, retval.getX(), 0.0f);
        assertEquals(Float.NaN, retval.getY(), 0.0f);
        assertEquals(Float.NaN, retval.getZ(), 0.0f);
    }

    @Test
    public void testComputeNormal2() {
        final Vector3f v1 = new Vector3f(-0.4f, 0.1f, 2.9f);
        final Vector3f v2 = new Vector3f(-0.4f, 0.1f, 2.9f);
        final Vector3f v3 = new Vector3f(-1.4f, 10.1f, 2.9f);

        final Vector3f retval = FastMath.computeNormal(v1, v2, v3);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testConvertFloatToHalfUnsupportedOperationException() {
        thrown.expect(UnsupportedOperationException.class);
        FastMath.convertFloatToHalf(Float.NaN);
    }

    @Test
    public void testConvertFloatToHalf() {
        assertEquals((short)-1024, FastMath.convertFloatToHalf(Float.NEGATIVE_INFINITY));
        assertEquals((short)31744, FastMath.convertFloatToHalf(Float.POSITIVE_INFINITY));
        assertEquals((short)-1025, FastMath.convertFloatToHalf(-131328.0f));
        assertEquals((short)-32767, FastMath.convertFloatToHalf(-0x1p-135f));
        assertEquals((short)1, FastMath.convertFloatToHalf(0x1.008p-71f));
        assertEquals((short)31743, FastMath.convertFloatToHalf(0x1.008p+121f));
        assertEquals((short)0, FastMath.convertFloatToHalf(0.0f));
    }

    @Test
    public void testConvertHalfToFloat() {
        assertEquals(Float.POSITIVE_INFINITY, FastMath.convertHalfToFloat((short)31744), 0.0f);
        assertEquals(0.0f, FastMath.convertHalfToFloat((short)0), 0.0f);
        assertEquals(65504.0f, FastMath.convertHalfToFloat((short)31743), 0.0f);
        assertEquals(-65536.0f, FastMath.convertHalfToFloat((short)-1024), 0.0f);
        assertEquals(-65504.0f, FastMath.convertHalfToFloat((short)-1025), 0.0f);
    }

    @Test
    public void testCopysign() {
        assertEquals(-3.85186e-34, FastMath.copysign(-3.85186e-34f, -1.0f), 0.01f);
        assertEquals(0.0f, FastMath.copysign(0.0f, Float.NaN), 0.0f);
        assertEquals(Float.NaN, FastMath.copysign(Float.NaN, 1.0f), 0.0f);
        assertEquals(0.0f, FastMath.copysign(-0.0f, -1.0f), 0.0f);
        assertEquals(0.0f, FastMath.copysign(-0.0f, 0.0f), 0.0f);
        assertEquals(-1.0f, FastMath.copysign(1.0f, -3.0f), 0.0f);
    }

    @Test
    public void testCounterClockwise2() {
        final Vector2f p0 = new Vector2f(0.125f, -2.14644e+09f);
        final Vector2f p1 = new Vector2f(-6.375f, -3.96141e+28f);
        final Vector2f p2 = new Vector2f(0.078125f, -2.14644e+09f);

        assertEquals(-1, FastMath.counterClockwise(p0, p1, p2));
    }

    @Test
    public void testCounterClockwise3() {
        final Vector2f p0 = new Vector2f(2.34982e-38f, 1.25063e+27f);
        final Vector2f p1 = new Vector2f(2.34982e-38f, 1.25061e+27f);
        final Vector2f p2 = new Vector2f(3.51844e+13f, Float.NaN);

        assertEquals(0, FastMath.counterClockwise(p0, p1, p2));
    }

    @Test
    public void testCounterClockwise4() {
        final Vector2f p0 = new Vector2f(262143.0f, 4.55504e-38f);
        final Vector2f p1 = new Vector2f(262144.0f, 2.50444f);
        final Vector2f p2 = new Vector2f(204349.0f, 4.77259e-38f);

        assertEquals(1, FastMath.counterClockwise(p0, p1, p2));
    }

    @Test
    public void testCounterClockwise5() {
        final Vector2f p0 = new Vector2f(-1.87985e-37f, -1.16631e-38f);
        final Vector2f p1 = new Vector2f(-1.87978e-37f, -1.18154e-38f);
        final Vector2f p2 = new Vector2f(-6.56451e-21f, -1.40453e-38f);

        assertEquals(1, FastMath.counterClockwise(p0, p1, p2));
    }

    @Test
    public void testCounterClockwise6() {
        final Vector2f p0 = new Vector2f(1.07374e+09f, 1.07374e+09f);
        final Vector2f p1 = new Vector2f(1.07374e+09f, 1.07374e+09f);
        final Vector2f p2 = new Vector2f(1.07374e+09f, 1.07374e+09f);

        assertEquals(0, FastMath.counterClockwise(p0, p1, p2));
    }


    @Test
    public void testDeterminant() {
        assertEquals(20.0f, FastMath.determinant(
                5.0,  -7.0,  2.0,  2.0,
                0.0,   3.0,  0.0, -4.0,
               -5.0,  -8.0,  0.0,  3.0,
                0.0,   5.0,  0.0, -6.0), 0.0f);

        assertEquals(0.0f, FastMath.determinant(
                1.0,  2.0,  3.0,  4.0,
                5.0,  6.0,  7.0,  8.0,
                9.0,  10.0, 11.0, 12.0,
                13.0, 14.0, 15.0, 16.0), 0.0f);
    }

    @Test
    public void testExtrapolateLinear() {
        final float scale = 0.0f;
        final Vector3f startValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f endValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.extrapolateLinear(scale, startValue, endValue, store);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testExtrapolateLinear2() {
        final float scale = 0.6f;
        final Vector3f startValue = new Vector3f(1.6f, 3.1f, 2.2f);
        final Vector3f endValue = new Vector3f(0.5f, 1.2f, 4.9f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.extrapolateLinear(scale, startValue, endValue, store);

        assertNotNull(retval);
        assertEquals(0.94f, retval.getX(), 0.01f);
        assertEquals(1.95f, retval.getY(), 0.01f);
        assertEquals(3.82f, retval.getZ(), 0.01f);
    }

    @Test
    public void testExtrapolateLinearNoStore() {
        final float scale = 0.0f;
        final Vector3f startValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f endValue = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.extrapolateLinear(scale, startValue, endValue);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testExtrapolateLinearNoStore2() {
        final float scale = 0.6f;
        final Vector3f startValue = new Vector3f(1.6f, 3.1f, 2.2f);
        final Vector3f endValue = new Vector3f(0.5f, 1.2f, 4.9f);

        final Vector3f retval = FastMath.extrapolateLinear(scale, startValue, endValue);

        assertNotNull(retval);
        assertEquals(0.94f, retval.getX(), 0.01f);
        assertEquals(1.95f, retval.getY(), 0.01f);
        assertEquals(3.82f, retval.getZ(), 0.01f);
    }

    @Test
    public void testInterpolateBezier() {
        final float u = 0.0f;
        final Vector3f p0 = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f p1 = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f p2 = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f p3 = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.interpolateBezier(u, p0, p1, p2, p3, store);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateBezier2() {
        final float u = 0.5f;
        final Vector3f p0 = new Vector3f(1.0f, 2.0f, 3.0f);
        final Vector3f p1 = new Vector3f(6.0f, 7.0f, 8.0f);
        final Vector3f p2 = new Vector3f(2.0f, 3.0f, 4.0f);
        final Vector3f p3 = new Vector3f(0.0f, 1.0f, 8.0f);
        final Vector3f store = new Vector3f(1.0f, 2.0f, 3.0f);

        final Vector3f retval = FastMath.interpolateBezier(u, p0, p1, p2, p3, store);

        assertNotNull(retval);
        assertEquals(3.125f, retval.getX(), 0.0f);
        assertEquals(4.125f, retval.getY(), 0.0f);
        assertEquals(5.875f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateBezierNoStore() {
        final float u = 0.5f;
        final Vector3f p0 = new Vector3f(1.0f, 2.0f, 3.0f);
        final Vector3f p1 = new Vector3f(6.0f, 7.0f, 8.0f);
        final Vector3f p2 = new Vector3f(2.0f, 3.0f, 4.0f);
        final Vector3f p3 = new Vector3f(0.0f, 1.0f, 8.0f);

        final Vector3f retval = FastMath.interpolateBezier(u, p0, p1, p2, p3);

        assertNotNull(retval);
        assertEquals(3.125f, retval.getX(), 0.0f);
        assertEquals(4.125f, retval.getY(), 0.0f);
        assertEquals(5.875f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateCatmullRom() {
        final float u = 0.5f;
        final float T = 0.5f;
        final Vector3f p0 = new Vector3f(1.0f, 2.0f, 3.0f);
        final Vector3f p1 = new Vector3f(6.0f, 7.0f, 8.0f);
        final Vector3f p2 = new Vector3f(2.0f, 3.0f, 4.0f);
        final Vector3f p3 = new Vector3f(0.0f, 1.0f, 8.0f);

        final Vector3f retval = FastMath.interpolateCatmullRom(u, T, p0, p1, p2, p3);

        assertNotNull(retval);
        assertEquals(4.4375f, retval.getX(), 0.0f);
        assertEquals(5.4375f, retval.getY(), 0.0f);
        assertEquals(6.0625f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear() {
        final float scale = -1.00195f;
        final Vector3f startValue = new Vector3f(32.0f, 0.0f, 0.0f);
        final Vector3f endValue = new Vector3f(32.0f, Float.POSITIVE_INFINITY, -0.0f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue);

        assertNotNull(retval);
        assertEquals(32.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear2() {
        final float scale = 1.0842e-19f;
        final Vector3f startValue = new Vector3f(0.0f, 0.0f, 1.4013e-45f);
        final Vector3f endValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(0.0f, store.getX(), 0.0f);
        assertEquals(0.0f, store.getY(), 0.0f);
        assertEquals(1.4013e-45f, store.getZ(), 0.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(1.4013e-45f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear3() {
        final float scale = 1.03125f;
        final Vector3f startValue = new Vector3f(0.0f, 16.0f, Float.NaN);
        final Vector3f endValue = new Vector3f(0.0f, 16.0f, Float.POSITIVE_INFINITY);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(0.0f, store.getX(), 0.0f);
        assertEquals(16.0f, store.getY(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, store.getZ(), 0.0f);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(16.0f, retval.getY(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear4() {
        final float scale = 1.00195f;
        final Vector3f startValue = new Vector3f(256.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        final Vector3f endValue = new Vector3f(0.0f, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.getY(), 0.0f);
        assertEquals(Float.POSITIVE_INFINITY, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear5() {
        final float scale = 0.309184f;
        final Vector3f startValue = new Vector3f(-53.1157f, 0.0f, 1.23634f);
        final Vector3f endValue = new Vector3f(-1.98571f, Float.POSITIVE_INFINITY, 3.67342e-40f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue);

        assertNotNull(retval);
        assertEquals(-37.3071f, retval.getX(), 0.01f);
        assertEquals(Float.POSITIVE_INFINITY, retval.getY(), 0.0f);
        assertEquals(0.854082f, retval.getZ(), 0.01f);
    }

    @Test
    public void testInterpolateLinear6() {
        final float scale = 0.0f;
        final Vector3f startValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f endValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue, store);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear7() {
        final float scale = 0.0f;
        final Vector3f startValue = new Vector3f(1.4013e-45f, 0.0f, 0.0f);
        final Vector3f endValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(1.4013e-45f, store.getX(), 0.0f);
        assertEquals(0.0f, store.getY(), 0.0f);
        assertEquals(0.0f, store.getZ(), 0.0f);

        assertNotNull(retval);
        assertEquals(1.4013e-45f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear8() {
        final float scale = 0.0f;
        final Vector3f startValue = new Vector3f(0.0f, 0.0f, 0.0f);
        final Vector3f endValue = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.interpolateLinear(scale, startValue, endValue);

        assertNotNull(retval);
        assertEquals(0.0f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testInterpolateLinear_float() {
        assertEquals(0.0f, FastMath.interpolateLinear(2.0f, 2.93874e-39f, 0.0f), 0.0f);
        assertEquals(0.0f, FastMath.interpolateLinear(0.999999f, 1.4013e-45f, 0.0f), 0.0f);
        assertEquals(-2.93874e-39f, FastMath.interpolateLinear(0.0f, -2.93874e-39f, -0.0f), 0.0f);
        assertEquals(0.0f, FastMath.interpolateLinear(0.0f, 0.0f, 0.0f), 0.0f);
    }

    @Test
    public void testNormalize() {
        assertEquals(0.0f, FastMath.normalize(Float.POSITIVE_INFINITY, 0.0f, 0.0f), 0.0f);
        assertEquals(0.0f, FastMath.normalize(Float.NaN, 0.0f, 0.0f), 0.0f);
        assertEquals(4.0f, FastMath.normalize(15.0f, 1.0f, 12.0f), 0.0f);
        assertEquals(15.0f, FastMath.normalize(15.0f, 1.0f, 16.0f), 0.0f);
        assertEquals(0.0f, FastMath.normalize(0.0f, 0.0f, 0.0f), 0.0f);
    }

    @Test
    public void testPointInsideTriangle() {
        final Vector2f t0 = new Vector2f(2.03f, -4.04f);
        final Vector2f t1 = new Vector2f(0.12f, 5.45f);
        final Vector2f t2 = new Vector2f(1.90f, 3.43f);
        final Vector2f p = new Vector2f(1.28f, 3.46f);

        assertEquals(-1, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle2() {
        final Vector2f t0 = new Vector2f(2.03f, 4.04f);
        final Vector2f t1 = new Vector2f(0.12f, -5.45f);
        final Vector2f t2 = new Vector2f(1.90f, -3.43f);
        final Vector2f p = new Vector2f(1.90f, -3.43f);

        assertEquals(1, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle3() {
        final Vector2f t0 = new Vector2f(-0.0f, 7.38f);
        final Vector2f t1 = new Vector2f(-1.0f, 1.70f);
        final Vector2f t2 = new Vector2f(Float.NaN, -4.18f);
        final Vector2f p = new Vector2f(-1.0f, -2.12f);

        assertEquals(1, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle4() {
        final Vector2f t0 = new Vector2f(4.82f, 1.35f);
        final Vector2f t1 = new Vector2f(-1.36f, Float.NaN);
        final Vector2f t2 = new Vector2f(-1.0f, 9.45f);
        final Vector2f p = new Vector2f(2.32f, Float.NaN);

        assertEquals(1, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle5() {
        final Vector2f t0 = new Vector2f(1.32f, 9.55f);
        final Vector2f t1 = new Vector2f(Float.NaN, -2.35f);
        final Vector2f t2 = new Vector2f(-5.42f, Float.NaN);
        final Vector2f p = new Vector2f(-7.20f, 8.81f);

        assertEquals(1, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle6() {
        final Vector2f t0 = new Vector2f(-0.43f, 2.54f);
        final Vector2f t1 = new Vector2f(Float.NEGATIVE_INFINITY, 2.54f);
        final Vector2f t2 = new Vector2f(Float.NaN, Float.POSITIVE_INFINITY);
        final Vector2f p = new Vector2f(-3.19f, -0.001f);

        assertEquals(0, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle7() {
        final Vector2f t0 = new Vector2f(-3.32f, 1.87f);
        final Vector2f t1 = new Vector2f(-2.72f, 1.87f);
        final Vector2f t2 = new Vector2f(-1.27f, 1.87f);
        final Vector2f p = new Vector2f(6.38f, 1.90f);

        assertEquals(0, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testPointInsideTriangle8() {
        final Vector2f t0 = new Vector2f(3.96f, -511.96f);
        final Vector2f t1 = new Vector2f(-5.36f, 1.27f);
        final Vector2f t2 = new Vector2f(1.56f, -7.84f);
        final Vector2f p = new Vector2f(5.06f, Float.NEGATIVE_INFINITY);

        assertEquals(0, FastMath.pointInsideTriangle(t0, t1, t2, p));
    }

    @Test
    public void testSaturate() {
        assertEquals(0.0f, FastMath.saturate(-2.0f), 0.0f);
        assertEquals(1.0f, FastMath.saturate(1.0f), 0.0f);
        assertEquals(1.0f, FastMath.saturate(7.5f), 0.0f);
        assertEquals(0.0f, FastMath.saturate(0.0f), 0.0f);
        assertEquals(0.5f, FastMath.saturate(0.5f), 0.0f);
        assertEquals(0.75f, FastMath.saturate(0.75f), 0.0f);
    }

    @Test
    public void testSign() {
        assertEquals(-1, FastMath.sign(-2_147_483_647));
        assertEquals(1, FastMath.sign(1));
        assertEquals(0, FastMath.sign(0));
        assertEquals(0.0f, FastMath.sign(0.0f), 0.0f);
    }

    @Test
    public void testSphericalToCartesian() {
        final Vector3f sphereCoords = new Vector3f(4.29497e+09f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.sphericalToCartesian(sphereCoords, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(4294969900f, store.getX(), 0.01f);
        assertEquals(0.0f, store.getY(), 0.0f);
        assertEquals(0.0f, store.getZ(), 0.0f);

        assertNotNull(retval);
        assertEquals(4294969900f, retval.getX(), 0.01f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }

    @Test
    public void testSphericalToCartesianZ() {
        final Vector3f sphereCoords = new Vector3f(4.29497e+09f, 0.0f, 0.0f);
        final Vector3f store = new Vector3f(0.0f, 0.0f, 0.0f);

        final Vector3f retval = FastMath.sphericalToCartesianZ(sphereCoords, store);

        assertEquals(store, retval);

        assertNotNull(store);
        assertEquals(4294969900f, store.getX(), 0.0f);
        assertEquals(0.0f, store.getY(), 0.0f);
        assertEquals(0.0f, store.getZ(), 0.0f);

        assertNotNull(retval);
        assertEquals(4294969900f, retval.getX(), 0.0f);
        assertEquals(0.0f, retval.getY(), 0.0f);
        assertEquals(0.0f, retval.getZ(), 0.0f);
    }
}
