package com.jme3.vulkan.buffers.generate;

import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.AccessFrequency;
import com.jme3.vulkan.util.Flag;

public interface BufferGenerator <T extends GpuBuffer> {

    T createStreamingBuffer(MemorySize size, Flag<BufferUsage> usage);

    T createDynamicBuffer(MemorySize size, Flag<BufferUsage> usage);

    T createStaticBuffer(MemorySize size, Flag<BufferUsage> usage);

    default T createBuffer(MemorySize size, Flag<BufferUsage> usage, AccessFrequency access) {
        switch (access) {
            case Stream: return createStreamingBuffer(size, usage);
            case Dynamic: return createDynamicBuffer(size, usage);
            case Static: return createStaticBuffer(size, usage);
            default: throw new UnsupportedOperationException(access + " access frequency is not supported.");
        }
    }

}
