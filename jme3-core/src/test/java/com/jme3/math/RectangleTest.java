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
 * Verifies that the {@link Rectangle} class works correctly.
 */
public class RectangleTest {

    private static final float TOLERANCE = 1e-5f;

    @Test
    public void testDefaultConstructor() {
        Rectangle r = new Rectangle();
        Assert.assertNotNull(r.getA());
        Assert.assertNotNull(r.getB());
        Assert.assertNotNull(r.getC());
    }

    @Test
    public void testParameterizedConstructor() {
        Vector3f a = new Vector3f(0f, 0f, 0f);
        Vector3f b = new Vector3f(1f, 0f, 0f);
        Vector3f c = new Vector3f(0f, 1f, 0f);
        Rectangle r = new Rectangle(a, b, c);
        Assert.assertSame(a, r.getA());
        Assert.assertSame(b, r.getB());
        Assert.assertSame(c, r.getC());
    }

    @Test
    public void testSetters() {
        Rectangle r = new Rectangle();
        Vector3f a = new Vector3f(1f, 0f, 0f);
        Vector3f b = new Vector3f(0f, 1f, 0f);
        Vector3f c = new Vector3f(0f, 0f, 1f);
        r.setA(a);
        r.setB(b);
        r.setC(c);
        Assert.assertSame(a, r.getA());
        Assert.assertSame(b, r.getB());
        Assert.assertSame(c, r.getC());
    }

    @Test
    public void testCalculateD() {
        // A=(0,0,0), B=(1,0,0), C=(0,1,0) => D = B+C-A = (1,1,0)
        Rectangle r = new Rectangle(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));
        Vector3f d = r.calculateD();
        Assert.assertEquals(1f, d.x, TOLERANCE);
        Assert.assertEquals(1f, d.y, TOLERANCE);
        Assert.assertEquals(0f, d.z, TOLERANCE);
    }

    @Test
    public void testCalculateNormal() {
        // Axis-aligned rectangle in XY plane
        Rectangle r = new Rectangle(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));
        Vector3f normal = r.calculateNormal(null);
        Assert.assertNotNull(normal);
        // Normal should be (0,0,1) or (0,0,-1)
        Assert.assertEquals(0f, normal.x, TOLERANCE);
        Assert.assertEquals(0f, normal.y, TOLERANCE);
        Assert.assertEquals(1f, Math.abs(normal.z), TOLERANCE);
    }

    @Test
    public void testRandomReturnsNonNull() {
        Rectangle r = new Rectangle(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));
        Assert.assertNotNull(r.random());
        Vector3f store = new Vector3f();
        Assert.assertSame(store, r.random(store));
    }

    @Test
    public void testClone() {
        Rectangle original = new Rectangle(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));
        Rectangle cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getA(), cloned.getA());
        Assert.assertEquals(original.getB(), cloned.getB());
        Assert.assertEquals(original.getC(), cloned.getC());
    }

    @Test
    public void testToString() {
        Rectangle r = new Rectangle(
                new Vector3f(0f, 0f, 0f),
                new Vector3f(1f, 0f, 0f),
                new Vector3f(0f, 1f, 0f));
        Assert.assertNotNull(r.toString());
    }
}
