package com.jme3.vulkan.buffers;

import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.function.Supplier;

public class MultiMappingBuffer<T extends GpuBuffer> extends AbstractNative<Object> implements GpuBuffer {

    private T buffer;
    private ByteBuffer mapping;
    private final PointerBuffer ptr = MemoryUtil.memAllocPointer(1);
    private int numMappings = 0;

    public MultiMappingBuffer() {}

    public MultiMappingBuffer(T buffer) {
        setBuffer(buffer);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        if (mapping == null) {
            mapping = buffer.mapBytes();
        }
        numMappings++;
        return ptr.put(0, MemoryUtil.memAddress(mapping, offset));
    }

    @Override
    public void unmap() {
        if (--numMappings <= 0) {
            if (numMappings < 0) {
                throw new IllegalStateException("Unmap call without corresponding map call.");
            }
            mapping = null;
            buffer.unmap();
        }
    }

    @Override
    public void push(int offset, int size) {
        buffer.push(offset, size);
    }

    public void setBuffer(T buffer) {
        this.buffer = Objects.requireNonNull(buffer, "Internal buffer cannot be null.");
    }

    public void setBufferIfAbsent(Supplier<T> factory) {
        if (buffer == null) {
            setBuffer(factory.get());
        }
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public T getBuffer() {
        return buffer;
    }

    public int getNumMappings() {
        return numMappings;
    }

    public boolean hasBuffer() {
        return buffer != null;
    }

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> MemoryUtil.memFree(ptr);
    }

}
