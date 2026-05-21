/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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
package com.jme3.scene;

import com.jme3.export.Savable;
import com.jme3.vulkan.buffers.IdxBuffer;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attributes.AttributeMapping;
import com.jme3.vulkan.mesh.VertexBuffer;
import com.jme3.vulkan.pipeline.FaceWinding;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.pipeline.VertexPipeline;

import java.util.Collection;
import java.util.function.Function;

/**
 * Stores the vertex, index, and instance data for drawing indexed meshes.
 *
 * <p>Each mesh is made up of at least one vertex buffer, at least one
 * index buffer, and at least one vertex attribute. Vertex attributes are
 * stored in vertex buffers and are identified by string. Implementations
 * are free to format the vertex attributes among the vertex buffers however
 * they choose, but the format and number of each vertex attributes' components
 * must be as previously or externally specified.</p>
 *
 * @author codex
 */
public interface Mesh extends Savable {

    VertexInput declareVertexInput(Function<String, Integer> attributeMapper);

    int selectLevelOfDetail(int lod);

    void setIndexBuffer(int lod, IdxBuffer buffer);

    IdxBuffer getIndexBuffer(int lod);

    IdxBuffer getLevelOfDetail(int lod);

    void addVertexBuffer(VertexBuffer vertexBuffer);

    Collection<VertexBuffer> getVertexBuffers();

    AttributeMapping mapAttributes(InputRate rate, String... attributes);

    int setVertexCount(int vertices);

    int setInstanceCount(int instances);

    void setVertexCapacity(int vertexCapacity);

    void setInstanceCapacity(int instanceCapacity);

    void setTopology(Topology topology);

    void setFaceWinding(FaceWinding winding);

    void setPrimitiveRestart(boolean primitiveRestart);

    int getVertexCount();

    int getInstanceCount();

    int getVertexCapacity();

    int getInstanceCapacity();

    Topology getTopology();

    FaceWinding getFaceWinding();

    boolean isPrimitiveRestart();

    default void setBaseIndexBuffer(IdxBuffer buffer) {
        setIndexBuffer(0, buffer);
    }

    default IdxBuffer getBaseIndexBuffer() {
        return getIndexBuffer(0);
    }

    default void setVertexCount(int vertices, int resizePadding) {
        if (resizePadding >= 0 && vertices > getVertexCapacity()) {
            setVertexCapacity(vertices + resizePadding);
        }
        setVertexCount(vertices);
    }

    default void setInstanceCount(int instances, int resizePadding) {
        if (resizePadding >= 0 && instances > getInstanceCapacity()) {
            setInstanceCapacity(instances + resizePadding);
        }
        setInstanceCount(instances);
    }

    default int getElementCount(InputRate rate) {
        switch (rate) {
            case Vertex: return getVertexCount();
            case Instance: return getInstanceCount();
            default: return 0;
        }
    }

    default int getElementCapacity(InputRate rate) {
        switch (rate) {
            case Vertex: return getVertexCapacity();
            case Instance: return getInstanceCapacity();
            default: return 0;
        }
    }

}
