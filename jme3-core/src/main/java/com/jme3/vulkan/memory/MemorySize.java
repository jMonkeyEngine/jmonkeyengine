package com.jme3.vulkan.memory;

import com.jme3.util.BufferUtils;

import java.nio.*;
import java.util.Objects;

public class MemorySize {

    public static final MemorySize ZERO = new MemorySize(0, 0, 1);

    private final long elements;
    private final int bytesPerElement;

    private final long offset, bytes;

    public MemorySize(long offset, long bytes) {
        this.offset = offset;
        this.bytes = bytes;
        this.elements = bytes;
        this.bytesPerElement = 1;
    }

    public MemorySize(long offset, long elements, int bytesPerElement) {
        assert offset >= 0 : "Offset must be non-negative";
        assert elements >= 0 : "Elements must be non-negative";
        assert bytesPerElement > 0 : "Bytes per element must be positive.";
        this.elements = elements;
        this.bytesPerElement = bytesPerElement;
        this.bytes = this.elements * this.bytesPerElement;
        this.offset = offset * bytesPerElement;
    }

    public MemorySize(long offset, Buffer buffer) {
        this.offset = offset;
        this.bytesPerElement = BufferUtils.getBytesPerElement(buffer);
        this.elements = buffer.limit() / bytesPerElement;
        this.bytes = buffer.limit();
    }

    public MemorySize(long bytes) {
        this(0, bytes);
    }

    public MemorySize(Buffer buffer) {
        this(0, buffer);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MemorySize that = (MemorySize) o;
        return elements == that.elements && bytesPerElement == that.bytesPerElement;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements, bytesPerElement);
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
        return new MemorySize(offset, elements, bytesPerElement);
    }

    public MemorySize setBytes(long bytes) {
        return new MemorySize(offset, bytes, Byte.BYTES);
    }

    public MemorySize setShorts(long shorts) {
        return new MemorySize(offset / Short.BYTES, shorts, Short.BYTES);
    }

    public MemorySize setInts(long ints) {
        return new MemorySize(offset / Integer.BYTES, ints, Integer.BYTES);
    }

    public MemorySize setFloats(long floats) {
        return new MemorySize(offset / Float.BYTES, floats, Float.BYTES);
    }

    public MemorySize setDoubles(long doubles) {
        return new MemorySize(offset / Double.BYTES, doubles, Double.BYTES);
    }

    public MemorySize setLongs(long longs) {
        return new MemorySize(offset / Long.BYTES, longs, Long.BYTES);
    }

    public int elementsFromBytes(int bytes) {
        return bytes / bytesPerElement;
    }

    public long getElements() {
        return elements;
    }

    public int getBytesPerElement() {
        return bytesPerElement;
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
        return new MemorySize(0, elements, 1);
    }

    public static MemorySize shorts(long elements) {
        return new MemorySize(0, elements, Short.BYTES);
    }

    public static MemorySize ints(long elements) {
        return new MemorySize(0, elements, Integer.BYTES);
    }

    public static MemorySize floats(long elements) {
        return new MemorySize(0, elements, Float.BYTES);
    }

    public static MemorySize doubles(long elements) {
        return new MemorySize(0, elements, Double.BYTES);
    }

    public static MemorySize longs(long elements) {
        return new MemorySize(0, elements, Long.BYTES);
    }

    public static MemorySize bytes(long offset, long elements) {
        return new MemorySize(offset, elements, 1);
    }

    public static MemorySize shorts(long offset, long elements) {
        return new MemorySize(offset, elements, Short.BYTES);
    }

    public static MemorySize ints(long offset, long elements) {
        return new MemorySize(offset, elements, Integer.BYTES);
    }

    public static MemorySize floats(long offset, long elements) {
        return new MemorySize(offset, elements, Float.BYTES);
    }

    public static MemorySize doubles(long offset, long elements) {
        return new MemorySize(offset, elements, Double.BYTES);
    }

    public static MemorySize longs(long offset, long elements) {
        return new MemorySize(offset, elements, Long.BYTES);
    }

    /**
     *
     * @param bytes total number of bytes
     * @param bytesPerElement number of bytes per element
     * @return memory size reflecting {@code bytes} and {@code bytesPerElement}
     */
    public static MemorySize dynamic(long bytes, long bytesPerElement) {
        return new MemorySize(bytes / bytesPerElement, bytesPerElement);
    }

}
