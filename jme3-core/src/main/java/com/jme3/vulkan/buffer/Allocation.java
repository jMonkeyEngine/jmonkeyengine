package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.alloc.MemoryAllocator;
import com.jme3.vulkan.util.Flag;

public class Allocation {

    private static MemoryAllocator memoryAllocator;

    public static GpuBuffer createDynamicBuffer(int capacity, Flag<GpuBuffer.Role> roles) {
        return memoryAllocator.createDynamicBuffer(capacity, roles);
    }

    public static GpuBuffer createReadbackBuffer(int capacity, Flag<GpuBuffer.Role> roles) {
        return memoryAllocator.createReadbackBuffer(capacity, roles);
    }

    public static GpuBuffer createLocalBuffer(int capacity, Flag<GpuBuffer.Role> roles) {
        return memoryAllocator.createLocalBuffer(capacity, roles);
    }

    public static GpuBuffer createStagingBuffer(int capacity, Flag<GpuBuffer.Role> roles) {
        return memoryAllocator.createStreamingBuffer(capacity, roles);
    }

}
