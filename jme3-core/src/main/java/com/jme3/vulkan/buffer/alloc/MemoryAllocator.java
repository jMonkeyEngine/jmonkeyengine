package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.images.newimage.ImageInfo;
import com.jme3.vulkan.util.Flag;

public interface MemoryAllocator {

    /**
     * Creates a buffer intended for efficient device access and for access by the host.
     *
     * @param capacity buffer size in bytes
     * @param roles roles of the buffer
     * @return allocated buffer
     */
    EngineBuffer createDynamicBuffer(int capacity, Flag<EngineBuffer.Role> roles);

    /**
     * Creates a buffer intended for efficient device access and for reading by the host.
     *
     * @param capacity buffer size in bytes
     * @param roles roles of the buffer
     * @return allocated buffer
     */
    EngineBuffer createReadbackBuffer(int capacity, Flag<EngineBuffer.Role> roles);

    /**
     * Creates a buffer intended only for device access.
     *
     * @param capacity
     * @param roles
     * @return
     */
    EngineBuffer createLocalBuffer(int capacity, Flag<EngineBuffer.Role> roles);

    /**
     * Creates a buffer intended for efficient host access and for reading by the device.
     * If the intended buffer is large and is accessed often by the device, prefer using
     * {@link #createDynamicBuffer(int, Flag)} instead.
     *
     * @param capacity
     * @param roles
     * @return
     */
    EngineBuffer createStreamingBuffer(int capacity, Flag<EngineBuffer.Role> roles);

    default EngineBuffer createBuffer(BufferType type, int capacity, Flag<EngineBuffer.Role> roles) {
        switch (type) {
            case Dynamic:   return createDynamicBuffer(capacity, roles);
            case Readback:  return createReadbackBuffer(capacity, roles);
            case Local:     return createLocalBuffer(capacity, roles);
            case Streaming: return createStreamingBuffer(capacity, roles);
            default: throw new UnsupportedOperationException("Type not implemented: " + type);
        }
    }

    EngineImage createImage(ImageInfo info);

}
