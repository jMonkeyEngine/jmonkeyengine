package com.jme3.vulkan.concurrent;

public class Region {

    private final int offset, size;

    public Region(int offset, int size) {
        this.offset = offset;
        this.size = size;
    }

    public boolean overlaps(Region r) {
        return offset + size > r.offset && offset < r.offset + r.size;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

}
