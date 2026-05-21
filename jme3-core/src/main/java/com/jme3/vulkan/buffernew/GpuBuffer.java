package com.jme3.vulkan.buffernew;

import com.jme3.vulkan.alloc.Memory;
import com.jme3.vulkan.alloc.NativeResource;

public interface GpuBuffer extends Memory, NativeResource {

    /**
     * Flushes staged changes to the GPU.
     */
    void flush();

    /**
     * Invalidates the CPU cache to receive changes from the GPU.
     */
    void invalidate();

    /**
     * Gets the size of this buffer in bytes.
     *
     * @return size in bytes
     */
    long size();

}
