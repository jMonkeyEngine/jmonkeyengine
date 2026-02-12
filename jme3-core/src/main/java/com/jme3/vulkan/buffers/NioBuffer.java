package com.jme3.vulkan.buffers;

import com.jme3.util.natives.Disposable;
import com.jme3.util.natives.DisposableReference;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;

public class NioBuffer implements MappableBuffer, Disposable {

    private final PointerBuffer address = MemoryUtil.memCallocPointer(1);
    protected final DisposableReference ref;
    private ByteBuffer buffer;
    private MemorySize size;
    private int padding;
    private long baseBufferAddress;
    private int lastMappedOffset = -1;

    public NioBuffer(MemorySize size) {
        this(size, 0, true);
    }

    public NioBuffer(MemorySize size, int padding) {
        this(size, padding, true);
    }

    public NioBuffer(MemorySize size, int padding, boolean clearMem) {
        this.size = size;
        this.padding = padding;
        if (clearMem) {
            buffer = MemoryUtil.memCalloc(size.getBytes(padding));
        } else {
            buffer = MemoryUtil.memAlloc(size.getBytes(padding));
        }
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
    public ByteBuffer mapBytes() {
        return buffer;
    }

    @Override
    public ShortBuffer mapShorts() {
        return buffer.asShortBuffer();
    }

    @Override
    public IntBuffer mapInts() {
        return buffer.asIntBuffer();
    }

    @Override
    public FloatBuffer mapFloats() {
        return buffer.asFloatBuffer();
    }

    @Override
    public DoubleBuffer mapDoubles() {
        return buffer.asDoubleBuffer();
    }

    @Override
    public LongBuffer mapLongs() {
        return buffer.asLongBuffer();
    }

    @Override
    public void unmap() {}

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> {
            MemoryUtil.memFree(buffer);
            MemoryUtil.memFree(address);
        };
    }

    @Override
    public void push(int offset, int size) {}

    @Override
    public boolean resize(MemorySize size) {
        this.size = size;
        if (size.getBytes() > buffer.capacity()) {
            ByteBuffer newBuffer = MemoryUtil.memRealloc(buffer, size.getBytes(padding));
            newBuffer.limit(size.getBytes());
            if (newBuffer != buffer) {
                buffer = newBuffer;
                baseBufferAddress = MemoryUtil.memAddress(buffer, 0);
                ref.refresh();
            }
            return true;
        }
        buffer.limit(size.getBytes());
        return false;
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
