package com.jme3.vulkan.alloc;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.util.HashMap;
import java.util.Map;

public class VulkanBufferManager {

    private final Map<Long, BufferStuff> stuffs = new HashMap<>();

    public long getVulkanId(NativeMemory mem) {
        // native handle is vulkan buffer ID for this manager implementation
        return mem.getNativeHandle();
    }

    public void push(NativeMemory mem) {

    }

    private static class BufferStuff {

        private Flag<MemoryProp> memProps;

    }

}
