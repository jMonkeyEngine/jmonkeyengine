package com.jme3.vulkan.buffers;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryFlag;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class PersistentBuffer extends VulkanBuffer {

    private final long address;

    public PersistentBuffer(LogicalDevice device, MemorySize size, Flag<BufferUsage> usage, Flag<MemoryFlag> mem, boolean concurrent) {
        super(device, size, usage, mem, concurrent);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            address = memory.map(stack, 0, size.getBytes(), 0).get(0);
        }
    }

    @Override
    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        return stack.pointers(address);
    }

    @Override
    public void unmap() {}

    public long getAddress() {
        return address;
    }

}
