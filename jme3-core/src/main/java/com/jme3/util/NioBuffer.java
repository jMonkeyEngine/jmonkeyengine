package com.jme3.util;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;

public class NioBuffer implements GpuBuffer, Native<Long> {

    private final PointerBuffer address = MemoryUtil.memCallocPointer(1);
    private final NativeReference ref;
    private ByteBuffer buffer;
    private MemorySize size;
    private int padding;
    private long baseBufferAddress;
    private int lastMappedOffset = -1;

    public NioBuffer(MemorySize size) {
        this(size, 0);
    }

    public NioBuffer(MemorySize size, int padding) {
        this.size = size;
        this.padding = padding;
        buffer = MemoryUtil.memCalloc(size.getElements() + padding, size.getBytesPerElement());
        buffer.limit(size.getBytes());
        baseBufferAddress = MemoryUtil.memAddress(buffer, 0);
        ref = Native.get().register(this);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative.");
        }
        if (offset != lastMappedOffset) {
            if (offset == 0) {
                address.put(0, baseBufferAddress);
            } else {
                address.put(0, MemoryUtil.memAddress(buffer, offset));
            }
            lastMappedOffset = offset;
        }
        return address;
    }

    @Override
    public void unmap() {}

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public long getId() {
        return baseBufferAddress;
    }

    @Override
    public void resize(int elements) {
        if (elements < 0) {
            throw new IllegalArgumentException("Buffer size cannot be negative.");
        }
        if (elements != size.getElements()) {
            size = new MemorySize(elements, size.getBytesPerElement());
            if (size.getBytes() > buffer.capacity()) {
                ByteBuffer newBuffer = MemoryUtil.memRealloc(buffer, size.getBytes(padding));
                if (newBuffer != buffer) {
                    buffer = newBuffer;
                    baseBufferAddress = MemoryUtil.memAddress(buffer, 0);
                    ref.refresh();
                }
            }
            buffer.limit(size.getBytes());
        }
    }

    @Override
    public Long getNativeObject() {
        return baseBufferAddress;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(address);
        };
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public int getPadding() {
        return padding;
    }

}
