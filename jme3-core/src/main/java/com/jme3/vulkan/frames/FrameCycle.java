package com.jme3.vulkan.frames;

/**
 * Simple mutable {@link FrameIndex} that cycles through a fixed number of slots in
 * order, wrapping back to 0 after the last. Call {@link #advance()} exactly once per
 * frame, after confirming (e.g. via a fence wait) that the device has finished with the
 * slot about to be reused.
 */
public class FrameCycle implements FrameIndex {

    private final int count;
    private int current = 0;

    public FrameCycle(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Frame count must be positive.");
        }
        this.count = count;
    }

    /**
     * Advances to the next frame-in-flight slot, wrapping back to 0 after the last.
     */
    public void advance() {
        current = (current + 1) % count;
    }

    @Override
    public int current() {
        return current;
    }

    @Override
    public int count() {
        return count;
    }

}
