package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;

public class BufferPartition <T extends MappableBuffer> implements MappableBuffer {

    private final T buffer;
    private MemorySize size;

    public BufferPartition(T buffer, MemorySize size) {
        if (buffer.size().getBytes() < size.getEnd()) {
            throw new IllegalArgumentException("Partition extends outside buffer.");
        }
        this.buffer = buffer;
        this.size = size;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        // check partition validity due to the source buffer possibly being resized
        if (buffer.size().getBytes() < this.size.getEnd()) {
            throw new IllegalStateException("Partition is outdated: extends outside buffer.");
        }
        return buffer.map(this.size.getOffset() + offset, size);
    }

    @Override
    public void stage(long offset, long size) {
        buffer.stage(this.size.getOffset() + offset, size);
    }

    @Override
    public ResizeResult resize(MemorySize size) {
        if (buffer.size().getBytes() < size.getEnd()) {
            return ResizeResult.Failure;
        }
        this.size = size;
        return ResizeResult.Success;
    }

    @Override
    public MemorySize size() {
        return size;
    }

    public T getBuffer() {
        return buffer;
    }

}
