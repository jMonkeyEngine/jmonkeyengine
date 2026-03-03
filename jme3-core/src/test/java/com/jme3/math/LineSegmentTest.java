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
 * Verifies that the {@link LineSegment} class works correctly.
 */
public class LineSegmentTest {

    private static final float TOLERANCE = 1e-5f;

    @Test
    public void testDefaultConstructor() {
        LineSegment seg = new LineSegment();
        Assert.assertEquals(0f, seg.getExtent(), 0f);
        Assert.assertNotNull(seg.getOrigin());
        Assert.assertNotNull(seg.getDirection());
    }

    @Test
    public void testOriginDirectionExtentConstructor() {
        Vector3f origin = new Vector3f(0f, 0f, 0f);
        Vector3f direction = new Vector3f(0f, 1f, 0f);
        LineSegment seg = new LineSegment(origin, direction, 5f);
        Assert.assertSame(origin, seg.getOrigin());
        Assert.assertSame(direction, seg.getDirection());
        Assert.assertEquals(5f, seg.getExtent(), 0f);
    }

    @Test
    public void testStartEndConstructor() {
        // Segment from (0,0,0) to (0,2,0) -> midpoint at (0,1,0), direction (0,1,0), extent 1
        Vector3f start = new Vector3f(0f, 0f, 0f);
        Vector3f end = new Vector3f(0f, 2f, 0f);
        LineSegment seg = new LineSegment(start, end);
        Assert.assertEquals(0f, seg.getOrigin().x, TOLERANCE);
        Assert.assertEquals(1f, seg.getOrigin().y, TOLERANCE);
        Assert.assertEquals(0f, seg.getOrigin().z, TOLERANCE);
        Assert.assertEquals(0f, seg.getDirection().x, TOLERANCE);
        Assert.assertEquals(1f, seg.getDirection().y, TOLERANCE);
        Assert.assertEquals(1f, seg.getExtent(), TOLERANCE);
    }

    @Test
    public void testCopyConstructor() {
        LineSegment original = new LineSegment(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(0f, 1f, 0f),
                4f);
        LineSegment copy = new LineSegment(original);
        Assert.assertEquals(original.getOrigin(), copy.getOrigin());
        Assert.assertEquals(original.getDirection(), copy.getDirection());
        Assert.assertEquals(original.getExtent(), copy.getExtent(), 0f);
        Assert.assertNotSame(original.getOrigin(), copy.getOrigin());
    }

    @Test
    public void testSetFromExisting() {
        LineSegment source = new LineSegment(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(0f, 1f, 0f),
                4f);
        LineSegment target = new LineSegment();
        target.set(source);
        Assert.assertEquals(source.getOrigin(), target.getOrigin());
        Assert.assertEquals(source.getExtent(), target.getExtent(), 0f);
    }

    @Test
    public void testSettersGetters() {
        LineSegment seg = new LineSegment();
        Vector3f origin = new Vector3f(5f, 0f, 0f);
        Vector3f direction = new Vector3f(1f, 0f, 0f);
        seg.setOrigin(origin);
        seg.setDirection(direction);
        seg.setExtent(3f);
        Assert.assertSame(origin, seg.getOrigin());
        Assert.assertSame(direction, seg.getDirection());
        Assert.assertEquals(3f, seg.getExtent(), 0f);
    }

    @Test
    public void testGetPositiveEnd() {
        // origin=(0,0,0), direction=(0,1,0), extent=5 -> positive end at (0,5,0)
        LineSegment seg = new LineSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                5f);
        Vector3f posEnd = seg.getPositiveEnd(null);
        Assert.assertEquals(0f, posEnd.x, TOLERANCE);
        Assert.assertEquals(5f, posEnd.y, TOLERANCE);
        Assert.assertEquals(0f, posEnd.z, TOLERANCE);
    }

    @Test
    public void testGetNegativeEnd() {
        // origin=(0,0,0), direction=(0,1,0), extent=5 -> negative end at (0,-5,0)
        LineSegment seg = new LineSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                5f);
        Vector3f negEnd = seg.getNegativeEnd(null);
        Assert.assertEquals(0f, negEnd.x, TOLERANCE);
        Assert.assertEquals(-5f, negEnd.y, TOLERANCE);
        Assert.assertEquals(0f, negEnd.z, TOLERANCE);
    }

    @Test
    public void testDistanceSquaredToPointOnSegment() {
        // Segment along Y from -1 to +1 (origin=0,0,0; direction=0,1,0; extent=1)
        LineSegment seg = new LineSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                1f);
        // Point at (3, 0, 0) -> closest point on segment is origin, distance^2 = 9
        Assert.assertEquals(9f, seg.distanceSquared(new Vector3f(3f, 0f, 0f)), TOLERANCE);
    }

    @Test
    public void testDistanceToPoint() {
        LineSegment seg = new LineSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                1f);
        Assert.assertEquals(3f, seg.distance(new Vector3f(3f, 0f, 0f)), TOLERANCE);
    }

    @Test
    public void testIsPointInsideBounds() {
        // Segment from (0,0,0) to (0,2,0): origin=(0,1,0), direction=(0,1,0), extent=1
        LineSegment seg = new LineSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 2f, 0f));
        // midpoint
        Assert.assertTrue(seg.isPointInsideBounds(new Vector3f(0f, 1f, 0f), 0.01f));
        // way outside
        Assert.assertFalse(seg.isPointInsideBounds(new Vector3f(5f, 5f, 5f)));
    }

    @Test
    public void testClone() {
        LineSegment original = new LineSegment(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(0f, 1f, 0f),
                4f);
        LineSegment cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getOrigin(), cloned.getOrigin());
        Assert.assertNotSame(original.getOrigin(), cloned.getOrigin());
    }

    @Test
    public void testToString() {
        LineSegment seg = new LineSegment(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(0f, 1f, 0f),
                1f);
        String s = seg.toString();
        Assert.assertNotNull(s);
        Assert.assertTrue(s.contains("LineSegment"));
    }
}
