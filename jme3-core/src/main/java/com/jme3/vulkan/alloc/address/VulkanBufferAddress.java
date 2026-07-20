package com.jme3.vulkan.alloc.address;

import com.jme3.vulkan.buffer.Handle;

@Deprecated
public class VulkanBufferAddress implements DeviceAddress<Long> {

    private final Handle<Long> buffer;
    private final int offset;
    private final int size;

    public VulkanBufferAddress(Handle<Long> buffer, int offset, int size) {
        this.buffer = buffer;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public Handle<Long> handle() {
        return buffer;
    }

    @Override
    public int offset() {
        return offset;
    }

    @Override
    public VulkanBufferAddress slice(int offset, int size) {
        return new VulkanBufferAddress(buffer, this.offset + offset, size);
    }

    @Override
    public int size() {
        return size;
    }

}
