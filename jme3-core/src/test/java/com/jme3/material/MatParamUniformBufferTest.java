/*
 * Copyright (c) 2009-2026 jMonkeyEngine
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
package com.jme3.material;

import com.jme3.math.ColorRGBA;
import com.jme3.shader.Shader;
import com.jme3.shader.ShaderBufferBlock;
import com.jme3.shader.VarType;
import com.jme3.shader.bufferobject.BufferBindingPoints;
import com.jme3.shader.bufferobject.BufferObject;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MatParamUniformBufferTest {

    @Test
    public void parsesStd140MatParamBlock() {
        MatParamUniformBuffer.Layout layout = MatParamUniformBuffer.parseLayout(shaderSource());

        assertNotNull(layout);
        assertEquals(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME, layout.blockName);
        assertEquals(48, layout.size);
        assertEquals(0, layout.getMember("Color").offset);
        assertEquals(16, layout.getMember("Roughness").offset);
        assertEquals(32, layout.getMember("Weights").offset);
    }

    @Test
    public void parsesInstanceNamedMatParamBlock() {
        MatParamUniformBuffer.Layout layout = MatParamUniformBuffer.parseLayout("#version 330\n"
                + "layout(std140) uniform MatParams {\n"
                + "    vec4 Color;\n"
                + "} m_MatParams;\n");

        assertNotNull(layout);
        assertEquals("MatParams", layout.blockName);
        assertEquals(0, layout.getMember("Color").offset);
    }

    @Test
    public void writesInstanceNamedMatParamBlock() {
        String source = "#version 330\n"
                + "layout(std140) uniform MatParams {\n"
                + "    vec4 Color;\n"
                + "} m_MatParams;\n";
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-instance-test.frag", source, null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer();
        buffer.begin(shader);
        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Color", new ColorRGBA(1f, 0f, 0f, 1f)), false));
        buffer.finish(shader);

        ShaderBufferBlock block = shader.getBufferBlock("MatParams");
        assertSame(buffer.getBufferObject(), block.getBufferObject());
        assertEquals(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME, block.getBufferObject().getName());
    }

    @Test
    public void ignoresBlocksWithUnsupportedMembers() {
        MatParamUniformBuffer.Layout layout = MatParamUniformBuffer.parseLayout("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    vec4 Color;\n"
                + "    sampler2D Unsupported;\n"
                + "    float Roughness;\n"
                + "};\n");

        assertNull(layout);
    }

    @Test
    public void ignoresBlocksWithUnsupportedArrayDeclarators() {
        MatParamUniformBuffer.Layout layout = MatParamUniformBuffer.parseLayout("#version 330\n"
                + "#define WEIGHT_COUNT 4\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    float Weights[WEIGHT_COUNT];\n"
                + "};\n");

        assertNull(layout);
    }

    @Test
    public void ignoresBlocksWithExplicitMemberLayout() {
        MatParamUniformBuffer.Layout layout = MatParamUniformBuffer.parseLayout("#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    layout(offset = 32) vec4 Color;\n"
                + "};\n");

        assertNull(layout);
    }

    @Test
    public void writesOnlyBlockMembersToBufferObject() {
        Shader shader = new Shader();
        shader.addSource(Shader.ShaderType.Fragment, "mat-param-test.frag", shaderSource(), null, "GLSL330");

        MatParamUniformBuffer buffer = new MatParamUniformBuffer();
        buffer.begin(shader);

        assertTrue(buffer.set(new MatParam(VarType.Vector4, "Color", new ColorRGBA(1f, 0.5f, 0.25f, 1f)), false));
        assertTrue(buffer.set(new MatParam(VarType.Float, "Roughness", 0.75f), false));
        assertFalse(buffer.set(new MatParam(VarType.Float, "Outside", 1f), false));
        buffer.finish(shader);

        ShaderBufferBlock block = shader.getBufferBlock(BufferBindingPoints.MAT_PARAMS_BLOCK_NAME);
        BufferObject bo = block.getBufferObject();
        assertSame(buffer.getBufferObject(), bo);
        assertEquals(ShaderBufferBlock.BufferType.UniformBufferObject, block.getType());

        ByteBuffer data = bo.getByteData();
        assertEquals(1f, data.getFloat(0), 0.0001f);
        assertEquals(0.5f, data.getFloat(4), 0.0001f);
        assertEquals(0.25f, data.getFloat(8), 0.0001f);
        assertEquals(1f, data.getFloat(12), 0.0001f);
        assertEquals(0.75f, data.getFloat(16), 0.0001f);
    }

    private static String shaderSource() {
        return "#version 330\n"
                + "layout(std140) uniform m_MatParams {\n"
                + "    vec4 Color;\n"
                + "    float Roughness;\n"
                + "    float Weights[1];\n"
                + "};\n"
                + "void main() {}\n";
    }
}
