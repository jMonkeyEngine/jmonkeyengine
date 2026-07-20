package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.buffer.BufferRole;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.util.Flag;

public interface BufferAllocator {

    /**
     * Creates a buffer intended for efficient device access and for access by the host.
     *
     * @param capacity buffer size in bytes
     * @param roles roles of the buffer
     * @return allocated buffer
     */
    EngineBuffer createDynamicBuffer(int capacity, Flag<BufferRole> roles);

    /**
     * Creates a buffer intended for efficient device access and for reading by the host.
     *
     * @param capacity buffer size in bytes
     * @param roles roles of the buffer
     * @return allocated buffer
     */
    EngineBuffer createReadbackBuffer(int capacity, Flag<BufferRole> roles);

    /**
     * Creates a buffer intended only for device access.
     *
     * @param capacity
     * @param roles
     * @return
     */
    EngineBuffer createLocalBuffer(int capacity, Flag<BufferRole> roles);

    /**
     * Creates a buffer intended for efficient host access and for reading by the device.
     * If the intended buffer is large and is accessed often by the device, prefer using
     * {@link #createDynamicBuffer(int, Flag)} instead.
     *
     * @param capacity
     * @param roles
     * @return
     */
    EngineBuffer createStagingBuffer(int capacity, Flag<BufferRole> roles);

}
