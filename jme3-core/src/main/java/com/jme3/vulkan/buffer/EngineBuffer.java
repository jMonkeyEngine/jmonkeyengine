package com.jme3.vulkan.buffer;

import com.jme3.vulkan.alloc.MemoryAddress;

public interface EngineBuffer extends MemoryAddress {

    void copyToPreferOptimal(EngineBuffer dst);

    void copyToPreferHost(EngineBuffer dst);

    void copyToPreferDevice(EngineBuffer dst);

    /**
     * Resizes this buffer to at least the specified size in bytes.
     *
     * @param size size in bytes to resize to
     * @throws UnsupportedOperationException if resizing to at least the requested size is not
     * supported by the implementation
     */
    void resize(int size) throws UnsupportedOperationException;

    /**
     * Stages a section of this buffer to be interacted with by various buffer operations.
     * Implementations are required to stage at least the given region, but are free to stage
     * more if necessary.
     *
     * @param offset region offset from the start of this buffer in bytes
     * @param size region size in bytes
     */
    void stage(int offset, int size);

    /**
     * Makes all staged sections of this buffer available externally.
     */
    void pushStaged();

    /**
     * Pulls external updates for all staged sections of this buffer.
     */
    void pullStaged();

    /**
     * Clears all staging information of this buffer so that no sections of this buffer are staged.
     */
    void clearStaging();

    /**
     * Gets the size of this buffer in bytes.
     *
     * @return size in bytes
     */
    int size();

    /**
     * Stages this entire buffer
     */
    default void stageAll() {
        stage(0, size());
    }

}
