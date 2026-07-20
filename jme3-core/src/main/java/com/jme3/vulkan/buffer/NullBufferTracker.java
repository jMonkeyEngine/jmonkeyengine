package com.jme3.vulkan.buffer;

import java.util.Iterator;

public class NullBufferTracker implements BufferTracker {

    public static final NullBufferTracker INSTANCE = new NullBufferTracker();
    private static final Iterator<Island> ITERATOR = new Iterator<Island>() {
        @Override
        public boolean hasNext() {
            return false;
        }
        @Override
        public Island next() {
            return null;
        }
    };

    @Override
    public void add(int start, int size) {}

    @Override
    public void remove(int start, int size) {}

    @Override
    public void clear() {}

    @Override
    public int getNumIslands() {
        return 0;
    }

    @Override
    public int getCoveredBytes() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Iterator<Island> iterator() {
        return ITERATOR;
    }

}
