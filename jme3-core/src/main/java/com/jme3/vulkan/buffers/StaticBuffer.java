package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;

public class StaticBuffer extends StageableBuffer {

    public StaticBuffer(LogicalDevice device, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryProp> mem, boolean concurrent) {
        super(device, size, usage, mem, concurrent);
    }

    @Override
    public boolean run(CommandBuffer cmd, int frame) {
        if (dirtyRegion == null) {
            freeStagingBuffer();
        }
        return super.run(cmd, frame);
    }

}
