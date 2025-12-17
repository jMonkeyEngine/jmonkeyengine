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

import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.util.IntEnum;

import java.util.Comparator;

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
public interface Mesh {

    <T extends Attribute> T mapAttribute(String name);

    void addLevelOfDetail(LodBuffer lod);

    LodBuffer selectLevelOfDetail(Comparator<LodBuffer> selector);

    int setElements(IntEnum<InputRate> rate, int elements);

    void pushElements(IntEnum<InputRate> rate, int baseElement, int elements);

    int getElements(IntEnum<InputRate> rate);

    int getCapacity(IntEnum<InputRate> rate);

    default <T extends Attribute> T mapAttribute(GlVertexBuffer.Type type) {
        return mapAttribute(type.name());
    }

    default void pushElements(IntEnum<InputRate> rate) {
        pushElements(rate, 0, getElements(rate));
    }

}
