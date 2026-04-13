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
 * Verifies that the {@link Line} class works correctly.
 */
public class LineTest {

    private static final float TOLERANCE = 1e-5f;

    @Test
    public void testDefaultConstructor() {
        Line l = new Line();
        Assert.assertEquals(0f, l.getOrigin().x, 0f);
        Assert.assertEquals(0f, l.getDirection().x, 0f);
    }

    @Test
    public void testParameterizedConstructor() {
        Vector3f origin = new Vector3f(1f, 2f, 3f);
        Vector3f direction = new Vector3f(0f, 1f, 0f);
        Line l = new Line(origin, direction);
        Assert.assertSame(origin, l.getOrigin());
        Assert.assertSame(direction, l.getDirection());
    }

    @Test
    public void testSetters() {
        Line l = new Line();
        Vector3f origin = new Vector3f(1f, 2f, 3f);
        Vector3f direction = new Vector3f(0f, 1f, 0f);
        l.setOrigin(origin);
        l.setDirection(direction);
        Assert.assertSame(origin, l.getOrigin());
        Assert.assertSame(direction, l.getDirection());
    }

    @Test
    public void testDistanceSquaredToOrigin() {
        // Line along X axis through origin, direction = (1,0,0)
        Line l = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f));
        // A point on the X axis has zero distance
        Assert.assertEquals(0f, l.distanceSquared(new Vector3f(5f, 0f, 0f)), TOLERANCE);
    }

    @Test
    public void testDistanceSquaredPerpendicular() {
        // Line along X axis; point at (0, 3, 0) should be 3 units away
        Line l = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(9f, l.distanceSquared(new Vector3f(0f, 3f, 0f)), TOLERANCE);
    }

    @Test
    public void testDistance() {
        Line l = new Line(new Vector3f(0f, 0f, 0f), new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(3f, l.distance(new Vector3f(0f, 3f, 0f)), TOLERANCE);
    }

    @Test
    public void testClone() {
        Line original = new Line(new Vector3f(1f, 2f, 3f), new Vector3f(0f, 1f, 0f));
        Line cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getOrigin(), cloned.getOrigin());
        Assert.assertEquals(original.getDirection(), cloned.getDirection());
    }

    @Test
    public void testToString() {
        Line l = new Line(new Vector3f(1f, 2f, 3f), new Vector3f(0f, 1f, 0f));
        String s = l.toString();
        Assert.assertNotNull(s);
        Assert.assertFalse(s.isEmpty());
    }
}
