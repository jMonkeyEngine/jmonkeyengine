package com.jme3.vulkan.memory;

import java.util.Objects;

public class MemorySize {

    public static final MemorySize ZERO = new MemorySize(0, 1);

    private final int elements;
    private final int bytesPerElement;
    private final int bytes;

    public MemorySize(int elements, int bytesPerElement) {
        this.elements = elements;
        this.bytesPerElement = bytesPerElement;
        this.bytes = this.elements * this.bytesPerElement;
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

    public int getElements() {
        return elements;
    }

    public int getBytesPerElement() {
        return bytesPerElement;
    }

    public int getBytes() {
        return bytes;
    }

    public int getBytes(int padding) {
        return bytes + padding * bytesPerElement;
    }

    public int getShorts() {
        return bytes / Short.BYTES;
    }

    public int getShorts(int padding) {
        return getBytes(padding) / Short.BYTES;
    }

    public int getInts() {
        return bytes / Integer.BYTES;
    }

    public int getInts(int padding) {
        return getBytes(padding) / Integer.BYTES;
    }

    public int getFloats() {
        return bytes / Float.BYTES;
    }

    public int getFloats(int padding) {
        return getBytes(padding) / Float.BYTES;
    }

    public int getDoubles() {
        return bytes / Double.BYTES;
    }

    public int getDoubles(int padding) {
        return getBytes(padding) / Double.BYTES;
    }

    public int getLongs() {
        return bytes / Long.BYTES;
    }

    public int getLongs(int padding) {
        return getBytes(padding) / Long.BYTES;
    }

    public static MemorySize bytes(int elements) {
        return new MemorySize(elements, 1);
    }

    public static MemorySize shorts(int elements) {
        return new MemorySize(elements, Short.BYTES);
    }

    public static MemorySize ints(int elements) {
        return new MemorySize(elements, Integer.BYTES);
    }

    public static MemorySize floats(int elements) {
        return new MemorySize(elements, Float.BYTES);
    }

    public static MemorySize doubles(int elements) {
        return new MemorySize(elements, Double.BYTES);
    }

    public static MemorySize longs(int elements) {
        return new MemorySize(elements, Long.BYTES);
    }

}
