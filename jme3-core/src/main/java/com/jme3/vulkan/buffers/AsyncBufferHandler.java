package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;
import java.util.function.Supplier;

public class AsyncBufferHandler <T extends GpuBuffer> implements Mappable {

    private T buffer;
    private PointerBuffer pointer;
    private int mappings = 0;

    public AsyncBufferHandler() {}

    public AsyncBufferHandler(T buffer) {
        this.buffer = buffer;
    }

    @Override
    public PointerBuffer map() {
        if (mappings++ == 0) {
            pointer = buffer.map();
        }
        return pointer;
    }

    @Override
    public void unmap() {
        if (mappings == 1) {
            mappings--;
            buffer.unmap();
            pointer = null;
        }
    }

    public void forceUnmap() {
        if (mappings > 0) {
            buffer.unmap();
            mappings = 0;
            pointer = null;
        }
    }

    public boolean resize(int elements) {
        if (buffer == null) {
            throw new NullPointerException("Internal buffer is null.");
        }
        if (mappings > 0) {
            throw new IllegalStateException("Cannot resize when mapped.");
        }
        return buffer.resize(elements);
    }

    @Override
    public ByteBuffer mapBytes() {
        return map().getByteBuffer(0, buffer.size().getBytes());
    }

    @Override
    public ShortBuffer mapShorts() {
        return map().getShortBuffer(0, buffer.size().getShorts());
    }

    @Override
    public IntBuffer mapInts() {
        return map().getIntBuffer(0, buffer.size().getInts());
    }

    @Override
    public FloatBuffer mapFloats() {
        return map().getFloatBuffer(0, buffer.size().getFloats());
    }

    @Override
    public DoubleBuffer mapDoubles() {
        return map().getDoubleBuffer(0, buffer.size().getDoubles());
    }

    @Override
    public LongBuffer mapLongs() {
        return map().getLongBuffer(0, buffer.size().getLongs());
    }

    public void copy(GpuBuffer buffer) {
        copy(buffer.mapBytes());
    }

    public void copy(AsyncBufferHandler<? extends GpuBuffer> buffer) {
        copy(buffer.mapBytes());
    }

    public void copy(ByteBuffer buffer) {
        MemoryUtil.memCopy(buffer, mapBytes());
    }

    public void copy(ShortBuffer buffer) {
        MemoryUtil.memCopy(buffer, mapShorts());
    }

    public void copy(IntBuffer buffer) {
        MemoryUtil.memCopy(buffer, mapInts());
    }

    public void copy(FloatBuffer buffer) {
        MemoryUtil.memCopy(buffer, mapFloats());
    }

    public void copy(DoubleBuffer buffer) {
        MemoryUtil.memCopy(buffer, mapDoubles());
    }

    public void copy(LongBuffer buffer) {
        MemoryUtil.memCopy(buffer, mapLongs());
    }

    public void copy(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            copy((ByteBuffer) buffer);
        } else if (buffer instanceof ShortBuffer) {
            copy((ShortBuffer) buffer);
        } else if (buffer instanceof IntBuffer) {
            copy((IntBuffer) buffer);
        } else if (buffer instanceof FloatBuffer) {
            copy((FloatBuffer) buffer);
        } else if (buffer instanceof DoubleBuffer) {
            copy((DoubleBuffer) buffer);
        } else if (buffer instanceof LongBuffer) {
            copy((LongBuffer) buffer);
        }
    }

    public void setBuffer(T buffer) {
        this.buffer = buffer;
    }

    public void setBufferIfAbsent(Supplier<T> factory) {
        if (buffer == null) {
            buffer = factory.get();
        }
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    public T getBuffer() {
        return buffer;
    }

    public int getMappings() {
        return mappings;
    }

    public boolean hasBuffer() {
        return buffer != null;
    }

}
