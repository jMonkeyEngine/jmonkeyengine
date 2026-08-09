package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.alloc.BufferAllocator;
import com.jme3.vulkan.util.Flag;

public class Allocation {

    private static BufferAllocator bufferAllocator;

    public static EngineBuffer createDynamicBuffer(int capacity, Flag<BufferRole> roles) {
        return bufferAllocator.createDynamicBuffer(capacity, roles);
    }

    public static EngineBuffer createReadbackBuffer(int capacity, Flag<BufferRole> roles) {
        return bufferAllocator.createReadbackBuffer(capacity, roles);
    }

    public static EngineBuffer createLocalBuffer(int capacity, Flag<BufferRole> roles) {
        return bufferAllocator.createLocalBuffer(capacity, roles);
    }

    public static EngineBuffer createStagingBuffer(int capacity, Flag<BufferRole> roles) {
        return bufferAllocator.createStreamingBuffer(capacity, roles);
    }

}
