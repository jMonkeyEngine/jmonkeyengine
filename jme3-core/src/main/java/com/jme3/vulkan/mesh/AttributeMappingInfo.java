package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.MappableBuffer;

public class AttributeMappingInfo {

    private final VertexBinding binding;
    private final MappableBuffer vertices;
    private final long size, offset;

    public AttributeMappingInfo(VertexBinding binding, MappableBuffer vertices, long size, long offset) {
        this.binding = binding;
        this.vertices = vertices;
        this.size = size;
        this.offset = offset;
    }

    /**
     * Gets the vertex buffer binding of the attribute.
     *
     * @return vertex binding
     */
    public VertexBinding getBinding() {
        return binding;
    }

    /**
     * Gets the vertex buffer data of the attribute.
     *
     * @return vertex data
     */
    public MappableBuffer getVertices() {
        return vertices;
    }

    /**
     * Gets the number of vertices in the attribute.
     *
     * @return number of vertices
     */
    public long getSize() {
        return size;
    }

    /**
     * Gets the attribute byte offset from the beginning of a vertex.
     *
     * @return byte offset from the beginning of a vertex
     */
    public long getOffset() {
        return offset;
    }

}
