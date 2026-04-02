package com.jme3.vulkan.buffers.saving;

import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.alloc.BufferAllocRequest;
import com.jme3.vulkan.util.Flag;

public interface BufferAllocator <T extends MappableBuffer> {

    T allocate(long bytes, BufferAllocRequest<T> alloc);

    /**
     * Allocates a standard buffer based on the update hint specified.
     *
     * @param bytes number of bytes to allocate
     * @param usage what the buffer is to be used for
     * @param hint hints at how often the buffer will be updated
     * @return allocated buffer
     */
    T allocateStandard(long bytes, Flag<BufferUsage> usage, UpdateHint hint);

}
