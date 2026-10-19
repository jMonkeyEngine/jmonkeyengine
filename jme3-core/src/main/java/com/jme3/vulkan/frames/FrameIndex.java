package com.jme3.vulkan.frames;

import com.jme3.vulkan.buffer.RapidBuffer;

/**
 * Tracks which of a fixed number of frame-in-flight slots is currently active, so that
 * per-frame resources (such as {@link RapidBuffer}) know which
 * of their backing copies to use without each having to track frames themselves.
 * <p>
 * This only tracks an index; it is not itself responsible for ensuring the device has
 * finished with a slot before the CPU reuses it. Whoever advances a {@link FrameCycle}
 * (typically right after waiting on that upcoming slot's completion fence) is
 * responsible for that synchronization.
 */
public interface FrameIndex {

    /**
     * Gets the currently active frame-in-flight slot, in {@code [0, count())}.
     *
     * @return current slot index
     */
    int current();

    /**
     * Gets the fixed number of frame-in-flight slots being cycled through.
     *
     * @return slot count
     */
    int count();

}
