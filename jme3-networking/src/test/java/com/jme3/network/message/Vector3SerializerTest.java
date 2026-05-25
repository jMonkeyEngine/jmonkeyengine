/*
 * Copyright (c) 2026 jMonkeyEngine
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
package com.jme3.network.serializing.serializers;

import com.jme3.math.Vector3f;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * Round-trip tests for {@link Vector3Serializer}, which encodes positions and
 * directions in multiplayer messages.
 */
public class Vector3SerializerTest {

    private static final int BYTES_PER_VECTOR = 12;

    @Test
    public void testRoundTripPreservesComponents() throws IOException {
        Vector3Serializer serializer = new Vector3Serializer();
        Vector3f original = new Vector3f(1.25f, -2.5f, 3.75f);

        ByteBuffer buffer = ByteBuffer.allocate(BYTES_PER_VECTOR);
        serializer.writeObject(buffer, original);
        buffer.flip();

        Vector3f decoded = serializer.readObject(buffer, Vector3f.class);

        assertNotSame(original, decoded);
        assertEquals(original.x, decoded.x, 0f);
        assertEquals(original.y, decoded.y, 0f);
        assertEquals(original.z, decoded.z, 0f);
        assertEquals(BYTES_PER_VECTOR, buffer.position());
    }

    @Test
    public void testRoundTripZeroVector() throws IOException {
        Vector3Serializer serializer = new Vector3Serializer();
        Vector3f original = new Vector3f(0, 0, 0);

        ByteBuffer buffer = ByteBuffer.allocate(BYTES_PER_VECTOR);
        serializer.writeObject(buffer, original);
        buffer.flip();

        Vector3f decoded = serializer.readObject(buffer, Vector3f.class);

        assertEquals(0f, decoded.x, 0f);
        assertEquals(0f, decoded.y, 0f);
        assertEquals(0f, decoded.z, 0f);
    }

    @Test
    public void testSequentialVectorsDoNotShareState() throws IOException {
        Vector3Serializer serializer = new Vector3Serializer();
        Vector3f first = new Vector3f(1, 2, 3);
        Vector3f second = new Vector3f(4, 5, 6);

        ByteBuffer buffer = ByteBuffer.allocate(BYTES_PER_VECTOR * 2);
        serializer.writeObject(buffer, first);
        serializer.writeObject(buffer, second);
        buffer.flip();

        Vector3f decodedFirst = serializer.readObject(buffer, Vector3f.class);
        Vector3f decodedSecond = serializer.readObject(buffer, Vector3f.class);

        assertEquals(1f, decodedFirst.x, 0f);
        assertEquals(4f, decodedSecond.x, 0f);
        assertEquals(BYTES_PER_VECTOR * 2, buffer.position());
    }
}
