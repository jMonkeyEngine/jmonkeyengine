package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.buffers.GpuBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

public class BufferDescriptor {

    private final GpuBuffer buffer;
    private final long offset, range;

    public BufferDescriptor(GpuBuffer buffer) {
        this(buffer, 0, buffer.size().getBytes());
    }

    public BufferDescriptor(GpuBuffer buffer, long offset, long range) {
        this.buffer = buffer;
        this.offset = offset;
        this.range = range;
    }

    public void fillDescriptorInfo(VkDescriptorBufferInfo info) {
        info.buffer(buffer.getNativeObject()).offset(offset).range(range);
    }

    public GpuBuffer getBuffer() {
        return buffer;
    }

    public long getOffset() {
        return offset;
    }

    public long getRange() {
        return range;
    }

}
