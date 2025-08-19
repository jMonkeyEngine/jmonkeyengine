package com.jme3.vulkan.memory;

public class MemorySize {

    private final int elements;
    private final int bytesPerElement;
    private final int bytes;

    public MemorySize(int elements, int bytesPerElement) {
        this.elements = elements;
        this.bytesPerElement = bytesPerElement;
        this.bytes = this.elements * this.bytesPerElement;
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
