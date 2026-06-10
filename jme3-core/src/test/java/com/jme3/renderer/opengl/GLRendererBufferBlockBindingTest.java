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
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.renderer.opengl;

import com.jme3.shader.ShaderBufferBlock;
import com.jme3.shader.ShaderBufferBlock.BufferType;
import com.jme3.shader.bufferobject.BufferBindingPoints;
import com.jme3.shader.bufferobject.BufferObject;
import com.jme3.util.ListMap;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GLRendererBufferBlockBindingTest {

    @Test
    public void testDetectsUnresolvedBlockAfterResolvedBlock() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        blocks.put("Resolved", block("Resolved", BufferType.UniformBufferObject, 0, 0));
        blocks.put("Unresolved", block("Unresolved", BufferType.UniformBufferObject, 1, -1));

        assertTrue(GLRenderer.hasUnresolvedBufferBlockBindings(blocks));
    }

    @Test
    public void testIgnoresUnconfiguredBlocksDuringUnresolvedScan() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        ShaderBufferBlock block = new ShaderBufferBlock();
        block.setName("Unconfigured");
        block.setLocation(0);
        blocks.put(block.getName(), block);

        assertFalse(GLRenderer.hasUnresolvedBufferBlockBindings(blocks));
    }

    @Test
    public void testUboEngineBindingsUseReservedRange() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        blocks.put(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME,
                block(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME, BufferType.UniformBufferObject, 0, 0));
        blocks.put("UserBlock", block("UserBlock", BufferType.UniformBufferObject, 1, 35));

        GLRenderer.resolveBufferBlockBindingCollisions(blocks, 36, 36);

        assertEquals(29, blocks.get(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME).getBinding());
        assertEquals(0, blocks.get("UserBlock").getBinding());
    }

    @Test
    public void testSsboCanUseFullBindingRange() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        blocks.put("Data", block("Data", BufferType.ShaderStorageBufferObject, 0, 35));

        GLRenderer.resolveBufferBlockBindingCollisions(blocks, 36, 36);

        assertEquals(35, blocks.get("Data").getBinding());
    }

    @Test
    public void testMatParamInstanceBlockUsesEngineBinding() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        ShaderBufferBlock block = block("MatParams", BufferType.UniformBufferObject, 0, 0);
        block.getBufferObject().setName(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME);
        blocks.put("MatParams", block);

        GLRenderer.resolveBufferBlockBindingCollisions(blocks, 36, 36);

        assertEquals(29, block.getBinding());
    }

    @Test
    public void testUboAndSsboBindingsAreSeparateNamespaces() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        blocks.put("Params", block("Params", BufferType.UniformBufferObject, 0, 1));
        blocks.put("Data", block("Data", BufferType.ShaderStorageBufferObject, 0, 1));

        GLRenderer.resolveBufferBlockBindingCollisions(blocks, 36, 36);

        assertEquals(1, blocks.get("Params").getBinding());
        assertEquals(1, blocks.get("Data").getBinding());
    }

    @Test
    public void testOutOfRangeUboBindingIsReassigned() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        blocks.put("Params", block("Params", BufferType.UniformBufferObject, 0, 99));

        GLRenderer.resolveBufferBlockBindingCollisions(blocks, 36, 36);

        assertEquals(0, blocks.get("Params").getBinding());
    }

    @Test
    public void testOutOfRangeSsboBindingIsReassigned() {
        ListMap<String, ShaderBufferBlock> blocks = new ListMap<>();
        blocks.put("Data", block("Data", BufferType.ShaderStorageBufferObject, 0, 99));

        GLRenderer.resolveBufferBlockBindingCollisions(blocks, 36, 36);

        assertEquals(0, blocks.get("Data").getBinding());
    }

    @Test
    public void testReadbackStoreIsClampedToBufferSize() {
        BufferObject bufferObject = new BufferObject();
        bufferObject.initializeEmpty(8);
        ByteBuffer store = ByteBuffer.allocateDirect(16);

        int readLength = GLRenderer.prepareBufferReadbackStore(bufferObject, store);

        assertEquals(8, readLength);
        assertEquals(0, store.position());
        assertEquals(8, store.limit());
    }

    @Test
    public void testReadbackStoreCanBeSmallerThanBuffer() {
        BufferObject bufferObject = new BufferObject();
        bufferObject.initializeEmpty(16);
        ByteBuffer store = ByteBuffer.allocateDirect(8);

        int readLength = GLRenderer.prepareBufferReadbackStore(bufferObject, store);

        assertEquals(8, readLength);
        assertEquals(0, store.position());
        assertEquals(8, store.limit());
    }

    private static ShaderBufferBlock block(String name, BufferType type, int location, int binding) {
        ShaderBufferBlock block = new ShaderBufferBlock();
        block.setName(name);
        block.setLocation(location);
        block.setBinding(binding);
        block.setBufferObject(type, new BufferObject());
        return block;
    }
}
