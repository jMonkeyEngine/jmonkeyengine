package com.jme3.vulkan;

import com.jme3.backend.Engine;
import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.saving.BufferAllocator;
import com.jme3.vulkan.buffers.saving.UpdateHint;
import com.jme3.vulkan.util.Flag;

public class JmePlatform {

    private static Engine engine;
    private static BufferAllocator allocator;

    public static void setEngine(Engine engine) {
        JmePlatform.engine = engine;
    }

    public static Engine getEngine() {
        return engine;
    }

    public static MappableBuffer allocateStandardBuffer(long bytes, Flag<BufferUsage> usage, UpdateHint update) {
        return engine.allocateStandard(bytes, usage, update);
    }

}
