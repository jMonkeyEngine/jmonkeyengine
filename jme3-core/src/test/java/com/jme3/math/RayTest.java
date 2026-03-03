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
 * Verifies that the {@link Ray} class works correctly.
 */
public class RayTest {

    private static final float TOLERANCE = 1e-5f;

    @Test
    public void testDefaultConstructor() {
        Ray r = new Ray();
        Assert.assertEquals(0f, r.getOrigin().x, 0f);
        Assert.assertEquals(0f, r.getOrigin().y, 0f);
        Assert.assertEquals(0f, r.getOrigin().z, 0f);
        Assert.assertEquals(0f, r.getDirection().x, 0f);
        Assert.assertEquals(0f, r.getDirection().y, 0f);
        Assert.assertEquals(1f, r.getDirection().z, 0f);
        Assert.assertTrue(Float.isInfinite(r.getLimit()));
    }

    @Test
    public void testParameterizedConstructor() {
        Vector3f origin = new Vector3f(1f, 2f, 3f);
        Vector3f direction = new Vector3f(0f, 0f, 1f);
        Ray r = new Ray(origin, direction);
        Assert.assertEquals(1f, r.getOrigin().x, 0f);
        Assert.assertEquals(2f, r.getOrigin().y, 0f);
        Assert.assertEquals(3f, r.getOrigin().z, 0f);
        Assert.assertEquals(0f, r.getDirection().x, 0f);
        Assert.assertEquals(0f, r.getDirection().y, 0f);
        Assert.assertEquals(1f, r.getDirection().z, 0f);
    }

    @Test
    public void testSetOriginAndDirection() {
        Ray r = new Ray();
        r.setOrigin(new Vector3f(5f, 0f, 0f));
        r.setDirection(new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(5f, r.getOrigin().x, 0f);
        Assert.assertEquals(1f, r.getDirection().x, 0f);
    }

    @Test
    public void testSetLimit() {
        Ray r = new Ray();
        r.setLimit(10f);
        Assert.assertEquals(10f, r.getLimit(), 0f);
    }

    @Test
    public void testSetFromSource() {
        Ray source = new Ray(new Vector3f(1f, 2f, 3f), new Vector3f(0f, 1f, 0f));
        Ray r = new Ray();
        r.set(source);
        Assert.assertEquals(1f, r.getOrigin().x, 0f);
        Assert.assertEquals(2f, r.getOrigin().y, 0f);
        Assert.assertEquals(0f, r.getDirection().x, 0f);
        Assert.assertEquals(1f, r.getDirection().y, 0f);
    }

    @Test
    public void testDistanceSquaredToPoint() {
        // Ray along X axis from origin
        Ray r = new Ray(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f));
        // Point at (0, 3, 0) - distance squared to X axis = 9
        float d2 = r.distanceSquared(new Vector3f(0f, 3f, 0f));
        Assert.assertEquals(9f, d2, TOLERANCE);
    }

    @Test
    public void testDistanceSquaredToPointBehindOrigin() {
        // Ray pointing along +X from origin; point is at (-5, 0, 0)
        // Since it's behind the origin, closest point is the origin
        Ray r = new Ray(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f));
        float d2 = r.distanceSquared(new Vector3f(-5f, 0f, 0f));
        Assert.assertEquals(25f, d2, TOLERANCE);
    }

    @Test
    public void testIntersectWhere_hit() {
        // Ray from (0,0,-5) pointing in +Z direction
        Ray r = new Ray(new Vector3f(0f, 0f, -5f), new Vector3f(0f, 0f, 1f));
        // Triangle in XY plane at z=0
        Vector3f v0 = new Vector3f(-1f, -1f, 0f);
        Vector3f v1 = new Vector3f(1f, -1f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);
        Vector3f loc = new Vector3f();
        boolean hit = r.intersectWhere(v0, v1, v2, loc);
        Assert.assertTrue(hit);
        Assert.assertEquals(0f, loc.z, TOLERANCE);
    }

    @Test
    public void testIntersectWhere_miss() {
        // Ray pointing away from the triangle
        Ray r = new Ray(new Vector3f(5f, 5f, -5f), new Vector3f(0f, 0f, 1f));
        Vector3f v0 = new Vector3f(-1f, -1f, 0f);
        Vector3f v1 = new Vector3f(1f, -1f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);
        Vector3f loc = new Vector3f();
        boolean hit = r.intersectWhere(v0, v1, v2, loc);
        Assert.assertFalse(hit);
    }

    @Test
    public void testClone() {
        Ray original = new Ray(new Vector3f(1f, 2f, 3f), new Vector3f(0f, 0f, 1f));
        Ray cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getOrigin(), cloned.getOrigin());
        Assert.assertEquals(original.getDirection(), cloned.getDirection());
    }

    @Test
    public void testToString() {
        Ray r = new Ray(new Vector3f(1f, 2f, 3f), new Vector3f(0f, 0f, 1f));
        String s = r.toString();
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("Ray"));
    }
}
