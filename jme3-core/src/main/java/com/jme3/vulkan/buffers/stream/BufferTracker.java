package com.jme3.vulkan.buffers.stream;

import java.util.Iterator;

public class BufferTracker implements Iterable<BufferTracker.Island> {

    private final Island head = new Island(0, 0);
    private Island tail = head;
    private long minIslandGap = 10;

    public void add(long offset, long size) {
        long end = offset + size;
        for (Island i = head.next; i != null; i = i.next) {
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
                if (offset < i.start) {
                    i.start = offset;
                }
                // merge with higher islands
                if (end > i.end) {
                    i.end = end;
                    Island next = i.next;
//noinspection StatementWithEmptyBody
                    for (; next != null && i.end >= next.start - minIslandGap; next = next.next);
                    if (next == null) {
                        tail = i;
                    } else if (next != i.next) {
                        next.prev = i;
                    }
                    i.next = next;
                }
                return;
            }
        }
        // append at end
        tail.next = new Island(offset, size);
        tail.next.prev = tail;
        tail = tail.next;
    }

    public void clear() {
        head.next = null;
        tail = head;
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
