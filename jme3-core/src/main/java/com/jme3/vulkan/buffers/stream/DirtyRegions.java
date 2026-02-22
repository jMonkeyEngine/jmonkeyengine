package com.jme3.vulkan.buffers.stream;

import java.util.Iterator;

public class DirtyRegions implements Iterable<DirtyRegions.Region> {

    private final Region head = new Region(0, 0);
    private int numRegions = 0;
    private long coverage = 0;

    public void add(long offset, long size) {
        if (offset < 0 || size < 0) {
            throw new IllegalArgumentException("Region parameters must be non-negative.");
        }
        if (size == 0) return;
        boolean merged = false;
        for (Region r = head, prev = null; r != null; r = r.next) {
            if (!(merged = (merged | r.mergeOnIntersect(offset, size))) && prev != null && offset + size < r.start) {
                (prev.next = new Region(offset, size)).next = r;
                break;
            }
            if (prev != null && prev.mergeOnIntersect(r)) {
                prev.next = r.next;
            } else {
                prev = r;
            }
        }
        long cov = 0;
        int n = 0;
        for (Region r : this) {
            cov += r.getSize();
            n++;
        }
        coverage = cov;
        numRegions = n;
    }

    public void optimize() {
        optimizeGaps(10);
        optimizeNumRegions(10);
    }

    public void optimizeGaps(int regionMergeGap) {
        for (Region r = head.next, prev = head; r != null; r = r.next) {
            long g = prev.fillGapBetween(r, regionMergeGap);
            if (g >= 0) {
                prev = r;
                numRegions--;
                coverage += g;
            }
        }
    }

    public void optimizeNumRegions(int maxRegions) {
        if (numRegions <= 1) return;
        for (; numRegions > maxRegions; numRegions--) {
            Region pref = head;
            long gap = Long.MAX_VALUE;
            for (Region r = head; r.next != null; r = r.next) {
                long g = r.next.start - r.end;
                if (g < gap) {
                    gap = g;
                    pref = r;
                }
            }
            coverage += pref.fillGapBetween(pref.next, Long.MAX_VALUE);
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

    public long getCoverage() {
        return coverage;
    }

    public static class Region {

        private long start, end;
        private Region next;

        public Region(long offset, long size) {
            this.start = offset;
            this.end = offset + size;
        }

        private boolean mergeOnIntersect(long offset, long size) {
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

        private long fillGapBetween(Region r, long minGap) {
            if (end + minGap >= r.start) {
                minGap = end - r.start;
                end = r.end;
                next = r.next;
                return minGap;
            }
            return -1;
        }

        public long getOffset() {
            return start;
        }

        public long getSize() {
            return end - start;
        }

        public long getEnd() {
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
