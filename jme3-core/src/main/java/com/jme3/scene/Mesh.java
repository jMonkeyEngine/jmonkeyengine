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

import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.mesh.*;
import com.jme3.vulkan.mesh.attribute.Attribute;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.util.IntEnum;

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

    MeshLayout getLayout();

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
     * Gets the vertex buffer data related to the vertex binding.
     *
     * @param binding binding related to the vertex buffer
     * @return vertex buffer data
     */
    VertexBuffer getVertexBuffer(VertexBinding binding);

    /**
     * Sets the index buffer for the level of detail. If no index buffer
     * is selected for rendering when this method is called, {@code buffer}
     * is selected for rendering.
     *
     * @param level level of detail
     * @param buffer index buffer
     */
    void setLevelOfDetail(int level, MappableBuffer buffer);

    /**
     * Selects the index buffer at or immediately below the specified level
     * of detail for rendering. The number of triangles represented by the
     * selected index buffer is determined by its {@link MappableBuffer#size() size}.
     * If no index buffer exists at or below the specified level, no index
     * buffer is selected for rendering.
     *
     * @param level level of detail
     * @return selected index buffer
     */
    MappableBuffer selectLevelOfDetail(int level);

    /**
     * Gets the index buffer at or immediately below the specified level
     * of detail, or null if none exists.
     *
     * @param level level of detail
     * @return index buffer
     */
    MappableBuffer getLevelOfDetail(int level);

    /**
     * Sets the number of elements for the specified input rate (i.e. vertices
     * or instances) that are rendered. The actual number used may be different
     * from {@code elements}, usually depending on the capacity for that input
     * rate. If set to zero or less than zero, implementations may choose to
     * skip rendering for this mesh.
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
     * {@link MappableBuffer#push(int, int) Pushes} the specified regions of all buffers
     * for the given input rate.
     *
     * @param rate rate of vertex buffer to push
     * @param baseElement first element to push
     * @param elements number of elements to push
     */
    void pushElements(IntEnum<InputRate> rate, int baseElement, int elements);

    /**
     * Gets the number of elements for the given input rate.
     *
     * @param rate rate to get number of elements of
     * @return number of elements
     */
    int getElements(IntEnum<InputRate> rate);

    /**
     * Gets the maximum number of elements for the given input rate.
     *
     * @param rate rate to get element capacity of
     * @return number of elements
     */
    int getCapacity(IntEnum<InputRate> rate);

    /**
     * Tests if the named attribute exists for this mesh.
     *
     * @param name attribute name
     * @return true if the attribute exists
     */
    boolean attributeExists(String name);

    /**
     * Gets the topology mode of this mesh.
     *
     * @return topology mode
     */
    IntEnum<Topology> getTopology();

    /**
     * {@link #mapAttribute(String) Maps} the named attribute.
     *
     * @param type attribute name
     * @return mapped attribute
     * @param <T> attribute type
     */
    default <T extends Attribute> T mapAttribute(GlVertexBuffer.Type type) {
        return mapAttribute(type.name());
    }

    /**
     * {@link #mapAttribute(String) Maps} and sets the {@link #setUsage(GlVertexBuffer.Type,
     * GlVertexBuffer.Usage) usage flag} of the named attribute.
     *
     * @param name attribute name
     * @param usage usage hint
     * @return mapped attribute
     * @param <T> attribute type
     */
    default <T extends Attribute> T mapAttribute(String name, GlVertexBuffer.Usage usage) {
        setUsage(name, usage);
        return mapAttribute(name);
    }

    /**
     * {@link #mapAttribute(String) Maps} and sets the {@link #setUsage(GlVertexBuffer.Type,
     * GlVertexBuffer.Usage) usage flag} of the named attribute.
     *
     * @param type attribute name
     * @param usage usage hint
     * @return mapped attribute
     * @param <T> attribute type
     */
    default <T extends Attribute> T mapAttribute(GlVertexBuffer.Type type, GlVertexBuffer.Usage usage) {
        setUsage(type, usage);
        return mapAttribute(type);
    }

    /**
     * {@link #mapAttribute(String) Maps} the named attribute and passes the mapped
     * attribute to a config Consumer.
     *
     * @param name attribute name
     * @param config Consumer accepting the attribute
     * @param <T> attribute type
     */
    default <T extends Attribute> void mapAttribute(String name, Consumer<T> config) {
        T attr = mapAttribute(name);
        if (attr != null) config.accept(attr);
    }

    /**
     * {@link #mapAttribute(String) Maps} the named attribute and passes the mapped
     * attribute to a config Consumer.
     *
     * @param type attribute name
     * @param config Consumer accepting the attribute
     * @param <T> attribute type
     */
    default <T extends Attribute> void mapAttribute(GlVertexBuffer.Type type, Consumer<T> config) {
        mapAttribute(type.name(), config);
    }

    /**
     * Sets the usage hint of the named attribute.
     *
     * @param type attribute name
     * @param usage usage hint
     * @see #setUsage(String, GlVertexBuffer.Usage)
     */
    default void setUsage(GlVertexBuffer.Type type, GlVertexBuffer.Usage usage) {
        setUsage(type.name(), usage);
    }

    /**
     * Pushes all elements for the given input rate.
     *
     * @param rate input rate to push
     * @see #pushElements(IntEnum, int, int)
     */
    default void pushElements(IntEnum<InputRate> rate) {
        pushElements(rate, 0, getElements(rate));
    }

    /**
     * Tests if the named attribute exists for this mesh.
     *
     * @param type attribute name
     * @return true if the attribute exists
     */
    default boolean attributeExists(GlVertexBuffer.Type type) {
        return attributeExists(type.name());
    }

}
