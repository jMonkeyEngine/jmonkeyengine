package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

public class BufferPartition <T extends GpuBuffer> implements GpuBuffer {

    private final T buffer;
    private final int offset;
    private final MemorySize size;

    public BufferPartition(T buffer, int offset, MemorySize size) {
        this.buffer = buffer;
        this.offset = offset;
        this.size = size;
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return buffer.map(this.offset + offset, size);
    }

    @Override
    public boolean resize(MemorySize size) {
        return false;
    }

    @Override
    public long getId() {
        return buffer.getId();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    public T getBuffer() {
        return buffer;
    }

    public int getOffset() {
        return offset;
    }

}
