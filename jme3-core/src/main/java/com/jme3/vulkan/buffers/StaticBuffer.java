package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public class StaticBuffer extends StageableBuffer {

    public StaticBuffer(LogicalDevice device, MemorySize size) {
        super(device, size);
    }

    @Override
    public void run(CommandBuffer cmd, int frame) {
        if (dirtyRegion == null) {
            freeStagingBuffer();
        }
        super.run(cmd, frame);
    }

}
