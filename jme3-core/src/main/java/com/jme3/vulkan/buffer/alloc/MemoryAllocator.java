package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.buffer.GpuBuffer;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.images.newimage.ImageInfo;
import com.jme3.vulkan.util.Flag;

public interface MemoryAllocator {

    /**
     * Allocates a {@link GpuBuffer} of the given {@code type}, selecting the memory
     * properties and usage pattern appropriate for that kind of buffer.
     *
     * @param type kind of buffer to allocate
     * @param capacity size in bytes
     * @param roles usage roles the buffer must support
     * @return the allocated buffer
     */
    GpuBuffer createBuffer(BufferType type, int capacity, Flag<GpuBuffer.Role> roles);

    EngineImage createImage(ImageInfo info);

}
