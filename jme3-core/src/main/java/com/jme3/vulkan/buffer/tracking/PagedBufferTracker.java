package com.jme3.vulkan.buffer.tracking;

import java.util.BitSet;
import java.util.Iterator;

public class PagedBufferTracker implements BufferTracker {

    private final int stride;
    private final BitSet pages = new BitSet();

    public PagedBufferTracker(int pageSize) {
        stride = pageSize;
    }

    @Override
    public void add(int start, int size) {
        int endIndex = (start + size - 1) / stride;
        for (int i = start / stride; i <= endIndex; i++) {
            pages.set(i);
        }
    }

    @Override
    public void remove(int start, int size) {
        int endIndex = (start + size) / stride;
        for (int i = ceilDiv(start, stride); i < endIndex && i < pages.size(); i++) {
            pages.clear(i);
        }
    }

    @Override
    public void clear() {
        pages.clear();
    }

    @Override
    public int getNumIslands() {
        return pages.cardinality();
    }

    @Override
    public int getNumCovered() {
        return pages.cardinality() * stride;
    }

    @Override
    public boolean isEmpty() {
        return pages.isEmpty();
    }

    @Override
    public Iterator<BufferTracker.Island> iterator() {
        return new IteratorImpl();
    }

    private static int ceilDiv(int a, int b) {
        return (int)Math.ceil((float)a / b);
    }

    private class IteratorImpl implements Iterator<BufferTracker.Island> {

        private int index = 0;

        @Override
        public boolean hasNext() {
            return pages.nextSetBit(index) >= 0;
        }

        @Override
        public Island next() {
            index = pages.nextSetBit(index) + 1;
            return new Page(index - 1);
        }

    }

    private class Page implements BufferTracker.Island {

        private final int index;
        private final int start, end;

        public Page(int index) {
            this.index = index;
            this.start = index * stride;
            this.end = start + stride;
        }

        @Override
        public int getStart() {
            return start;
        }

        @Override
        public int getEnd() {
            return end;
        }

        @Override
        public int getSize() {
            return end - start;
        }

        @Override
        public int getAvailableAfter(int limit) {
            if (end >= limit) return 0;
            int next = pages.nextSetBit(index + 1) * stride;
            if (next < 0 || next > limit) return limit - end;
            return next - end;
        }

    }

}
