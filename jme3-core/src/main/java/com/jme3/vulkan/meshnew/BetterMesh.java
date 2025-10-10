package com.jme3.vulkan.meshnew;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.mesh.AttributeModifier;

/**
 * Stores the vertex and index data for drawing indexed meshes.
 *
 * <p>Each mesh is made up of at least one vertex buffer, at least one
 * index buffer, and at least one vertex attribute. Vertex attributes are
 * stored in vertex buffers and are identified by string. Implementations
 * are free to format the vertex attributes among the vertex buffers however
 * they choose, but the format and number of each vertex attributes' components
 * must be as previously or externally specified.</p>
 *
 * <p>The base level of detail (LOD) index buffer at index 0 must always be
 * non-null. If a LOD is requested that does not yet exist, that LOD must
 * be created as a result (except when {@link #draw(RenderManager, CommandBuffer, Geometry, int)
 * drawing}).</p>
 */
public interface BetterMesh {

    /**
     * Submits commands to draw this mesh in context with {@code geometry}.
     * Only indices for the specified {@link #setTriangleCount(int, int)
     * triangle count} are submitted. For vertex data, implementations
     * must submit at least the specified {@link #setVertexCount(int)
     * vertex count} amount of vertex data, but may submit more than
     * that if convenient.
     *
     * @param renderManager render manager
     * @param cmd           command buffer to submit to (null for opengl rendering)
     * @param geometry      geometry to draw with
     * @param lod           level of detail (LOD) to draw (must be non-negative)
     */
    void draw(RenderManager renderManager, CommandBuffer cmd, Geometry geometry, int lod);

    /**
     * Creates an AttributeModifier with which to modify the vertex data
     * of the named attribute. If the named attribute does not exist,
     * the implementation may either throw an IllegalArgumentException or
     * create a new attribute under {@code attributeName}.
     *
     * @param attributeName attribute to modify
     * @return modifier
     * @throws IllegalArgumentException if {@code namedAttribute} does not
     * correspond to an existing attribute and the implementation is unable
     * to create a new attribute.
     */
    AttributeModifier modify(String attributeName);

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
    void setAccessFrequency(String attributeName, AccessRate hint);

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
     * @param instances
     */
    void setInstanceCount(int instances);

}
