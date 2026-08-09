package com.jme3.vulkan.buffer.tracking;

import java.util.Iterator;

public class SliceBufferTracker implements BufferTracker {

    private final BufferTracker tracker;
    private final int offset, size;

    public SliceBufferTracker(BufferTracker tracker, int offset, int size) {
        this.tracker = tracker;
        this.offset = offset;
        this.size = size;
    }

    public BufferTracker getParentTracker() {
        return tracker;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    @Override
    public void add(int start, int size) {
        tracker.add(offset + start, size);
    }

    @Override
    public void remove(int start, int size) {
        tracker.add(offset + start, size);
    }

    @Override
    public void clear() {
        tracker.remove(offset, size);
    }

    @Override
    public int getNumIslands() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getNumCovered() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<Island> iterator() {
        throw new UnsupportedOperationException();
    }
}
