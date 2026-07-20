package com.jme3.vulkan.buffer;

public interface BufferTracker extends Iterable<BufferTracker.Island> {

    void add(int start, int size);

    void remove(int start, int size);

    void clear();

    int getNumIslands();

    int getCoveredBytes();

    boolean isEmpty();

    interface Island {

        int getStart();

        int getEnd();

        int getAvailableBytesAfter(int limit);

        default int getSize() {
            return getEnd() - getStart();
        }

    }

}
