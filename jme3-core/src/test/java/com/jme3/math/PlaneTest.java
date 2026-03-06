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
 * Verifies that the {@link Plane} class works correctly.
 */
public class PlaneTest {

    private static final float TOLERANCE = 1e-6f;

    @Test
    public void testDefaultConstructor() {
        Plane p = new Plane();
        Assert.assertEquals(0f, p.getConstant(), 0f);
        Assert.assertEquals(0f, p.getNormal().x, 0f);
    }

    @Test
    public void testConstructorNormalConstant() {
        Vector3f normal = new Vector3f(0f, 1f, 0f);
        Plane p = new Plane(normal, 5f);
        Assert.assertEquals(5f, p.getConstant(), 0f);
        Assert.assertEquals(0f, p.getNormal().x, 0f);
        Assert.assertEquals(1f, p.getNormal().y, 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullNormalThrows() {
        new Plane(null, 1f);
    }

    @Test
    public void testConstructorNormalPoint() {
        Vector3f normal = new Vector3f(0f, 1f, 0f);
        Vector3f point = new Vector3f(0f, 3f, 0f);
        Plane p = new Plane(normal, point);
        // constant = normal.dot(point) = 3
        Assert.assertEquals(3f, p.getConstant(), TOLERANCE);
    }

    @Test
    public void testSetNormalVector() {
        Plane p = new Plane();
        p.setNormal(new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(1f, p.getNormal().x, 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetNullNormalThrows() {
        Plane p = new Plane();
        p.setNormal(null);
    }

    @Test
    public void testSetNormalComponents() {
        Plane p = new Plane();
        p.setNormal(0f, 0f, 1f);
        Assert.assertEquals(1f, p.getNormal().z, 0f);
    }

    @Test
    public void testSetConstant() {
        Plane p = new Plane();
        p.setConstant(7f);
        Assert.assertEquals(7f, p.getConstant(), 0f);
    }

    @Test
    public void testPseudoDistance() {
        // Plane y=5: normal=(0,1,0), constant=5
        Plane p = new Plane(new Vector3f(0f, 1f, 0f), 5f);
        // Point above: y=7 -> distance = 7-5 = 2
        Assert.assertEquals(2f, p.pseudoDistance(new Vector3f(0f, 7f, 0f)), TOLERANCE);
        // Point below: y=3 -> distance = 3-5 = -2
        Assert.assertEquals(-2f, p.pseudoDistance(new Vector3f(0f, 3f, 0f)), TOLERANCE);
        // On plane: y=5 -> distance = 0
        Assert.assertEquals(0f, p.pseudoDistance(new Vector3f(0f, 5f, 0f)), TOLERANCE);
    }

    @Test
    public void testWhichSide() {
        Plane p = new Plane(new Vector3f(0f, 1f, 0f), 5f);
        Assert.assertEquals(Plane.Side.Positive, p.whichSide(new Vector3f(0f, 7f, 0f)));
        Assert.assertEquals(Plane.Side.Negative, p.whichSide(new Vector3f(0f, 3f, 0f)));
        Assert.assertEquals(Plane.Side.None, p.whichSide(new Vector3f(0f, 5f, 0f)));
    }

    @Test
    public void testIsOnPlane() {
        Plane p = new Plane(new Vector3f(0f, 1f, 0f), 5f);
        Assert.assertTrue(p.isOnPlane(new Vector3f(0f, 5f, 0f)));
        Assert.assertFalse(p.isOnPlane(new Vector3f(0f, 6f, 0f)));
    }

    @Test
    public void testGetClosestPoint() {
        // Plane y=0: normal=(0,1,0), constant=0
        Plane p = new Plane(new Vector3f(0f, 1f, 0f), 0f);
        Vector3f point = new Vector3f(3f, 5f, 7f);
        Vector3f closest = p.getClosestPoint(point);
        // Closest point should project onto the plane (y=0)
        Assert.assertEquals(3f, closest.x, TOLERANCE);
        Assert.assertEquals(0f, closest.y, TOLERANCE);
        Assert.assertEquals(7f, closest.z, TOLERANCE);
    }

    @Test
    public void testReflect() {
        // Plane y=0: normal=(0,1,0)
        Plane p = new Plane(new Vector3f(0f, 1f, 0f), 0f);
        Vector3f point = new Vector3f(0f, 3f, 0f);
        Vector3f reflected = p.reflect(point, null);
        Assert.assertEquals(0f, reflected.x, TOLERANCE);
        Assert.assertEquals(-3f, reflected.y, TOLERANCE);
        Assert.assertEquals(0f, reflected.z, TOLERANCE);
    }

    @Test
    public void testSetPlanePoints() {
        Plane p = new Plane();
        p.setPlanePoints(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));
        // Normal should be (0,0,1), constant = 0
        Assert.assertEquals(0f, p.getNormal().x, TOLERANCE);
        Assert.assertEquals(0f, p.getNormal().y, TOLERANCE);
        Assert.assertEquals(1f, Math.abs(p.getNormal().z), TOLERANCE);
    }

    @Test
    public void testSetOriginNormal() {
        Vector3f origin = new Vector3f(0f, 5f, 0f);
        Vector3f normal = new Vector3f(0f, 1f, 0f);
        Plane p = new Plane();
        p.setOriginNormal(origin, normal);
        Assert.assertEquals(5f, p.getConstant(), TOLERANCE);
    }

    @Test
    public void testClone() {
        Plane original = new Plane(new Vector3f(0f, 1f, 0f), 5f);
        Plane cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getConstant(), cloned.getConstant(), 0f);
        Assert.assertEquals(original.getNormal(), cloned.getNormal());
    }

    @Test
    public void testToString() {
        Plane p = new Plane(new Vector3f(0f, 1f, 0f), 5f);
        String s = p.toString();
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("Plane"));
    }

    // -----------------------------------------------------------------------
    // Complex behavioral tests
    // -----------------------------------------------------------------------

    /**
     * Reflecting a point P across a plane yields P' such that
     * pseudoDistance(P') == −pseudoDistance(P).
     */
    @Test
    public void testReflectPreservesDistanceFromPlane() {
        Plane plane = new Plane(new Vector3f(0f, 1f, 0f), 0f); // y = 0
        Vector3f point = new Vector3f(0f, 3f, 0f);
        float originalDist = plane.pseudoDistance(point);

        Vector3f reflected = plane.reflect(point, null);
        float reflectedDist = plane.pseudoDistance(reflected);

        Assert.assertEquals(-originalDist, reflectedDist, TOLERANCE);
    }

    /** Reflecting a point that lies on the plane must return the same point. */
    @Test
    public void testReflectPointOnPlaneIsItself() {
        Plane plane = new Plane(new Vector3f(0f, 1f, 0f), 0f); // y = 0
        Vector3f onPlane = new Vector3f(1f, 0f, 2f);
        Vector3f reflected = plane.reflect(onPlane, null);
        Assert.assertEquals(onPlane.x, reflected.x, TOLERANCE);
        Assert.assertEquals(onPlane.y, reflected.y, TOLERANCE);
        Assert.assertEquals(onPlane.z, reflected.z, TOLERANCE);
    }

    /**
     * After setPlanePoints(A, B, C), all three points must satisfy
     * isOnPlane (pseudoDistance ≈ 0).
     */
    @Test
    public void testSetPlanePointsAllPointsAreOnPlane() {
        Vector3f a = new Vector3f(1f, 0f, 0f);
        Vector3f b = new Vector3f(0f, 1f, 0f);
        Vector3f c = new Vector3f(0f, 0f, 1f);
        Plane plane = new Plane();
        plane.setPlanePoints(a, b, c);

        Assert.assertTrue("A must be on the plane", plane.isOnPlane(a));
        Assert.assertTrue("B must be on the plane", plane.isOnPlane(b));
        Assert.assertTrue("C must be on the plane", plane.isOnPlane(c));
    }

    /** The result of getClosestPoint must lie on the plane (pseudoDistance ≈ 0). */
    @Test
    public void testGetClosestPointIsOnPlane() {
        Plane plane = new Plane(new Vector3f(0f, 1f, 0f), 0f); // y = 0
        Vector3f offPlane = new Vector3f(3f, 7f, -2f);
        Vector3f closest = plane.getClosestPoint(offPlane);
        Assert.assertTrue(plane.isOnPlane(closest));
    }

    /**
     * The closest point to P on the plane must be closer to P than any
     * other point on the plane.  We verify this by checking that
     * distance(P, closest) == |pseudoDistance(P)|.
     */
    @Test
    public void testGetClosestPointMinimizesDistance() {
        Plane plane = new Plane(new Vector3f(0f, 1f, 0f), 0f); // y = 0
        Vector3f p = new Vector3f(3f, 5f, -1f);
        Vector3f closest = plane.getClosestPoint(p);
        float dist = p.distance(closest);
        Assert.assertEquals(Math.abs(plane.pseudoDistance(p)), dist, TOLERANCE);
    }

    /**
     * Plane through the three points (1,0,0), (0,1,0), (0,0,1):
     * the normal must be proportional to (1,1,1) and all three
     * defining points satisfy pseudoDistance ≈ 0.
     */
    @Test
    public void testNonAxisAlignedPlane() {
        Vector3f a = new Vector3f(1f, 0f, 0f);
        Vector3f b = new Vector3f(0f, 1f, 0f);
        Vector3f c = new Vector3f(0f, 0f, 1f);
        Plane plane = new Plane();
        plane.setPlanePoints(a, b, c);

        Vector3f n = plane.getNormal();
        // Normal must point along (1,1,1)/sqrt(3) or its negation
        float expectedComponent = 1f / FastMath.sqrt(3f);
        Assert.assertEquals(expectedComponent, Math.abs(n.x), TOLERANCE);
        Assert.assertEquals(expectedComponent, Math.abs(n.y), TOLERANCE);
        Assert.assertEquals(expectedComponent, Math.abs(n.z), TOLERANCE);

        // All defining points must be on the plane
        Assert.assertTrue(plane.isOnPlane(a));
        Assert.assertTrue(plane.isOnPlane(b));
        Assert.assertTrue(plane.isOnPlane(c));
    }
}
