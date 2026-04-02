package com.jme3.vulkan.memory;

import com.jme3.export.*;
import com.jme3.util.BufferUtils;
import com.jme3.vulkan.tmp.Final;
import com.jme3.vulkan.tmp.FinalWriter;

import java.io.IOException;
import java.nio.*;
import java.util.Objects;

public class MemorySize implements Savable {

    public static final MemorySize ZERO = new MemorySize(0, 0);

    @Final private long offset, bytes;

    public MemorySize(long offset, long bytes) {
        this.offset = offset;
        this.bytes = bytes;
    }

    public MemorySize(long bytes) {
        this(0, bytes);
    }

    public MemorySize(Buffer buffer) {
        this(buffer.position(), buffer.remaining());
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule out = ex.getCapsule(this);
        out.write(offset, "offset", 0);
        out.write(bytes, "bytes", 0);
    }

    @Override
    @FinalWriter
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        offset = in.readLong("offset", 0);
        bytes = in.readLong("bytes", 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MemorySize that = (MemorySize) o;
        return offset == that.offset && bytes == that.bytes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, bytes);
    }

    public ByteBuffer position(ByteBuffer buffer) {
        buffer.position((int)offset);
        buffer.limit((int)(offset + bytes));
        return buffer;
    }

    public ShortBuffer position(ShortBuffer buffer) {
        buffer.position((int)offset / Short.BYTES);
        buffer.limit((int)(offset + bytes) / Short.BYTES);
        return buffer;
    }

    public IntBuffer position(IntBuffer buffer) {
        buffer.position((int)offset / Integer.BYTES);
        buffer.limit((int)(offset + bytes) / Integer.BYTES);
        return buffer;
    }

    public FloatBuffer position(FloatBuffer buffer) {
        buffer.position((int)offset / Float.BYTES);
        buffer.limit((int)(offset + bytes) / Float.BYTES);
        return buffer;
    }

    public DoubleBuffer position(DoubleBuffer buffer) {
        buffer.position((int)offset / Double.BYTES);
        buffer.limit((int)(offset + bytes) / Double.BYTES);
        return buffer;
    }

    public LongBuffer position(LongBuffer buffer) {
        buffer.position((int)offset / Long.BYTES);
        buffer.limit((int)(offset + bytes) / Long.BYTES);
        return buffer;
    }

    public MemorySize setOffset(long offset) {
        return new MemorySize(offset, bytes);
    }

    public MemorySize setBytes(long bytes) {
        return new MemorySize(offset, bytes);
    }

    public MemorySize setShorts(long shorts) {
        return new MemorySize(offset, shorts * Short.BYTES);
    }

    public MemorySize setInts(long ints) {
        return new MemorySize(offset, ints * Integer.BYTES);
    }

    public MemorySize setFloats(long floats) {
        return new MemorySize(offset, floats * Float.BYTES);
    }

    public MemorySize setDoubles(long doubles) {
        return new MemorySize(offset, doubles * Double.BYTES);
    }

    public MemorySize setLongs(long longs) {
        return new MemorySize(offset, longs * Long.BYTES);
    }

    public long getOffset() {
        return offset;
    }

    public long getBytes() {
        return bytes;
    }

    public long getEnd() {
        return offset + bytes;
    }

    public long getShorts() {
        return bytes / Short.BYTES;
    }

    public long getInts() {
        return bytes / Integer.BYTES;
    }

    public long getFloats() {
        return bytes / Float.BYTES;
    }

    public long getDoubles() {
        return bytes / Double.BYTES;
    }

    public long getLongs() {
        return bytes / Long.BYTES;
    }

    public static MemorySize bytes(long elements) {
        return new MemorySize(0, elements);
    }

    public static MemorySize shorts(long elements) {
        return new MemorySize(0, elements * Short.BYTES);
    }

    public static MemorySize ints(long elements) {
        return new MemorySize(0, elements * Integer.BYTES);
    }

    public static MemorySize floats(long elements) {
        return new MemorySize(0, elements * Float.BYTES);
    }

    public static MemorySize doubles(long elements) {
        return new MemorySize(0, elements * Double.BYTES);
    }

    public static MemorySize longs(long elements) {
        return new MemorySize(0, elements * Long.BYTES);
    }

    public static MemorySize bytes(long offset, long elements) {
        return new MemorySize(offset, elements);
    }

    public static MemorySize shorts(long offset, long elements) {
        return new MemorySize(offset, elements * Short.BYTES);
    }

    public static MemorySize ints(long offset, long elements) {
        return new MemorySize(offset, elements * Integer.BYTES);
    }

    public static MemorySize floats(long offset, long elements) {
        return new MemorySize(offset, elements * Float.BYTES);
    }

    public static MemorySize doubles(long offset, long elements) {
        return new MemorySize(offset, elements * Double.BYTES);
    }

    public static MemorySize longs(long offset, long elements) {
        return new MemorySize(offset, elements * Long.BYTES);
    }

    @Deprecated
    public static MemorySize dynamic(long bytes, long bytesPerElement) {
        return new MemorySize(bytes / bytesPerElement, bytesPerElement);
    }

}
