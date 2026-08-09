package com.jme3.vulkan.buffer.tracking;

import java.util.Iterator;

public class ExactBufferTracker implements BufferTracker {

    private final Island head = new Island(0, 0);
    private int count = 0;

    @Override
    public void add(int offset, int size) {
        Island add = new Island(offset, offset + size);
        Island prev = head;
        for (Island i = head.next; i != null; prev = i, i = i.next) {
            if (i.addAndMergeUp(add)) return;
        }
        prev.insertAfterUnchecked(add);
    }

    @Override
    public void remove(int offset, int size) {
        Island erasure = new Island(offset, offset + size);
        for (Island i = head.next; i != null; i = i.next) {
            i.remove(erasure);
        }
    }

    @Override
    public void clear() {
        head.next = null;
    }

    @Override
    public int getNumIslands() {
        return count;
    }

    @Override
    public int getNumCovered() {
        int coverage = 0;
        for (BufferTracker.Island i : this) {
            coverage += i.getSize();
        }
        return coverage;
    }

    @Override
    public boolean isEmpty() {
        return head.next == null;
    }

    @Override
    public Iterator<BufferTracker.Island> iterator() {
        return new IteratorImpl(head);
    }

    protected class Island implements BufferTracker.Island {

        private int start, end;
        private Island next, prev;

        private Island(int start, int end) {
            this.start = start;
            this.end = end;
        }

        private Island(Island i) {
            this.start = i.start;
            this.end = i.end;
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
            if (next == null) return limit - end;
            return next.start - end;
        }

        public boolean mergeOrCopyBelowUnchecked(Island i) {
            if (i.end < start) {
                insertBeforeUnchecked(new Island(i));
                return true;
            }
            if (i.start <= end) {
                start = Math.min(start, i.start);
                end = Math.max(end, i.end);
                return true;
            }
            return false;
        }

        public void insertBeforeUnchecked(Island i) {
            i.prev = prev;
            i.next = this;
            prev.next = i;
            prev = i;
            count++;
        }

        public void insertAfterUnchecked(Island i) {
            if (next != null) {
                i.next = next;
                next.prev = i;
            }
            i.prev = this;
            next = i;
            count++;
        }

        public void remove() {
            prev.next = next;
            if (next != null) next.prev = prev;
            count--;
        }

        public boolean remove(Island i) {
            if (i.start > start && i.end < end) {
                splitUnchecked(i.start, i.end); // erasure in middle
            } else if (i.start <= start && i.end >= end) {
                remove(); // erasure encompassing
            } else if (i.start <= start && i.end >= start) {
                start = i.end; // remove top
            } else if (i.start > start) {
                end = i.start; // remove bottom
            } else return false;
            return true;
        }

        public void splitUnchecked(int lowerEnd, int upperStart) {
            Island i = new Island(upperStart, end);
            end = lowerEnd;
            insertAfterUnchecked(i);
        }

        public boolean addAndMergeUp(Island i) {
            if (i.start <= end && i.end >= start) {
                start = Math.min(start, i.start);
                end = Math.min(end, i.end);
                for (; next != null; next = next.next) {
                    if (end >= next.start) {
                        end = Math.max(end, next.end);
                        if (next.next != null) {
                            next.next.prev = this;
                        }
                        count--;
                    } else break;
                }
                return true;
            }
            return false;
        }

    }

    private static class IteratorImpl implements Iterator<BufferTracker.Island> {

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
