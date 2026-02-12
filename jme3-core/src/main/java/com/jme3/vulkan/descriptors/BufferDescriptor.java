package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.buffers.MappableBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

public class BufferDescriptor {

    private final MappableBuffer buffer;
    private final long offset, range;

    public BufferDescriptor(MappableBuffer buffer) {
        this(buffer, 0, buffer.size().getBytes());
    }

    public BufferDescriptor(MappableBuffer buffer, long offset, long range) {
        this.buffer = buffer;
        this.offset = offset;
        this.range = range;
    }

    public void fillDescriptorInfo(VkDescriptorBufferInfo info) {
        info.buffer(buffer.getId()).offset(offset).range(range);
    }

    public MappableBuffer getBuffer() {
        return buffer;
    }

    public long getOffset() {
        return offset;
    }

    public long getRange() {
        return range;
    }

}
