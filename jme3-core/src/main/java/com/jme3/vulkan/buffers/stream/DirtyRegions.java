package com.jme3.vulkan.buffers.stream;

import java.util.Iterator;

public class DirtyRegions implements Iterable<DirtyRegions.Region> {

    private final Region head = new Region(0, 0);
    private int numRegions = 0;
    private int coverage = 0;

    public void add(int offset, int size) {
        if (offset < 0 || size < 0) {
            throw new IllegalArgumentException("Region parameters must be non-negative.");
        }
        if (size == 0) return;
        boolean merged = false;
        for (Region r = head, prev = null; r != null; r = r.next) {
            if (!(merged = merged | r.mergeOnIntersect(offset, size)) && prev != null && offset + size < r.start) {
                (prev.next = new Region(offset, size)).next = r;
                break;
            }
            if (prev != null && prev.mergeOnIntersect(r)) {
                prev.next = r.next;
            } else {
                prev = r;
            }
        }
        int cov = 0, n = 0;
        for (Region r : this) {
            cov += r.getSize();
            n++;
        }
        coverage = cov;
        numRegions = n;
    }

    public void optimize(int minGap) {
        for (Region r = head.next, prev = head; r != null; r = r.next) {
            if (!prev.fillGapBetween(r, minGap)) {
                prev = r;
            }
        }
    }

    public void clear() {
        head.next = null;
        head.start = 0;
        head.end = 0;
        coverage = 0;
        numRegions = 0;
    }

    @Override
    public Iterator<Region> iterator() {
        return new IteratorImpl(head);
    }

    public int getNumRegions() {
        return numRegions;
    }

    public int getCoverage() {
        return coverage;
    }

    public static class Region {

        private int start, end;
        private Region next;

        public Region(int offset, int size) {
            this.start = offset;
            this.end = offset + size;
        }

        private boolean mergeOnIntersect(int offset, int size) {
            if (offset + size >= start && offset <= end) {
                start = Math.min(start, offset);
                end = Math.max(end, offset + size);
                return true;
            }
            return false;
        }

        private boolean mergeOnIntersect(Region r) {
            if (r.end >= start && r.start <= end) {
                start = Math.min(start, r.start);
                end = Math.max(end, r.end);
                return true;
            }
            return false;
        }

        private boolean fillGapBetween(Region r, int minGap) {
            if (end + minGap >= r.start) {
                end = r.end;
                next = r.next;
                return true;
            }
            return false;
        }

        public int getOffset() {
            return start;
        }

        public int getSize() {
            return end - start;
        }

        public int getEnd() {
            return end;
        }

    }

    private static class IteratorImpl implements Iterator<Region> {

        private Region current, prev;

        private IteratorImpl(Region current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return current != null && current.start != current.end;
        }

        @Override
        public Region next() {
            prev = current;
            current = current.next;
            return prev;
        }

        @Override
        public void remove() {
            if (prev != null && current != null) {
                prev.next = current.next;
            }
        }

    }

}
