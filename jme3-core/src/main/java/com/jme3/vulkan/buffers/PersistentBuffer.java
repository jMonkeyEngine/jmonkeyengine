package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;

public class PersistentBuffer <T extends MappableBuffer> implements MappableBuffer {

    private final T buffer;
    private volatile BufferMapping mapping;

    public PersistentBuffer(T buffer) {
        this.buffer = buffer;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (mapping == null) synchronized (buffer) {
            if (mapping == null) {
                mapping = buffer.map(offset, size);
            }
        }
        return new VirtualBufferMapping(mapping.getAddress() + offset, size);
    }

    @Override
    public void stage(long offset, long size) {
        buffer.stage(offset, size);
    }

    @Override
    public ResizeResult resize(MemorySize size) {
        if (mapping != null) {
            forceUnmap();
        }
        return buffer.resize(size);
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public void forceUnmap() {
        if (mapping != null) synchronized (buffer) {
            if (mapping != null) {
                mapping.close();
                mapping = null;
            }
        }
    }

    public T getBuffer() {
        return buffer;
    }

}
