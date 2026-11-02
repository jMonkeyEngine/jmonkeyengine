package com.jme3.vulkan.frames;

/**
 * Simple mutable {@link Frame} that cycles through a fixed number of slots in
 * order, wrapping back to 0 after the last. Call {@link #advance()} exactly once per
 * frame, after confirming (e.g. via a fence wait) that the device has finished with the
 * slot about to be reused.
 */
public class FrameCycle implements Frame {

    private final int count;
    private int current = 0;
    private long frame = 0;
    private long completed = -1;

    public FrameCycle(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Frame count must be positive.");
        }
        this.count = count;
    }

    /**
     * Advances to the next frame-in-flight slot, wrapping back to 0 after the last. Since the
     * caller has waited on the upcoming slot's fence, the frame that last used that slot is
     * recorded as completed.
     */
    public void advance() {
        current = (current + 1) % count;
        complete(++frame - count);
    }

    /**
     * Records that every frame up to and including {@code frame} has completed on the device,
     * e.g. after polling the fences of frames in flight or reading a timeline semaphore. This
     * lets resources be reused sooner than {@link #advance()} alone would allow. Values older
     * than the current completed frame are ignored.
     *
     * @param frame newest frame known to be completed
     */
    public void complete(long frame) {
        if (frame > this.frame) {
            throw new IllegalArgumentException("Frame " + frame + " has not started yet.");
        }
        completed = Math.max(completed, frame);
    }

    @Override
    public int current() {
        return current;
    }

    @Override
    public int count() {
        return count;
    }

    @Override
    public long frame() {
        return frame;
    }

    @Override
    public long completed() {
        return completed;
    }

}
