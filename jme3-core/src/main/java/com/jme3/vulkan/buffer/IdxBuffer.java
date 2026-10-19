package com.jme3.vulkan.buffer;

import com.jme3.vulkan.mesh.IndexType;

/**
 * Associates a {@link GpuBuffer} holding index data with the {@link IndexType} those
 * indices are packed as.
 */
public class IdxBuffer {

    private final IndexType type;
    private final GpuBuffer buffer;

    public IdxBuffer(IndexType type, GpuBuffer buffer) {
        this.type = type;
        this.buffer = buffer;
    }

    /**
     * Gets the type each index in {@link #getBuffer()} is packed as.
     *
     * @return index type
     */
    public IndexType getIndexType() {
        return type;
    }

    /**
     * Gets the buffer backing this index buffer.
     *
     * @return backing buffer
     */
    public GpuBuffer getBuffer() {
        return buffer;
    }

    /**
     * Gets how many indices {@link #getBuffer()} has room for, derived from its byte
     * capacity and {@link #getIndexType()}.
     *
     * @return index count
     */
    public int getIndexCount() {
        return buffer.capacity() / type.getBytes();
    }

}
