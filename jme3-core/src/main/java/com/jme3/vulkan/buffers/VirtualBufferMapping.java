package com.jme3.vulkan.buffers;

import org.lwjgl.PointerBuffer;

import java.nio.*;

public class VirtualBufferMapping implements BufferMapping {

    private final BufferMapping source;

    public VirtualBufferMapping(BufferMapping source) {
        this.source = source;
    }

    @Override
    public void close() {}

    @Override
    public boolean isMapped() {
        return source.isMapped();
    }

    @Override
    public void stage(long offset, long size) {
        source.stage(offset, size);
    }

    @Override
    public long getAddress() {
        return source.getAddress();
    }

    @Override
    public long getSize() {
        return source.getSize();
    }

    @Override
    public ByteBuffer getBytes() {
        return source.getBytes();
    }

    @Override
    public ShortBuffer getShorts() {
        return source.getShorts();
    }

    @Override
    public IntBuffer getInts() {
        return source.getInts();
    }

    @Override
    public FloatBuffer getFloats() {
        return source.getFloats();
    }

    @Override
    public DoubleBuffer getDoubles() {
        return source.getDoubles();
    }

    @Override
    public LongBuffer getLongs() {
        return source.getLongs();
    }

    @Override
    public PointerBuffer getPointers() {
        return source.getPointers();
    }

}
