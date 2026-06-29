/*
 * Copyright (c) 2009-2019 jMonkeyEngine
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

package com.jme3.scene.debug.custom;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.buffers.IdxBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.tmp.SerializationOnly;

public class JointShape extends AdaptiveMesh {

    /**
     * Serialization only. Do not use.
     */
    @SerializationOnly
    protected JointShape() {}

    public static JointShape create() {
        JointShape shape = new JointShape();
        shape.setVertexCount(4, 0);
        shape.setInstanceCount(1, 0);
        VertexBuffer<Vertex> buffer = new VertexBuffer<>(InputRate.Vertex, new Vertex(),
                JmePlatform.allocateStandardBuffer(1, BufferUsage.Vertex, UpdateHint.Static));
        shape.addVertexBuffer(buffer);
        float width = 1;
        float height = 1;
        try (StructMapping<Vertex> m = buffer.map()) {
            Vertex v = m.get();
            m.sample(0);
            v.position.set(v.position.alias().set(-width * 0.5f, -height * 0.5f, 0));
            v.texCoord.set(v.texCoord.alias().set(0, 0));
            v.normal.set(Vector3f.UNIT_Z);
            v.color.set(ColorRGBA.White);
            m.increment();
            v.position.set(v.position.alias().set(width * 0.5f, -height * 0.5f, 0));
            v.texCoord.set(v.texCoord.alias().set(1, 0));
            v.normal.set(Vector3f.UNIT_Z);
            v.color.set(ColorRGBA.White);
            m.increment();
            v.position.set(v.position.alias().set(width * 0.5f, height * 0.5f, 0));
            v.texCoord.set(v.texCoord.alias().set(1, 1));
            v.normal.set(Vector3f.UNIT_Z);
            v.color.set(ColorRGBA.White);
            m.increment();
            v.position.set(v.position.alias().set(-width * 0.5f, height * 0.5f, 0));
            v.texCoord.set(v.texCoord.alias().set(0, 1));
            v.normal.set(Vector3f.UNIT_Z);
            v.color.set(ColorRGBA.White);
        }
        IdxBuffer index = new IdxBuffer(IndexType.UInt16, JmePlatform.allocateStandardBuffer(
                6L * Short.BYTES, BufferUsage.Index, UpdateHint.Static));
        shape.setBaseIndexBuffer(index);
        try (BufferMapping m = index.map()) {
            m.getShorts().put(new short[]{0, 1, 2, 0, 2, 3});
        }
        shape.updateBound();
        return shape;
    }

    private static class Vertex extends Struct<VertexAttr> {

        public final VertexAttr<Vector3f> position = new VertexAttr<>(CommonAttributes.Position, new Vector3f());
        public final VertexAttr<Vector2f> texCoord = new VertexAttr<>(CommonAttributes.TexCoord, new Vector2f());
        public final VertexAttr<Vector3f> normal = new VertexAttr<>(CommonAttributes.Normal, new Vector3f());
        public final VertexAttr<ColorRGBA> color = new VertexAttr<>(CommonAttributes.Color, new ColorRGBA());

        public Vertex() {
            addFields(position, texCoord, normal, color);
        }

    }

}
