package com.jme3.vulkan.buffer.tracking;

import java.util.Iterator;

public class RangeBufferTracker implements BufferTracker, BufferTracker.Island {

    private int min = Integer.MAX_VALUE;
    private int max = -1;

    public RangeBufferTracker() {}

    public RangeBufferTracker(int start, int size) {
        add(start, size);
    }

    @Override
    public void add(int start, int size) {
        min = Math.min(min, start);
        max = Math.max(max, start + size);
    }

    @Override
    public void remove(int start, int size) {
        int end = start + size;
        //noinspection StatementWithEmptyBody
        if (end <= min || start >= max || size == 0);
        else if (start <= min && end >= max) clear();
        else if (start <= min) min = end;
        else if (end >= max) max = start;
    }

    @Override
    public void clear() {
        min = Integer.MAX_VALUE;
        max = -1;
    }

    @Override
    public int getNumIslands() {
        return max > min ? 1 : 0;
    }

    @Override
    public int getNumCovered() {
        return max > min ? max - min : 0;
    }

    @Override
    public boolean isEmpty() {
        return max <= min;
    }

    @Override
    public Iterator<Island> iterator() {
        return new Singleton(this);
    }

    @Override
    public int getStart() {
        return min;
    }

    @Override
    public int getEnd() {
        return max;
    }

    @Override
    public int getAvailableAfter(int limit) {
        return Math.max(0, limit - max);
    }

    private static class Singleton implements Iterator<BufferTracker.Island> {

        private BufferTracker.Island value;

        public Singleton(Island value) {
            this.value = value;
        }

        @Override
        public boolean hasNext() {
            return value != null;
        }

        @Override
        public Island next() {
            BufferTracker.Island i = value;
            value = null;
            return i;
        }

    }

}
