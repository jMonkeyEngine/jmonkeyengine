package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;

public class AsyncMappingBuffer <T extends MappableBuffer> implements MappableBuffer {

    private final Object mappingLock = new Object();

    private final T buffer;
    private ByteBuffer mapping;
    private int numMappings = 0;

    public AsyncMappingBuffer(T buffer) {
        this.buffer = Objects.requireNonNull(buffer, "Buffer cannot be null.");
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        synchronized (mappingLock) {
            if (mapping == null) {
                mapping = buffer.mapBytes();
            }
            numMappings++;
            return BufferUtils.createPointerBuffer(1).put(0, MemoryUtil.memAddress(mapping, offset));
        }
    }

    @Override
    public void unmap() {
        synchronized (mappingLock) {
            numMappings--;
            if (numMappings < 0) {
                throw new IllegalStateException("Unmap call without corresponding map call.");
            } else if (numMappings == 0) {
                mapping = null;
                buffer.unmap();
            }
        }
    }

    @Override
    public void push(int offset, int size) {
        buffer.push(offset, size);
    }

    @Override
    public boolean resize(MemorySize size) {
        synchronized (mappingLock) {
            if (numMappings > 0) {
                throw new IllegalStateException("Cannot resize while mapped.");
            }
            mapping = null;
            return buffer.resize(size);
        }
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public T getBuffer() {
        return buffer;
    }

    public int getNumConcurrentMappings() {
        return numMappings;
    }

}
