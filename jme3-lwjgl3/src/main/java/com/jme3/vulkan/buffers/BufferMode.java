package com.jme3.vulkan.buffers;

/**
 * Hint for communicating the mode of buffer to use. Does not
 * map directly with buffer implementations.
 */
public enum BufferMode {

    /**
     * Buffer is best suited for no modifications. A static buffers is usually
     * only accessible by the GPU after the initial upload, and may throw an
     * exception if attempting to do so.
     */
    Static,

    /**
     * Buffer is best suited for occassional modifications. Usually such buffers
     * use fast GPU memory, with a CPU-accessible intermediate buffer to transfer
     * updates from the CPU to the GPU. Similar to {@link #Static}, except the
     * intermediate buffer is not destroyed after the initial upload.
     */
    Adjustable,

    /**
     * Buffer is best suited for regular modifications, ideally every frame.
     * Usually such buffers emphasize CPU access, usually to the detriment of
     * GPU access speeds.
     */
    Dynamic,

}
