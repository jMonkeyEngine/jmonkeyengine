package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

import java.nio.*;

public class PersistentBuffer <T extends MappableBuffer> implements MappableBuffer {

    private final T buffer;
    private MappingImpl mapping;
    private long mappedOffset, mappedSize;

    public PersistentBuffer(T buffer) {
        this.buffer = buffer;
    }

    @Override
    public BufferMapping map(long offset, long size) {
        if (mapping == null || offset != mappedOffset || mappedSize != size) {
            if (mapping != null) {
                mapping.forceClose();
            }
            mapping = new MappingImpl(buffer.map(offset, size));
            mappedOffset = offset;
            mappedSize = size;
        }
        return mapping;
    }

    @Override
    public void push(long offset, long size) {
        buffer.push(offset, size);
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
        if (mapping != null) {
            mapping.forceClose();
            mapping = null;
        }
    }

    public T getBuffer() {
        return buffer;
    }

    private static class MappingImpl implements BufferMapping {

        private final BufferMapping delegate;

        public MappingImpl(BufferMapping delegate) {
            this.delegate = delegate;
        }

        public void forceClose() {
            delegate.close();
        }

        @Override
        public void close() {}

        @Override
        public void push(long offset, long size) {
            delegate.push(offset, size);
        }

        @Override
        public long getAddress() {
            return delegate.getAddress();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public ByteBuffer getBytes() {
            return delegate.getBytes();
        }

        @Override
        public ShortBuffer getShorts() {
            return delegate.getShorts();
        }

        @Override
        public IntBuffer getInts() {
            return delegate.getInts();
        }

        @Override
        public FloatBuffer getFloats() {
            return delegate.getFloats();
        }

        @Override
        public DoubleBuffer getDoubles() {
            return delegate.getDoubles();
        }

        @Override
        public LongBuffer getLongs() {
            return delegate.getLongs();
        }

        @Override
        public PointerBuffer getPointers() {
            return delegate.getPointers();
        }

    }

}
