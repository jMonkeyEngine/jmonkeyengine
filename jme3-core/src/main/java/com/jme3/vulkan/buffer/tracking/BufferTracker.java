package com.jme3.vulkan.buffer.tracking;

import com.jme3.vulkan.buffer.BufferCopy;

public interface BufferTracker extends Iterable<BufferTracker.Island> {

    void add(int start, int size);

    default void add(BufferTracker tracker) {
        for (BufferTracker.Island i : tracker) {
            add(i.getStart(), i.getSize());
        }
    }

    default void addDst(BufferCopy copy) {
        for (BufferCopy.Region r : copy.getRegions()) {
            add(r.getDstOffset(), r.getSize());
        }
    }

    void remove(int start, int size);

    void clear();

    int getNumIslands();

    int getNumCovered();

    boolean isEmpty();

    interface Island {

        int getStart();

        int getEnd();

        int getAvailableAfter(int limit);

        default int getSize() {
            return getEnd() - getStart();
        }

    }

}
