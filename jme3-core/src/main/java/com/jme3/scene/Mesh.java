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
import java.util.function.Consumer;

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

    /**
     * Creates and returns an {@link Attribute} object which maps to the
     * specified vertex attribute, regardless of where and how the attribute
     * is stored in the mesh.
     *
     * @param name name of the attribute to map
     * @return Attribute object
     * @param <T> type of Attribute object
     */
    <T extends Attribute> T mapAttribute(String name);

    /**
     * Adds the level of detail index buffer to this mesh. The LOD buffer
     * that is used for rendering is selected via {@link #selectLevelOfDetail(Comparator)}.
     * If no LOD buffers are provided, primitives will be automatically formed
     * from the vertex buffers.
     *
     * @param lod level of detail buffer to add
     */
    void addLevelOfDetail(LodBuffer lod);

    /**
     * Selects the level of detail buffer to use for rendering.
     *
     * @param selector chooses the LOD buffer from this mesh to render with
     * @return the selected LOD buffer
     */
    LodBuffer selectLevelOfDetail(Comparator<LodBuffer> selector);

    /**
     * Sets the number of elements for the specified input rate.
     *
     * <p>For example, to set the number of vertices:</p>
     * <pre><code>mesh.setElements(InputRate.Vertex, 120);</code></pre>
     *
     * <p>The number of elements may not necessarily be equal to {@code elements}
     * after this method call.</p>
     *
     * @param rate vertex rate
     * @param elements number of elements associated with that vertex rate
     * @return the number of elements used for the vertex rate as a result
     * of this method call
     */
    int setElements(IntEnum<InputRate> rate, int elements);

    /**
     * Sets the usage hint of the specified attribute. The effectiveness of
     * a usage hint depends on the exact implementation. It is generally
     * most effective to set an attribute's usage hint before the attribute's
     * vertex buffer is first interacted with.
     *
     * @param attributeName name of the attribute
     * @param usage usage hint
     */
    void setUsage(String attributeName, GlVertexBuffer.Usage usage);

    /**
     *
     * @param rate
     * @param baseElement
     * @param elements
     */
    void pushElements(IntEnum<InputRate> rate, int baseElement, int elements);

    /**
     *
     * @param rate
     * @return
     */
    int getElements(IntEnum<InputRate> rate);

    /**
     *
     * @param rate
     * @return
     */
    int getCapacity(IntEnum<InputRate> rate);

    /**
     *
     * @param name
     * @return
     */
    boolean attributeExists(String name);

    /**
     *
     * @param type
     * @return
     * @param <T>
     */
    default <T extends Attribute> T mapAttribute(GlVertexBuffer.Type type) {
        return mapAttribute(type.name());
    }

    /**
     *
     * @param name
     * @param usage
     * @return
     * @param <T>
     */
    default <T extends Attribute> T mapAttribute(String name, GlVertexBuffer.Usage usage) {
        setUsage(name, usage);
        return mapAttribute(name);
    }

    /**
     *
     * @param type
     * @param usage
     * @return
     * @param <T>
     */
    default <T extends Attribute> T mapAttribute(GlVertexBuffer.Type type, GlVertexBuffer.Usage usage) {
        setUsage(type, usage);
        return mapAttribute(type);
    }

    /**
     *
     * @param name
     * @param config
     * @return
     * @param <T>
     */
    default <T extends Attribute> void mapAttribute(String name, Consumer<T> config) {
        T attr = mapAttribute(name);
        if (attr != null) config.accept(attr);
    }

    /**
     *
     * @param type
     * @param config
     * @param <T>
     */
    default <T extends Attribute> void mapAttribute(GlVertexBuffer.Type type, Consumer<T> config) {
        mapAttribute(type.name(), config);
    }

    /**
     *
     * @param type
     * @param usage
     */
    default void setUsage(GlVertexBuffer.Type type, GlVertexBuffer.Usage usage) {
        setUsage(type.name(), usage);
    }

    /**
     *
     * @param rate
     */
    default void pushElements(IntEnum<InputRate> rate) {
        pushElements(rate, 0, getElements(rate));
    }

    /**
     *
     * @param type
     * @return
     */
    default boolean attributeExists(GlVertexBuffer.Type type) {
        return attributeExists(type.name());
    }

}
