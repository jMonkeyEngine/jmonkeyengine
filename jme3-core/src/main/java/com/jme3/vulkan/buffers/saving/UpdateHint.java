package com.jme3.vulkan.buffers.saving;

/**
 * Hints at how often data is to be updated on the GPU by the CPU and vise versa,
 * so that buffers can be optimized for that update rate. Violating a buffer's
 * update hint can result in doing operations the buffer is poorly optimized
 * for.
 *
 * <p>For example, treating a {@link #Stream} buffer as static can suffer
 * in performance due to frequent reads from unoptimal memory. Likewise,
 * treating a {@link #Static} buffer as streaming can result in frequent
 * writes to memory that the CPU cannot access directly.</p>
 */
public enum UpdateHint {

    /**
     * Data is updated on the GPU every frame.
     */
    Stream,

    /**
     * Data is updated on the GPU after initialization, but not regularly.
     */
    Dynamic,

    /**
     * Data is never on the GPU updated after initialization.
     */
    Static,

    /**
     * Data is never updated on the GPU, and stays local to the CPU.
     */
    Never

}
