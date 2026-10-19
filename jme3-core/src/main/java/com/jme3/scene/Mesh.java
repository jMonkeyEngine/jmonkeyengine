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

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Vector3f;
import com.jme3.util.struct.FieldArray;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructArray;
import com.jme3.util.struct.StructField;
import com.jme3.vulkan.buffer.IdxBuffer;
import com.jme3.vulkan.mesh.VertexAttr;
import com.jme3.vulkan.mesh.VertexBuffer;
import com.jme3.vulkan.mesh.attributes.CommonAttributes;
import com.jme3.vulkan.pipeline.FaceWinding;
import com.jme3.vulkan.pipeline.Topology;

import java.util.Collection;

/**
 * Stores the vertex, index, and instance data for drawing indexed meshes.
 *
 * @author codex
 */
public interface Mesh {

    /**
     * Attribute name every mesh's position data must be stored under - every mesh is
     * required to have position data, unlike other attributes which are optional.
     */
    String POSITION_ATTRIBUTE = CommonAttributes.Position;

    void setIndexBuffer(IdxBuffer buffer);

    /**
     * Gets this mesh's required position data: the vertex buffer containing
     * {@link #POSITION_ATTRIBUTE}, paired with that attribute itself via
     * {@link FieldArray}. Every mesh is required to have position data, so
     * implementations must guarantee this never returns null.
     *
     * @return position field array
     */
    FieldArray getPositionField();

    void addVertexBuffer(StructArray<? extends Struct<VertexAttr>> vertexBuffer);

    Collection<StructArray<? extends Struct<VertexAttr>>> getVertexBuffers();

    Collection<StructArray<? extends Struct<VertexAttr>>> getInstanceBuffers();

    /**
     * Finds the {@link VertexBuffer} among {@link #getVertexBuffers()} that contains
     * {@code attribute}, e.g. one of the names in
     * {@link com.jme3.vulkan.mesh.attributes.CommonAttributes}.
     *
     * @param attribute attribute name
     * @return the vertex buffer containing {@code attribute}, or null if none does
     */
    default StructArray<? extends Struct<VertexAttr>> getVertexBuffer(String attribute) {
        for (StructArray<? extends Struct<VertexAttr>> vb : getVertexBuffers()) {
            for (VertexAttr a : vb.getStruct().getFields()) {
                if (attribute.equals(a.getName())) {
                    return vb;
                }
            }
        }
        for (StructArray<? extends Struct<VertexAttr>> vb : getInstanceBuffers()) {
            for (VertexAttr a : vb.getStruct().getFields()) {
                if (attribute.equals(a.getName())) {
                    return vb;
                }
            }
        }
        return null;
    }

    default int getVertexCount() {
        return getVertexBuffers().stream().findAny().map(StructArray::length).orElse(0);
    }

    default int getInstanceCount() {
        return getInstanceBuffers().stream().findAny().map(StructArray::length).orElse(0);
    }

    void setTopology(Topology topology);

    void setFaceWinding(FaceWinding winding);

    void setPrimitiveRestart(boolean primitiveRestart);

    BoundingVolume getBounds();

    Topology getTopology();

    FaceWinding getFaceWinding();

    boolean isPrimitiveRestart();

}
