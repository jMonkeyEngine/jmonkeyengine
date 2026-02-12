package com.jme3.vulkan.buffers;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;

public class VirtualBufferMapping implements BufferMapping {

    private final long address;
    private final int size;
    private ByteBuffer bytes;
    private ShortBuffer shorts;
    private IntBuffer ints;
    private FloatBuffer floats;
    private DoubleBuffer doubles;
    private LongBuffer longs;
    private PointerBuffer pointers;

    public VirtualBufferMapping(ByteBuffer bytes) {
        this.address = MemoryUtil.memAddress(bytes, 0);
        this.bytes = bytes;
        this.size = bytes.limit();
    }

    @Override
    public void close() {}

    @Override
    public void push(long offset, long size) {}

    @Override
    public long getAddress() {
        return address;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public ByteBuffer getBytes() {
        if (bytes == null) {
            bytes = MemoryUtil.memByteBuffer(address, size);
        }
        return bytes;
    }

    @Override
    public ShortBuffer getShorts() {
        if (shorts == null) {
            shorts = MemoryUtil.memShortBuffer(address, size / Short.BYTES);
        }
        return shorts;
    }

    @Override
    public IntBuffer getInts() {
        if (ints == null) {
            ints = MemoryUtil.memIntBuffer(address, size / Integer.BYTES);
        }
        return ints;
    }

    @Override
    public FloatBuffer getFloats() {
        if (floats == null) {
            floats = MemoryUtil.memFloatBuffer(address, size / Float.BYTES);
        }
        return floats;
    }

    @Override
    public DoubleBuffer getDoubles() {
        if (doubles == null) {
            doubles = MemoryUtil.memDoubleBuffer(address, size / Double.BYTES);
        }
        return doubles;
    }

    @Override
    public LongBuffer getLongs() {
        if (longs == null) {
            longs = MemoryUtil.memLongBuffer(address, size / Long.BYTES);
        }
        return longs;
    }

    @Override
    public PointerBuffer getPointers() {
        if (pointers == null) {
            pointers = MemoryUtil.memPointerBuffer(address, size / Long.BYTES);
        }
        return pointers;
    }

}
