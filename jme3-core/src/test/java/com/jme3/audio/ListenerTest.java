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
package com.jme3.audio;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link Listener} class works correctly.
 */
public class ListenerTest {

    private static final float TOLERANCE = 1e-6f;

    @Test
    public void testDefaultConstructor() {
        Listener listener = new Listener();
        Assert.assertNotNull(listener.getLocation());
        Assert.assertNotNull(listener.getVelocity());
        Assert.assertNotNull(listener.getRotation());
        Assert.assertEquals(1f, listener.getVolume(), 0f);
    }

    @Test
    public void testCopyConstructor() {
        Listener original = new Listener();
        original.setLocation(new Vector3f(1f, 2f, 3f));
        original.setVelocity(new Vector3f(4f, 5f, 6f));
        original.setVolume(0.5f);

        Listener copy = new Listener(original);
        Assert.assertEquals(1f, copy.getLocation().x, TOLERANCE);
        Assert.assertEquals(2f, copy.getLocation().y, TOLERANCE);
        Assert.assertEquals(3f, copy.getLocation().z, TOLERANCE);
        Assert.assertEquals(4f, copy.getVelocity().x, TOLERANCE);
        Assert.assertEquals(0.5f, copy.getVolume(), TOLERANCE);
    }

    @Test
    public void testSetLocation() {
        Listener listener = new Listener();
        listener.setLocation(new Vector3f(10f, 20f, 30f));
        Assert.assertEquals(10f, listener.getLocation().x, TOLERANCE);
        Assert.assertEquals(20f, listener.getLocation().y, TOLERANCE);
        Assert.assertEquals(30f, listener.getLocation().z, TOLERANCE);
    }

    @Test
    public void testSetVelocity() {
        Listener listener = new Listener();
        listener.setVelocity(new Vector3f(1f, 0f, 0f));
        Assert.assertEquals(1f, listener.getVelocity().x, TOLERANCE);
    }

    @Test
    public void testSetRotation() {
        Listener listener = new Listener();
        Quaternion q = new Quaternion(0f, 0f, 0f, 1f);
        listener.setRotation(q);
        Assert.assertEquals(q, listener.getRotation());
    }

    @Test
    public void testSetVolume() {
        Listener listener = new Listener();
        listener.setVolume(0.7f);
        Assert.assertEquals(0.7f, listener.getVolume(), TOLERANCE);
    }

    @Test
    public void testGetDirectionVectors() {
        Listener listener = new Listener();
        // Default rotation should give reasonable direction vectors
        Assert.assertNotNull(listener.getLeft());
        Assert.assertNotNull(listener.getUp());
        Assert.assertNotNull(listener.getDirection());
    }

    @Test
    public void testSetRendererDoesNotCrash() {
        Listener listener = new Listener();
        // Setting null renderer should not crash
        listener.setRenderer(null);
        // Setting volume with null renderer should not crash
        listener.setVolume(0.5f);
    }
}
