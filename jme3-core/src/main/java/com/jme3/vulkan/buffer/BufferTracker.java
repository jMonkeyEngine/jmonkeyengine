package com.jme3.vulkan.buffer;

import java.util.Iterator;

public class BufferTracker implements Iterable<BufferTracker.Island> {

    private final Island head = new Island(0, 0);

    public BufferTracker add(int offset, int size) {
        Island add = new Island(offset, offset + size);
        Island prev = head;
        for (Island i = head.next; i != null; prev = i, i = i.next) {
            if (i.addAndMergeUp(add)) return this;
        }
        prev.insertAfterUnchecked(add);
        return this;
    }

    public BufferTracker remove(int offset, int size) {
        Island erasure = new Island(offset, offset + size);
        for (Island i = head.next; i != null; i = i.next) {
            i.remove(erasure);
        }
        return this;
    }

    public BufferTracker clear() {
        head.next = null;
        return this;
    }

    public boolean isEmpty() {
        return head.next == null;
    }

    @Override
    public Iterator<Island> iterator() {
        return new IteratorImpl(head);
    }

    public enum Position {

        Lower, Intersect, Higher

    }

    public static class Island {

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

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public int getSize() {
            return end - start;
        }

        public Position getPositionTo(Island i) {
            if (end < i.start) return Position.Lower;
            else if (start > i.end) return Position.Higher;
            else return Position.Intersect;
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
        }

        public void insertAfterUnchecked(Island i) {
            if (next != null) {
                i.next = next;
                next.prev = i;
            }
            i.prev = this;
            next = i;
        }

        public void remove() {
            prev.next = next;
            if (next != null) next.prev = prev;
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
                for (Island n = next; n != null; n = n.next) {
                    if (end >= n.start) {
                        end = Math.max(end, n.end);
                    } else break;
                }
                return true;
            }
            return false;
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
