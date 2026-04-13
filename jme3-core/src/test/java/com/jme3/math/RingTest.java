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
 * Verifies that the {@link Ring} class works correctly.
 */
public class RingTest {

    @Test
    public void testDefaultConstructor() {
        Ring r = new Ring();
        Assert.assertEquals(0f, r.getInnerRadius(), 0f);
        Assert.assertEquals(1f, r.getOuterRadius(), 0f);
        Assert.assertNotNull(r.getCenter());
        Assert.assertNotNull(r.getUp());
    }

    @Test
    public void testParameterizedConstructor() {
        Vector3f center = new Vector3f(1f, 2f, 3f);
        Vector3f up = new Vector3f(0f, 1f, 0f);
        Ring r = new Ring(center, up, 0.5f, 2f);
        Assert.assertSame(center, r.getCenter());
        Assert.assertSame(up, r.getUp());
        Assert.assertEquals(0.5f, r.getInnerRadius(), 0f);
        Assert.assertEquals(2f, r.getOuterRadius(), 0f);
    }

    @Test
    public void testSetters() {
        Ring r = new Ring();
        Vector3f center = new Vector3f(1f, 0f, 0f);
        Vector3f up = new Vector3f(0f, 0f, 1f);
        r.setCenter(center);
        r.setUp(up);
        r.setInnerRadius(1f);
        r.setOuterRadius(3f);
        Assert.assertSame(center, r.getCenter());
        Assert.assertSame(up, r.getUp());
        Assert.assertEquals(1f, r.getInnerRadius(), 0f);
        Assert.assertEquals(3f, r.getOuterRadius(), 0f);
    }

    @Test
    public void testRandomReturnsNonNull() {
        Ring r = new Ring();
        Assert.assertNotNull(r.random());

        Vector3f store = new Vector3f();
        Assert.assertSame(store, r.random(store));
    }

    @Test
    public void testClone() {
        Ring original = new Ring(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(0f, 1f, 0f),
                0.5f, 2f);
        Ring cloned = original.clone();
        Assert.assertNotSame(original, cloned);
        Assert.assertEquals(original.getInnerRadius(), cloned.getInnerRadius(), 0f);
        Assert.assertEquals(original.getOuterRadius(), cloned.getOuterRadius(), 0f);
        Assert.assertEquals(original.getCenter(), cloned.getCenter());
        Assert.assertNotSame(original.getCenter(), cloned.getCenter());
    }
}
