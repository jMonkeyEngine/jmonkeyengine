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
import com.jme3.collision.Collidable;
import com.jme3.collision.CollisionResults;
import com.jme3.export.*;
import com.jme3.math.Triangle;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.mesh.AttributeModifier;
import com.jme3.vulkan.mesh.DataAccess;
import com.jme3.vulkan.mesh.exp.Vertex;
import com.jme3.vulkan.pipeline.Topology;
import com.jme3.vulkan.util.IntEnum;

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

    Vertex getVertices();

    /**
     * Creates an AttributeModifier with which to modify the vertex data
     * of the named attribute. If the named attribute has not been initialized,
     * it will be initialized here if possible.
     *
     * <p>The returned modifier must be properly closed after use.</p>
     *
     * @param attributeName attribute to modify
     * @return modifier
     * @throws IllegalArgumentException if {@code namedAttribute} does not
     * correspond to an existing attribute and the implementation is unable
     * to create a new attribute.
     */
    AttributeModifier modify(String attributeName);

    boolean attributeExists(String attributeName);

    /**
     * Gets the index buffer for the specified level of detail (LOD).
     *
     * @param lod the level of detail to fetch from
     * @return index buffer for {@code lod}, or null if no such buffer exists
     */
    GpuBuffer getIndices(int lod);

    /**
     * Hints at how often the named attribute will be modified by the host.
     * If {@code hint's} ordinal is less than the attribute's vertex buffer's
     * current access hint ordinal (indicating more accesses), then the vertex
     * buffer is replaced with one optimized for higher frequency accesses.
     *
     * @param attributeName name of the attribute the hint applies to
     * @param hint access frequency hint (lower ordinal is more optimized
     *             for frequent accesses)
     */
    void setAccessFrequency(String attributeName, DataAccess hint);

    /**
     * Sets the minimum number of vertices that must be supported by
     * this mesh's vertex buffers.
     *
     * <p>If the current vertex buffers are unable to support the minimum
     * count as a result of this method, then the current vertex data is
     * copied to a functionally identical set of larger vertex buffers.
     * Implementations are free to anticipate demand for more vertices
     * by creating larger vertex buffers than are currently necessary.</p>
     *
     * @param vertices minimum number of vertices that must be supported
     */
    void setVertexCount(int vertices);

    /**
     * Sets the minimum number of triangles that must be supported
     * by the LOD's index buffer. For rendering purposes, only
     * {@code triangles} number of triangles are drawn.
     *
     * <p>If the current index buffer is unable to support the minimum
     * count as a result of this method, then the current index data is
     * copied to a larger (but functionally identical) index buffer.
     * Implementations are free to anticipate demand for more triangles
     * by creating larger index buffers than are currently necessary.</p>
     *
     * @param lod level of detail (LOD) index
     * @param triangles minimum number of triangles that must be supported
     */
    void setTriangleCount(int lod, int triangles);

    /**
     * Sets the minimum number of instances that must be supported by this
     * mesh's {@link com.jme3.vulkan.mesh.InputRate#Instance per-instance}
     * vertex buffers.
     *
     * @param instances number of instances on this mesh
     */
    void setInstanceCount(int instances);

    /**
     * Gets the vertex count as specified by {@link #setVertexCount(int)}.
     *
     * @return vertex count
     */
    int getVertexCount();

    /**
     * Gets the triangle count for the specified LOD. If the LOD
     * does not exist, zero is returned.
     *
     * @param lod level of detail
     * @return number of triangles for that LOD
     */
    int getTriangleCount(int lod);

    /**
     * Gets the instance count as specified by {@link #setInstanceCount(int)}.
     *
     * @return instance count
     */
    int getInstanceCount();

    /**
     * Collides with {@code other} in the context of {@code geometry}. Collisions
     * are stored in {@code results}.
     *
     * @param other collidable to test collisions with
     * @param geometry geometry specifying the world bound and matrix
     * @param results collision results to store the collisions in
     * @return number of collisions that occured
     */
    int collideWith(Collidable other, Geometry geometry, CollisionResults results);

    /**
     * Updates the bounding volume to contain all vertices of this mesh in model space.
     */
    void updateBound();

    /**
     * Sets the bounding volume to be used by this mesh.
     *
     * @param volume volume to use
     */
    void setBound(BoundingVolume volume);

    /**
     * Gets the bounding volume used by this mesh.
     *
     * @return bounding volume
     */
    BoundingVolume getBound();

    /**
     * Sets the topology format of the index buffers.
     *
     * @param topology topology
     */
    void setTopology(IntEnum<Topology> topology);

    /**
     *
     * @return topology
     */
    IntEnum<Topology> getTopology();

    /**
     * Gets the number of defined LOD levels.
     *
     * @return number of LOD levels
     */
    int getNumLodLevels();

    /**
     * Fetches that positional data of the requested triangle.
     *
     * @param triangleIndex triangle to fetch
     * @param store stores the result (if null, a new Triangle is created)
     * @return triangle respresenting the mesh triangle
     */
    Triangle getTriangle(int triangleIndex, Triangle store);

    /**
     * Gets the indices pointing to the vertices of the requested triangle.
     *
     * @param triangleIndex triangle to fetch
     * @param store array storing the indices (must have at least 3 elements or be null)
     * @return array storing the indices
     */
    int[] getTriangle(int triangleIndex, int[] store);

    /* ----- DEFAULTS ----- */

    /**
     * Modifies the named attribute.
     *
     * @param name name of the attribute
     * @return modifier
     * @see #modify(String)
     */
    default AttributeModifier modify(GlVertexBuffer.Type name) {
        return modify(name.getName());
    }

    /**
     * Modifies the position attribute.
     *
     * @return modifier
     * @see #modify(String)
     */
    default AttributeModifier modifyPosition() {
        return modify(GlVertexBuffer.Type.Position.name());
    }

    /**
     * Gets the triangle count for the base LOD level (0).
     *
     * @return triangel count for the base LOD
     */
    default int getTriangleCount() {
        return getTriangleCount(0);
    }

    default boolean attributeExists(GlVertexBuffer.Type type) {
        return attributeExists(type.name());
    }

    default boolean isAnimated() {
        return attributeExists(GlVertexBuffer.Type.BoneIndex)
                || attributeExists(GlVertexBuffer.Type.HWBoneIndex);
    }

    /* ----- COMPATIBILITY WITH OLD MESH ----- */

    @Deprecated
    default void setMode(IntEnum<Topology> topology) {
        setTopology(topology);
    }

    @Deprecated
    default IntEnum<Topology> getMode() {
        return getTopology();
    }

    // todo: determine what to do with this method
    // It's used only seriously by Joint, and only then for attachment nodes.
    // I hope it can be replaced by something else or attachment nodes could
    // be refactored to remove that dependency, because the GlMesh implementation
    // for this is concerning.
    @Deprecated
    default boolean isAnimatedByJoint(int i) {
        return false;
    }

}
