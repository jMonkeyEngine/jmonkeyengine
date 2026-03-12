/*
 * Copyright (c) 2024 jMonkeyEngine
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

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link Quaternion} class works correctly.
 *
 * @author Richard Tingle (aka Richtea)
 */
public class QuaternionTest extends TestCase{

    /**
     * Verify that the {@link Quaternion#isValidQuaternion(com.jme3.math.Quaternion)} method works correctly. Testing
     * for NaNs and infinities (which are not "valid")
     */
    public void testIsValidQuaternion(){
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.NaN, 2.1f, 3.0f, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, Float.NaN, 3.0f, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, 2.1f, Float.NaN, 1.5f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(1f, 2.1f, 3.0f, Float.NaN)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.POSITIVE_INFINITY, 1.5f, 1.9f, 2.0f)));
        assertFalse(Quaternion.isValidQuaternion(new Quaternion(Float.NEGATIVE_INFINITY, 2.5f, 8.2f, 3.0f)));
        assertFalse(Quaternion.isValidQuaternion(null));

        assertTrue(Quaternion.isValidQuaternion(new Quaternion()));
        assertTrue(Quaternion.isValidQuaternion(new Quaternion(1.5f, -5.7f, 8.2f, 3.0f)));
    }

    private static final float TOL = 1e-5f;

    /** Default constructor must produce the identity quaternion (0,0,0,1). */
    @Test
    public void testIdentityQuaternion() {
        Quaternion q = new Quaternion();
        Assert.assertEquals(0f, q.getX(), TOL);
        Assert.assertEquals(0f, q.getY(), TOL);
        Assert.assertEquals(0f, q.getZ(), TOL);
        Assert.assertEquals(1f, q.getW(), TOL);
        Assert.assertEquals(1f, q.norm(), TOL);
        Assert.assertTrue(q.isIdentity());
    }

    /** fromAngleAxis(90°, Z) then toAngleAxis must round-trip to ~90° around ~(0,0,1). */
    @Test
    public void testFromAngleAxisAndBack() {
        Quaternion q = new Quaternion();
        q.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
        Vector3f axis = new Vector3f();
        float angle = q.toAngleAxis(axis);
        Assert.assertEquals(FastMath.HALF_PI, angle, TOL);
        Assert.assertEquals(0f, axis.x, TOL);
        Assert.assertEquals(0f, axis.y, TOL);
        Assert.assertEquals(1f, axis.z, TOL);
    }

    /** A normalised quaternion must have norm == 1. */
    @Test
    public void testNorm() {
        // For a unit quaternion, norm() (which returns the sum of squared components) == 1
        Quaternion unit = new Quaternion();
        Assert.assertEquals(1f, unit.norm(), TOL);

        // For (1,2,3,4), norm() = 1²+2²+3²+4² = 30 (squared norm)
        Quaternion q = new Quaternion(1f, 2f, 3f, 4f);
        Assert.assertEquals(30f, q.norm(), TOL);
    }

    /** After normalizeLocal() the norm must be 1. */
    @Test
    public void testNormalizeLocal() {
        Quaternion q = new Quaternion(1f, 2f, 3f, 4f);
        q.normalizeLocal();
        Assert.assertEquals(1f, q.norm(), TOL);
    }

    /** q * identity must equal q for any quaternion. */
    @Test
    public void testMultiplyIdentity() {
        Quaternion q = new Quaternion();
        q.fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y);
        Quaternion identity = new Quaternion();
        Quaternion result = q.mult(identity);
        Assert.assertEquals(q.getX(), result.getX(), TOL);
        Assert.assertEquals(q.getY(), result.getY(), TOL);
        Assert.assertEquals(q.getZ(), result.getZ(), TOL);
        Assert.assertEquals(q.getW(), result.getW(), TOL);
    }

    /** q * q^-1 must be the identity quaternion (for a unit quaternion). */
    @Test
    public void testMultiplyInverse() {
        Quaternion q = new Quaternion();
        q.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        Quaternion inv = q.inverse();
        Quaternion product = q.mult(inv);
        Assert.assertEquals(0f, product.getX(), TOL);
        Assert.assertEquals(0f, product.getY(), TOL);
        Assert.assertEquals(0f, product.getZ(), TOL);
        Assert.assertEquals(1f, product.getW(), TOL);
    }

    /**
     * Anti-homomorphism of the inverse: (q*p)^-1 == p^-1 * q^-1.
     * For unit quaternions the inverse equals the conjugate.
     */
    @Test
    public void testConjugate() {
        Quaternion q = new Quaternion();
        q.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
        Quaternion p = new Quaternion();
        p.fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_X);

        Quaternion qpInv  = q.mult(p).inverse();
        Quaternion pInvQInv = p.inverse().mult(q.inverse());

        Assert.assertEquals(pInvQInv.getX(), qpInv.getX(), TOL);
        Assert.assertEquals(pInvQInv.getY(), qpInv.getY(), TOL);
        Assert.assertEquals(pInvQInv.getZ(), qpInv.getZ(), TOL);
        Assert.assertEquals(pInvQInv.getW(), qpInv.getW(), TOL);
    }

    /**
     * fromAngles(pitch, yaw, roll) then toAngles() must reproduce the same
     * rotation when applied to a vector.
     */
    @Test
    public void testFromAnglesRoundTrip() {
        float pitch = 0.3f, yaw = 0.5f, roll = 0.7f;
        Quaternion q1 = new Quaternion().fromAngles(pitch, yaw, roll);
        float[] angles = q1.toAngles(null);
        Quaternion q2 = new Quaternion().fromAngles(angles);

        Vector3f v = new Vector3f(1f, 2f, 3f);
        Vector3f r1 = q1.mult(v);
        Vector3f r2 = q2.mult(v);
        Assert.assertEquals(r1.x, r2.x, 1e-4f);
        Assert.assertEquals(r1.y, r2.y, 1e-4f);
        Assert.assertEquals(r1.z, r2.z, 1e-4f);
    }

    /** slerp(q1, q2, 0) must equal q1. */
    @Test
    public void testSlerpAtZero() {
        Quaternion q1 = new Quaternion();
        q1.fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Z);
        Quaternion q2 = new Quaternion();
        q2.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);

        Quaternion result = new Quaternion().slerp(q1, q2, 0f);
        Assert.assertEquals(q1.getX(), result.getX(), TOL);
        Assert.assertEquals(q1.getY(), result.getY(), TOL);
        Assert.assertEquals(q1.getZ(), result.getZ(), TOL);
        Assert.assertEquals(q1.getW(), result.getW(), TOL);
    }

    /** slerp(q1, q2, 1) must equal q2. */
    @Test
    public void testSlerpAtOne() {
        Quaternion q1 = new Quaternion();
        q1.fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Z);
        Quaternion q2 = new Quaternion();
        q2.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);

        Quaternion result = new Quaternion().slerp(q1, q2, 1f);
        Assert.assertEquals(q2.getX(), result.getX(), TOL);
        Assert.assertEquals(q2.getY(), result.getY(), TOL);
        Assert.assertEquals(q2.getZ(), result.getZ(), TOL);
        Assert.assertEquals(q2.getW(), result.getW(), TOL);
    }

    /**
     * slerp(identity, 90°-around-Z, 0.5) must produce a 45° rotation
     * around Z.  Verified by rotating (1,0,0) and checking (cos45°, sin45°, 0).
     */
    @Test
    public void testSlerpAtHalf() {
        Quaternion q1 = new Quaternion(); // identity
        Quaternion q2 = new Quaternion();
        q2.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z); // 90° around Z

        Quaternion half = new Quaternion().slerp(q1, q2, 0.5f);
        Vector3f rotated = half.mult(new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(FastMath.cos(FastMath.QUARTER_PI), rotated.x, TOL);
        Assert.assertEquals(FastMath.sin(FastMath.QUARTER_PI), rotated.y, TOL);
        Assert.assertEquals(0f, rotated.z, TOL);
    }

    /**
     * Converting a rotation matrix built from angle-axis to a Quaternion must
     * yield the same vector transform as the original matrix.
     */
    @Test
    public void testFromRotationMatrixRoundTrip() {
        Matrix3f mat = new Matrix3f();
        mat.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);
        Quaternion q = new Quaternion();
        q.fromRotationMatrix(mat);
        Vector3f v = new Vector3f(1f, 0f, 0f);
        Vector3f fromMatrix = mat.mult(v);
        Vector3f fromQuat   = q.mult(v);
        Assert.assertEquals(fromMatrix.x, fromQuat.x, TOL);
        Assert.assertEquals(fromMatrix.y, fromQuat.y, TOL);
        Assert.assertEquals(fromMatrix.z, fromQuat.z, TOL);
    }

    /**
     * Rotating (1,0,0) by 90° around Y using multLocal(Vector3f) must give
     * approximately (0,0,-1).
     */
    @Test
    public void testRotateVector() {
        Quaternion q = new Quaternion();
        q.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        Vector3f v = new Vector3f(1f, 0f, 0f);
        q.multLocal(v);
        Assert.assertEquals(0f,  v.x, TOL);
        Assert.assertEquals(0f,  v.y, TOL);
        Assert.assertEquals(-1f, v.z, TOL);
    }

    /** Quaternion multiplication must be associative: (q1*q2)*q3 == q1*(q2*q3). */
    @Test
    public void testMultiplicationAssociativity() {
        Quaternion q1 = new Quaternion();
        q1.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_X);
        Quaternion q2 = new Quaternion();
        q2.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
        Quaternion q3 = new Quaternion();
        q3.fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Z);

        Quaternion lhs = q1.mult(q2).mult(q3);
        Quaternion rhs = q1.mult(q2.mult(q3));
        Assert.assertEquals(lhs.getX(), rhs.getX(), TOL);
        Assert.assertEquals(lhs.getY(), rhs.getY(), TOL);
        Assert.assertEquals(lhs.getZ(), rhs.getZ(), TOL);
        Assert.assertEquals(lhs.getW(), rhs.getW(), TOL);
    }

    /** q.add(q).subtract(q) must equal q (component-wise). */
    @Test
    public void testAddSubtract() {
        Quaternion q = new Quaternion(0.1f, 0.2f, 0.3f, 0.9f);
        Quaternion result = q.add(q).subtract(q);
        Assert.assertEquals(q.getX(), result.getX(), TOL);
        Assert.assertEquals(q.getY(), result.getY(), TOL);
        Assert.assertEquals(q.getZ(), result.getZ(), TOL);
        Assert.assertEquals(q.getW(), result.getW(), TOL);
    }
}