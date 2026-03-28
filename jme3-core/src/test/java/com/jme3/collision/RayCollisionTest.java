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
package com.jme3.collision;

import com.jme3.math.Ray;
import com.jme3.math.Triangle;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests barycentric coordinate computation in ray vs. triangle collision.
 */
public class RayCollisionTest {

    private static final float DELTA = 1e-5f;

    /**
     * A ray hitting exactly at v0 should give barycoords (0, 0),
     * meaning the weight of v0 is 1 and the weights of v1 and v2 are 0.
     */
    @Test
    public void testBarycentricAtV0() {
        // Triangle in the XY plane
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        // Ray aimed straight at v0
        Ray ray = new Ray(new Vector3f(0f, 0f, 5f), new Vector3f(0f, 0f, -1f));

        Vector2f bary = new Vector2f();
        float t = ray.intersects(v0, v1, v2, bary);

        Assert.assertFalse("Expected intersection", Float.isInfinite(t));
        Assert.assertEquals("u should be 0 at v0", 0f, bary.x, DELTA);
        Assert.assertEquals("v should be 0 at v0", 0f, bary.y, DELTA);
        // w0 = 1 - u - v = 1
    }

    /**
     * A ray hitting exactly at v1 should give barycoords (1, 0).
     */
    @Test
    public void testBarycentricAtV1() {
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        // Ray aimed straight at v1
        Ray ray = new Ray(new Vector3f(1f, 0f, 5f), new Vector3f(0f, 0f, -1f));

        Vector2f bary = new Vector2f();
        float t = ray.intersects(v0, v1, v2, bary);

        Assert.assertFalse("Expected intersection", Float.isInfinite(t));
        Assert.assertEquals("u should be 1 at v1", 1f, bary.x, DELTA);
        Assert.assertEquals("v should be 0 at v1", 0f, bary.y, DELTA);
        // w0 = 1 - 1 - 0 = 0
    }

    /**
     * A ray hitting exactly at v2 should give barycoords (0, 1).
     */
    @Test
    public void testBarycentricAtV2() {
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        // Ray aimed straight at v2
        Ray ray = new Ray(new Vector3f(0f, 1f, 5f), new Vector3f(0f, 0f, -1f));

        Vector2f bary = new Vector2f();
        float t = ray.intersects(v0, v1, v2, bary);

        Assert.assertFalse("Expected intersection", Float.isInfinite(t));
        Assert.assertEquals("u should be 0 at v2", 0f, bary.x, DELTA);
        Assert.assertEquals("v should be 1 at v2", 1f, bary.y, DELTA);
        // w0 = 1 - 0 - 1 = 0
    }

    /**
     * A ray hitting the centroid should give barycoords (1/3, 1/3).
     */
    @Test
    public void testBarycentricAtCentroid() {
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        // Centroid = (v0 + v1 + v2) / 3 = (1/3, 1/3, 0)
        float cx = (v0.x + v1.x + v2.x) / 3f;
        float cy = (v0.y + v1.y + v2.y) / 3f;
        Ray ray = new Ray(new Vector3f(cx, cy, 5f), new Vector3f(0f, 0f, -1f));

        Vector2f bary = new Vector2f();
        float t = ray.intersects(v0, v1, v2, bary);

        Assert.assertFalse("Expected intersection", Float.isInfinite(t));
        Assert.assertEquals("u should be 1/3 at centroid", 1f / 3f, bary.x, DELTA);
        Assert.assertEquals("v should be 1/3 at centroid", 1f / 3f, bary.y, DELTA);
        // w0 = 1 - 1/3 - 1/3 = 1/3
    }

    /**
     * Verifies that the barycentric coordinates reconstruct the contact point.
     */
    @Test
    public void testBarycentricReconstructsContactPoint() {
        Vector3f v0 = new Vector3f(2f, 0f, 0f);
        Vector3f v1 = new Vector3f(0f, 3f, 0f);
        Vector3f v2 = new Vector3f(-1f, -1f, 0f);

        // Hit point: midpoint of edge v0-v1 (weight v0=0.5, v1=0.5, v2=0)
        float hitX = (v0.x + v1.x) / 2f;
        float hitY = (v0.y + v1.y) / 2f;
        Ray ray = new Ray(new Vector3f(hitX, hitY, 10f), new Vector3f(0f, 0f, -1f));

        Vector2f bary = new Vector2f();
        float t = ray.intersects(v0, v1, v2, bary);

        Assert.assertFalse("Expected intersection", Float.isInfinite(t));

        float u = bary.x; // weight of v1
        float v = bary.y; // weight of v2
        float w = 1f - u - v; // weight of v0

        // Reconstruct hit point from barycentric coords
        float recX = w * v0.x + u * v1.x + v * v2.x;
        float recY = w * v0.y + u * v1.y + v * v2.y;

        Assert.assertEquals("Reconstructed X matches hit point", hitX, recX, DELTA);
        Assert.assertEquals("Reconstructed Y matches hit point", hitY, recY, DELTA);
    }

    /**
     * Verifies that CollisionResult contains barycentric coords when a ray
     * collides with an AbstractTriangle.
     */
    @Test
    public void testCollisionResultContainsBaryCoords() {
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        // Ray aimed at centroid
        float cx = (v0.x + v1.x + v2.x) / 3f;
        float cy = (v0.y + v1.y + v2.y) / 3f;
        Ray ray = new Ray(new Vector3f(cx, cy, 5f), new Vector3f(0f, 0f, -1f));

        Triangle tri = new Triangle(v0, v1, v2);
        CollisionResults results = new CollisionResults();
        int count = ray.collideWith(tri, results);

        Assert.assertEquals("Expected exactly 1 collision", 1, count);

        CollisionResult cr = results.getClosestCollision();
        Assert.assertNotNull("CollisionResult should not be null", cr);

        Vector2f bary = cr.getContactBaryCoords();
        Assert.assertNotNull("Barycentric coords should be set", bary);
        Assert.assertEquals("u should be 1/3", 1f / 3f, bary.x, DELTA);
        Assert.assertEquals("v should be 1/3", 1f / 3f, bary.y, DELTA);
    }

    /**
     * Verifies that no intersection returns POSITIVE_INFINITY and bary is not
     * modified.
     */
    @Test
    public void testNoIntersectionLeavesBaryUnchanged() {
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        // Ray that misses the triangle (outside the XY square)
        Ray ray = new Ray(new Vector3f(5f, 5f, 5f), new Vector3f(0f, 0f, -1f));

        Vector2f bary = new Vector2f(99f, 99f);
        float t = ray.intersects(v0, v1, v2, bary);

        Assert.assertTrue("Expected no intersection", Float.isInfinite(t));
        // bary should be untouched on miss
        Assert.assertEquals("bary.x should be unchanged", 99f, bary.x, DELTA);
        Assert.assertEquals("bary.y should be unchanged", 99f, bary.y, DELTA);
    }

    /**
     * Verifies that passing null for baryCoords still returns correct distance.
     */
    @Test
    public void testNullBaryCoordsDoesNotThrow() {
        Vector3f v0 = new Vector3f(0f, 0f, 0f);
        Vector3f v1 = new Vector3f(1f, 0f, 0f);
        Vector3f v2 = new Vector3f(0f, 1f, 0f);

        Ray ray = new Ray(new Vector3f(0.25f, 0.25f, 5f), new Vector3f(0f, 0f, -1f));

        float t = ray.intersects(v0, v1, v2, null);

        Assert.assertFalse("Expected intersection", Float.isInfinite(t));
        Assert.assertEquals("Distance should be 5", 5f, t, DELTA);
    }
}
