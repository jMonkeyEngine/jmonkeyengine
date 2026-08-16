package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.util.Flag;

public class Allocation {

    private static MemoryAllocator memoryAllocator;

    public static EngineBuffer createDynamicBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return memoryAllocator.createDynamicBuffer(capacity, roles);
    }

    public static EngineBuffer createReadbackBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return memoryAllocator.createReadbackBuffer(capacity, roles);
    }

    public static EngineBuffer createLocalBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return memoryAllocator.createLocalBuffer(capacity, roles);
    }

    public static EngineBuffer createStagingBuffer(int capacity, Flag<EngineBuffer.Role> roles) {
        return memoryAllocator.createStreamingBuffer(capacity, roles);
    }

}
