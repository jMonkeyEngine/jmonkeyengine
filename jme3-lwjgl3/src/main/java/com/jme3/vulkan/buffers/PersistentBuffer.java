package com.jme3.vulkan.buffers;

import com.jme3.vulkan.LogicalDevice;
import com.jme3.vulkan.flags.MemoryFlags;
import com.jme3.vulkan.flags.BufferUsageFlags;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class PersistentBuffer extends GpuBuffer {

    private final long address;

    public PersistentBuffer(LogicalDevice device, int size, BufferUsageFlags usage, MemoryFlags mem, boolean concurrent) {
        super(device, size, usage, mem, concurrent);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            address = memory.map(stack, 0, size, 0).get(0);
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
