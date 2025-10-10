package com.jme3.vulkan.buffers.generate;

import com.jme3.util.NioBuffer;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.frames.SingleResource;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public class NioBufferGenerator implements BufferGenerator<NioBuffer> {

    @Override
    public VersionedResource<? extends NioBuffer> createStreamingBuffer(MemorySize size, Flag<BufferUsage> usage) {
        return createBuffer(size);
    }

    @Override
    public VersionedResource<? extends NioBuffer> createDynamicBuffer(MemorySize size, Flag<BufferUsage> usage) {
        return createBuffer(size);
    }

    @Override
    public VersionedResource<? extends NioBuffer> createStaticBuffer(MemorySize size, Flag<BufferUsage> usage) {
        return createBuffer(size);
    }

    private VersionedResource<? extends NioBuffer> createBuffer(MemorySize size) {
        return new SingleResource<>(new NioBuffer(size));
    }

}
