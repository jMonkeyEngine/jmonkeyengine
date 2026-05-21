/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.debug;

import com.jme3.math.Vector3f;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.JmePlatform;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.IdxBuffer;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.pipeline.Topology;

import java.nio.ShortBuffer;

/**
 * Simple grid shape.
 *
 * @author Kirill Vainer
 */
public class Grid extends AdaptiveMesh {

    public Grid() {
    }

    /**
     * Creates a grid debug shape.
     *
     * @param xLines number of lines parallel to the X axis
     * @param yLines number of lines parallel to the Y axis
     * @param lineDist the separation between consecutive lines (in world units)
     */
    public Grid(int xLines, int yLines, float lineDist) {
        VertexBuffer<Vertex> buffer = new VertexBuffer<>(InputRate.Vertex, new Vertex(),
                JmePlatform.allocateStandardBuffer(1, BufferUsage.Vertex, UpdateHint.Static));
        addVertexBuffer(buffer);

        int lineCount = xLines + yLines;
        setVertexCount(lineCount * 6, 0);
        IdxBuffer index = new IdxBuffer(IndexType.UInt16, JmePlatform.allocateStandardBuffer(
                2L * lineCount * Short.BYTES, BufferUsage.Index, UpdateHint.Static));
        setBaseIndexBuffer(index);

        float xLineLen = (yLines - 1) * lineDist;
        float yLineLen = (xLines - 1) * lineDist;
        short curIndex = 0;

        try (StructMapping<Vertex> vertMap = buffer.map(); BufferMapping idxMap = index.map()) {
            Vertex v = vertMap.get();
            vertMap.sample(0);
            ShortBuffer shorts = idxMap.getShorts();
            // add lines along X
            for (int i = 0; i < xLines; i++) {
                float y = (i) * lineDist;
                v.position.set(v.position.alias().set(0, 0, y));
                vertMap.increment();
                v.position.set(v.position.alias().set(xLineLen, 0, y));
                vertMap.increment();
                shorts.put(curIndex++);
                shorts.put(curIndex++);
            }
            // add lines along Y
            for (int i = 0; i < yLines; i++) {
                float x = (i) * lineDist;
                v.position.set(v.position.alias().set(x, 0, 0));
                vertMap.increment();
                v.position.set(v.position.alias().set(x, 0, yLineLen));
                vertMap.increment();
                shorts.put(curIndex++);
                shorts.put(curIndex++);
            }
        }
        setTopology(Topology.LineList);
        updateBound();
    }

    private static class Vertex extends Struct<VertexAttr> {

        public final VertexAttr<Vector3f> position = new VertexAttr<>(CommonAttributes.Position, new Vector3f());

        public Vertex() {
            addFields(position);
        }

    }

}
