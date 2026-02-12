package com.jme3.vulkan.buffers;

import org.lwjgl.PointerBuffer;

import java.nio.*;

public class SourceBufferMapping implements BufferMapping {

    private final MappableBuffer source;
    private final PointerBuffer address;
    private final long size;
    private ByteBuffer bytes;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private FloatBuffer floats;
    private DoubleBuffer doubles;
    private LongBuffer longs;
    private PointerBuffer pointers;

    public SourceBufferMapping(MappableBuffer source, PointerBuffer address, long size) {
        this.source = source;
        this.address = address;
        this.size = size;
    }

    @Override
    public void close() {
        source.unmap();
    }

    @Override
    public void push(long offset, long size) {
        source.push(offset, size);
    }

    @Override
    public long getAddress() {
        return address.get(0);
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public ByteBuffer getBytes() {
        if (bytes == null) {
            bytes = address.getByteBuffer(0, (int)size);
        }
        return bytes.position(0).limit((int)size);
    }

    @Override
    public ShortBuffer getShorts() {
        if (shorts == null) {
            shorts = address.getShortBuffer(0, (int)size / Short.BYTES);
        }
        return shorts;
    }

    @Override
    public IntBuffer getInts() {
        if (ints == null) {
            ints = address.getIntBuffer(0, (int)size / Integer.BYTES);
        }
        return ints;
    }

    @Override
    public FloatBuffer getFloats() {
        if (floats == null) {
            floats = address.getFloatBuffer(0, (int)size / Float.BYTES);
        }
        return floats;
    }

    @Override
    public DoubleBuffer getDoubles() {
        if (doubles == null) {
            doubles = address.getDoubleBuffer(0, (int)size / Double.BYTES);
        }
        return doubles;
    }

    @Override
    public LongBuffer getLongs() {
        if (longs == null) {
            longs = address.getLongBuffer(0, (int)size / Long.BYTES);
        }
        return longs;
    }

    @Override
    public PointerBuffer getPointers() {
        if (pointers == null) {
            pointers = address.getPointerBuffer(0, (int)size);
        }
        return pointers;
    }

}
