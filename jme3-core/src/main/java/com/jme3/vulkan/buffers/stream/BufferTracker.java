package com.jme3.vulkan.buffers.stream;

import java.util.Iterator;

public class BufferTracker implements Iterable<BufferTracker.Island> {

    private final Island head = new Island(0, 0);
    private long minIslandGap = 32;

    public void add(long offset, long size) {
        long end = offset + size;
        Island prev = head;
        for (Island i = head.next; i != null; prev = i, i = i.next) {
            if (end < i.start - minIslandGap) {
                // insert below
                Island ins = new Island(offset, end);
                ins.prev = i.prev;
                ins.next = i;
                i.prev.next = ins;
                i.prev = ins;
                return;
            }
            if (offset <= i.end + minIslandGap) {
                if (offset < i.start) i.start = offset;
                // merge with higher islands
                if (end > i.end) {
                    i.end = end;
                    Island next = i.next;
                    for (; next != null && i.end >= next.start - minIslandGap; next = next.next) {
                        i.end = Math.max(next.end, i.end);
                    }
                    if (next != null) next.prev = i;
                    i.next = next;
                }
                return;
            }
        }
        // append at end
        prev.next = new Island(offset, size);
        prev.next.prev = prev;
    }

    public void remove(long offset, long size) {
        long end = offset + size;
        for (Island i = head.next; i != null; i = i.next) {
            if (i.start >= end) return;
            if (i.end <= offset) continue;
            boolean side1 = offset - i.start > 0;
            boolean side2 = i.end - end > 0;
            if (side1 && side2) {
                // split island
                i.start = offset;
                Island next = new Island(end, i.end);
                next.next = i.next;
                next.prev = i;
                i.next.prev = next;
                i.next = next;
                return;
            }
            if (side1) {
                // squash island up
                i.start = end;
                return;
            }
            if (side2) {
                // squash island down
                i.end = offset;
                continue;
            }
            // fully encompassed, remove island
            i.prev.next = i.next;
            if (i.next != null) i.next.prev = i.prev;
        }
    }

    public void clear() {
        head.next = null;
    }

    public boolean isEmpty() {
        return head.next == null;
    }

    public void setMinIslandGap(long minIslandGap) {
        this.minIslandGap = minIslandGap;
    }

    public long getMinIslandGap() {
        return minIslandGap;
    }

    @Override
    public Iterator<Island> iterator() {
        return new IteratorImpl(head);
    }

    public static class Island {

        private long start, end;
        private Island next, prev;

        private Island(long start, long end) {
            this.start = start;
            this.end = end;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public long getSize() {
            return end - start;
        }

    }

    private static class IteratorImpl implements Iterator<Island> {

        private Island current;

        public IteratorImpl(Island current) {
            this.current = current;
        }

        @Override
        public boolean hasNext() {
            return current.next != null;
        }

        @Override
        public Island next() {
            return current = current.next;
        }

    }

}
