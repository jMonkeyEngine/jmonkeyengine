/*
 * Copyright (c) 2023 jMonkeyEngine
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

package com.jme3.scene.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

/**
 * Tests selected methods of the Mesh class.
 *
 * @author Melvyn Linke
 */
public class MeshTest {

    /**
     * Tests getVertexCount() on a empty Mesh.
     */
    @Test
    public void testVertexCountOfEmptyMesh() {
        final Mesh mesh = new Mesh();

        assertEquals(-1, mesh.getVertexCount());
    }

    /**
     * Tests interleaving half-float vertex buffers, which are byte-backed.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testInterleavedHalfBuffer() {
        final Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, VertexBuffer.Format.Float,
                BufferUtils.createFloatBuffer(0f, 0f, 0f, 1f, 1f, 1f));

        ByteBuffer halfTexCoords = BufferUtils.createByteBuffer(8);
        halfTexCoords.putShort((short) 0);
        halfTexCoords.putShort((short) 0);
        halfTexCoords.putShort((short) 0x3c00);
        halfTexCoords.putShort((short) 0x3c00);
        halfTexCoords.clear();
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, VertexBuffer.Format.Half, halfTexCoords);

        mesh.setInterleaved();

        VertexBuffer interleaved = mesh.getBuffer(VertexBuffer.Type.InterleavedData);
        assertNotNull(interleaved);
        assertEquals(VertexBuffer.Format.UnsignedByte, interleaved.getFormat());
        assertEquals(32, interleaved.getData().limit());
    }
}
