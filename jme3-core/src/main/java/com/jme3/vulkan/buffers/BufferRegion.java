package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;

public class BufferRegion {

    private int offset, end;

    public BufferRegion(int offset, int size) {
        this.offset = verifyOffset(offset);
        this.end = offset + verifySize(size);
    }

    public BufferRegion set(BufferRegion region) {
        this.offset = region.offset;
        this.end = region.end;
        return this;
    }

    public BufferRegion set(int offset, int size) {
        this.offset = verifyOffset(offset);
        this.end = offset + verifySize(size);
        return this;
    }

    public boolean setEnd(int end) {
        if (end <= offset) {
            return false;
        }
        this.end = end;
        return true;
    }

    public BufferRegion unionLocal(BufferRegion region) {
        offset = Math.min(offset, region.offset);
        end = Math.max(end, region.end);
        return this;
    }

    public BufferRegion unionLocal(int offset, int size) {
        this.offset = Math.min(this.offset, verifyOffset(offset));
        this.end = Math.max(this.end, offset + verifySize(size));
        return this;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return end - offset;
    }

    public int getEnd() {
        return end;
    }

    public static BufferRegion all(MemorySize size) {
        return new BufferRegion(0, size.getBytes());
    }

    public static BufferRegion union(BufferRegion region, int offset, int size) {
        if (region == null) {
            return new BufferRegion(offset, size);
        } else {
            return region.unionLocal(offset, size);
        }
    }

    private static int verifyOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative.");
        }
        return offset;
    }

    private static int verifySize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive.");
        }
        return size;
    }

}
