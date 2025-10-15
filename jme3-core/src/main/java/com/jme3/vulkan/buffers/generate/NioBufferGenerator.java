package com.jme3.vulkan.buffers.generate;

import com.jme3.vulkan.buffers.NioBuffer;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public class NioBufferGenerator implements BufferGenerator<NioBuffer> {

    @Override
    public NioBuffer createStreamingBuffer(MemorySize size, Flag<BufferUsage> usage) {
        return createBuffer(size);
    }

    @Override
    public NioBuffer createDynamicBuffer(MemorySize size, Flag<BufferUsage> usage) {
        return createBuffer(size);
    }

    @Override
    public NioBuffer createStaticBuffer(MemorySize size, Flag<BufferUsage> usage) {
        return createBuffer(size);
    }

    private NioBuffer createBuffer(MemorySize size) {
        return new NioBuffer(size);
    }

}
