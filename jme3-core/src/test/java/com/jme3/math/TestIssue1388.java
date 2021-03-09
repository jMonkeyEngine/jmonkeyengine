/*
 * Copyright (c) 2020-2021 jMonkeyEngine
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
 * Verify the order in which Tait-Bryan angles are applied by the Quaternion
 * class. This was issue #1388 at GitHub.
 *
 * @author Stephen Gold
 */
public class TestIssue1388 {

    @Test
    public void testIssue1388() {
        Vector3f in = new Vector3f(4f, 6f, 9f); // test vector, never modified
        Vector3f saveIn = in.clone();
        /*
         * Three arbitrary rotation angles between -PI/2 and +PI/2
         */
        final float xAngle = 1.23f;
        final float yAngle = 0.765f;
        final float zAngle = -0.456f;
        float[] angles = new float[]{xAngle, yAngle, zAngle};
        float[] saveAngles = new float[]{xAngle, yAngle, zAngle};
        /*
         * Part 1: verify that the extrinsic rotation order is x-z-y
         *
         * Apply extrinsic rotations to the "in" vector in x-z-y order.
         */
        Quaternion qx = new Quaternion().fromAngleAxis(xAngle, Vector3f.UNIT_X);
        Quaternion qy = new Quaternion().fromAngleAxis(yAngle, Vector3f.UNIT_Y);
        Quaternion qz = new Quaternion().fromAngleAxis(zAngle, Vector3f.UNIT_Z);
        Vector3f outXZY = qx.mult(in);
        qz.mult(outXZY, outXZY);
        qy.mult(outXZY, outXZY);
        /*
         * Construct a Quaternion using fromAngles(float, float, float),
         * use it to rotate the "in" vector, and compare.
         */
        Quaternion q1 = new Quaternion().fromAngles(xAngle, yAngle, zAngle);
        Vector3f out1 = q1.mult(in);
        assertEquals(outXZY, out1, 1e-5f);
        /*
         * Construct a Quaternion using fromAngles(float[]),
         * use it to rotate the "in" vector, and compare.
         */
        Quaternion q2 = new Quaternion().fromAngles(angles);
        Vector3f out2 = q2.mult(in);
        assertEquals(outXZY, out2, 1e-5f);
        /*
         * Construct a Quaternion using only the constructor,
         * use it to rotate the "in" vector, and compare.
         */
        Quaternion q3 = new Quaternion(angles);
        Vector3f out3 = q3.mult(in);
        assertEquals(outXZY, out3, 1e-5f);
        /*
         * Verify that fromAngles() reverses toAngles() for the chosen angles.
         */
        float[] out4 = q1.toAngles(null);
        assertEquals(angles, out4, 1e-5f);
        float[] out5 = q2.toAngles(null);
        assertEquals(angles, out5, 1e-5f);
        float[] out6 = q3.toAngles(null);
        assertEquals(angles, out6, 1e-5f);
        /*
         * Part 2: verify intrinsic rotation order
         *
         * Apply intrinsic rotations to the "in" vector in y-z'-x" order.
         */
        Quaternion q4 = qy.mult(qz).mult(qx);
        Vector3f out7 = q4.mult(in);
        assertEquals(outXZY, out7, 1e-5f);
        /*
         * Verify that the values of "saveAngles" and "in" haven't changed.
         */
        assertEquals(saveAngles, angles, 0f);
        assertEquals(saveIn, in, 0f);
    }

    private void assertEquals(float[] expected, float[] actual,
            float tolerance) {
        Assert.assertEquals(expected[0], actual[0], tolerance);
        Assert.assertEquals(expected[1], actual[1], tolerance);
        Assert.assertEquals(expected[2], actual[2], tolerance);
    }

    private void assertEquals(Vector3f expected, Vector3f actual,
            float tolerance) {
        Assert.assertEquals(expected.x, actual.x, tolerance);
        Assert.assertEquals(expected.y, actual.y, tolerance);
        Assert.assertEquals(expected.z, actual.z, tolerance);
    }
}
