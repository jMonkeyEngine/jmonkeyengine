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
package com.jme3.util;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.math.Vector4f;
import java.nio.FloatBuffer;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies that the {@link BufferUtils} class works correctly.
 */
public class BufferUtilsTest {

    private static final float TOLERANCE = 1e-6f;

    // -----------------------------------------------------------------------
    // createFloatBuffer
    // -----------------------------------------------------------------------

    @Test
    public void testCreateFloatBufferFromFloatVarargs() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(1f, 2f, 3f);
        Assert.assertNotNull(buf);
        Assert.assertEquals(3, buf.capacity());
        Assert.assertEquals(1f, buf.get(0), TOLERANCE);
        Assert.assertEquals(2f, buf.get(1), TOLERANCE);
        Assert.assertEquals(3f, buf.get(2), TOLERANCE);
    }

    @Test
    public void testCreateFloatBufferFromVector3fArray() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(4f, 5f, 6f));
        Assert.assertEquals(6, buf.capacity());
        Assert.assertEquals(1f, buf.get(0), TOLERANCE);
        Assert.assertEquals(2f, buf.get(1), TOLERANCE);
        Assert.assertEquals(3f, buf.get(2), TOLERANCE);
    }

    @Test
    public void testCreateFloatBufferFromVector2fArray() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new Vector2f(1f, 2f),
                new Vector2f(3f, 4f));
        Assert.assertEquals(4, buf.capacity());
        Assert.assertEquals(1f, buf.get(0), TOLERANCE);
        Assert.assertEquals(2f, buf.get(1), TOLERANCE);
        Assert.assertEquals(3f, buf.get(2), TOLERANCE);
        Assert.assertEquals(4f, buf.get(3), TOLERANCE);
    }

    @Test
    public void testCreateFloatBufferFromVector4fArray() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new Vector4f(1f, 2f, 3f, 4f));
        Assert.assertEquals(4, buf.capacity());
        Assert.assertEquals(4f, buf.get(3), TOLERANCE);
    }

    @Test
    public void testCreateFloatBufferFromColorRGBA() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new ColorRGBA(1f, 0.5f, 0.25f, 1f));
        Assert.assertEquals(4, buf.capacity());
        Assert.assertEquals(1f, buf.get(0), TOLERANCE);
        Assert.assertEquals(0.5f, buf.get(1), TOLERANCE);
    }

    @Test
    public void testCreateFloatBufferFromQuaternion() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new Quaternion(0f, 0f, 0f, 1f));
        Assert.assertEquals(4, buf.capacity());
    }

    // -----------------------------------------------------------------------
    // Vector3 buffer operations
    // -----------------------------------------------------------------------

    @Test
    public void testCreateVector3Buffer() {
        FloatBuffer buf = BufferUtils.createVector3Buffer(3);
        Assert.assertNotNull(buf);
        Assert.assertEquals(9, buf.capacity());
    }

    @Test
    public void testSetAndPopulateVector3() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(6);
        Vector3f v = new Vector3f(1f, 2f, 3f);
        BufferUtils.setInBuffer(v, buf, 0);
        Vector3f result = new Vector3f();
        BufferUtils.populateFromBuffer(result, buf, 0);
        Assert.assertEquals(1f, result.x, TOLERANCE);
        Assert.assertEquals(2f, result.y, TOLERANCE);
        Assert.assertEquals(3f, result.z, TOLERANCE);
    }

    @Test
    public void testEqualsVector3InBuffer() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(6);
        Vector3f v = new Vector3f(1f, 2f, 3f);
        BufferUtils.setInBuffer(v, buf, 0);
        Assert.assertTrue(BufferUtils.equals(v, buf, 0));
        Assert.assertFalse(BufferUtils.equals(new Vector3f(4f, 5f, 6f), buf, 0));
    }

    @Test
    public void testGetVector3Array() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new Vector3f(1f, 2f, 3f),
                new Vector3f(4f, 5f, 6f));
        Vector3f[] arr = BufferUtils.getVector3Array(buf);
        Assert.assertEquals(2, arr.length);
        Assert.assertEquals(1f, arr[0].x, TOLERANCE);
        Assert.assertEquals(4f, arr[1].x, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Vector2 buffer operations
    // -----------------------------------------------------------------------

    @Test
    public void testCreateVector2Buffer() {
        FloatBuffer buf = BufferUtils.createVector2Buffer(4);
        Assert.assertNotNull(buf);
        Assert.assertEquals(8, buf.capacity());
    }

    @Test
    public void testSetAndPopulateVector2() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(4);
        Vector2f v = new Vector2f(3f, 7f);
        BufferUtils.setInBuffer(v, buf, 0);
        Vector2f result = new Vector2f();
        BufferUtils.populateFromBuffer(result, buf, 0);
        Assert.assertEquals(3f, result.x, TOLERANCE);
        Assert.assertEquals(7f, result.y, TOLERANCE);
    }

    @Test
    public void testGetVector2Array() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(
                new Vector2f(1f, 2f),
                new Vector2f(3f, 4f));
        Vector2f[] arr = BufferUtils.getVector2Array(buf);
        Assert.assertEquals(2, arr.length);
        Assert.assertEquals(1f, arr[0].x, TOLERANCE);
        Assert.assertEquals(3f, arr[1].x, TOLERANCE);
    }

    // -----------------------------------------------------------------------
    // Vector4 buffer operations
    // -----------------------------------------------------------------------

    @Test
    public void testSetAndPopulateVector4() {
        FloatBuffer buf = BufferUtils.createFloatBuffer(8);
        Vector4f v = new Vector4f(1f, 2f, 3f, 4f);
        BufferUtils.setInBuffer(v, buf, 0);
        Vector4f result = new Vector4f();
        BufferUtils.populateFromBuffer(result, buf, 0);
        Assert.assertEquals(1f, result.x, TOLERANCE);
        Assert.assertEquals(4f, result.w, TOLERANCE);
    }
}
