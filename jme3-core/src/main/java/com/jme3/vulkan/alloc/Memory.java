package com.jme3.vulkan.alloc;

import java.nio.ByteBuffer;

public interface Memory {

    /**
     * Maps this memory and registers the mapping with {@code arena} if
     * this memory is not already mapped.
     *
     * @param arena arena to handle the mapping
     * @return mapped byte buffer
     */
    ByteBuffer map(MappingArena arena);

    /**
     * Gets the current mapping of this memory.
     *
     * @return mapped byte buffer
     * @throws IllegalStateException if this memory has not been
     * mapped with a {@link MappingArena}
     */
    ByteBuffer map();

    /**
     * Stages a section of this memory for update, either to upload
     * to the GPU or other update operation.
     *
     * @param offset byte offset
     * @param size byte size
     */
    void stage(long offset, long size);

    /**
     * Stages all memory.
     */
    void stage();

}
