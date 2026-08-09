package com.jme3.vulkan.buffer;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.vulkan.buffer.tracking.BufferTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

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

    public BufferCopy add(Struct<?> src, Struct<?> dst) {
        ListIterator<? extends StructField> srcFields = src.getFields().listIterator();
        ListIterator<? extends StructField> dstFields = dst.getFields().listIterator();
        while (srcFields.hasNext() && dstFields.hasNext()) {
            StructField originSrc = srcFields.next();
            StructField originDst = dstFields.next();
            int size = Math.min(originSrc.capacity(), originDst.capacity());
            while (srcFields.hasNext() && dstFields.hasNext()) {
                StructField seqSrc = srcFields.next();
                StructField seqDst = srcFields.next();
                int srcStride = seqSrc.getStructLocalOffset() - originSrc.getStructLocalOffset();
                if (srcStride == seqDst.getStructLocalOffset() - originDst.getStructLocalOffset()) {
                    size = srcStride + Math.min(seqSrc.capacity(), seqDst.capacity());
                } else {
                    srcFields.previous();
                    dstFields.previous();
                    break;
                }
            }
            regions.add(new Region(originSrc.getBufferLocalOffset(), originDst.getBufferLocalOffset(), size));
        }
        return this;
    }

    public BufferCopy add(int srcOffset, int dstOffset, BufferTracker tracker) {
        for (BufferTracker.Island i : tracker) {
            regions.add(new Region(srcOffset + i.getStart(), dstOffset + i.getStart(), i.getSize()));
        }
        return this;
    }

    public BufferCopy add(EngineBuffer src, int srcOffset, EngineBuffer dst, int dstOffset) {
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
