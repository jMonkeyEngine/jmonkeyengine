package com.jme3.vulkan.buffer;

import com.jme3.vulkan.buffer.tracking.BufferTracker;

import java.util.ArrayList;
import java.util.Collection;

public class BufferCopy {

    private final Collection<Region> regions = new ArrayList<>();

    public BufferCopy add(int srcOffset, int dstOffset, int size) {
        regions.add(new Region(srcOffset, dstOffset, size));
        return this;
    }

    public BufferCopy add(Region r) {
        regions.add(r);
        return this;
    }

    public BufferCopy add(BufferCopy cpy) {
        regions.addAll(cpy.getRegions());
        return this;
    }

    public BufferCopy add(BufferTracker t) {
        for (BufferTracker.Island i : t) {
            regions.add(new Region(i.getStart(), i.getStart(), i.getSize()));
        }
        return this;
    }

    public BufferCopy add(int srcOffset, int dstOffset, BufferTracker tracker) {
        for (BufferTracker.Island i : tracker) {
            regions.add(new Region(srcOffset + i.getStart(), dstOffset + i.getStart(), i.getSize()));
        }
        return this;
    }

    public BufferCopy add(GpuBuffer src, int srcOffset, GpuBuffer dst, int dstOffset) {
        regions.add(new Region(srcOffset, dstOffset, Math.min(src.capacity() - srcOffset, dst.capacity() - dstOffset)));
        return this;
    }

    public BufferCopy addFlatten(int srcOffset, int dstOffset, BufferTracker tracker) {
        for (BufferTracker.Island i : tracker) {
            regions.add(new Region(srcOffset + i.getStart(), dstOffset, i.getSize()));
            dstOffset += i.getSize();
        }
        return this;
    }

    public BufferCopy addExpand(int srcOffset, int dstOffset, BufferTracker tracker) {
        for (BufferTracker.Island i : tracker) {
            regions.add(new Region(srcOffset, dstOffset + i.getStart(), i.getSize()));
            srcOffset += i.getSize();
        }
        return this;
    }

    public Collection<Region> getRegions() {
        return regions;
    }

    public static class Region {

        private final int srcOffset, dstOffset;
        private final int size;

        public Region(int srcOffset, int dstOffset, int size) {
            this.srcOffset = srcOffset;
            this.dstOffset = dstOffset;
            this.size = size;
        }

        public int getSrcOffset() {
            return srcOffset;
        }

        public int getDstOffset() {
            return dstOffset;
        }

        public int getSize() {
            return size;
        }

    }

}
