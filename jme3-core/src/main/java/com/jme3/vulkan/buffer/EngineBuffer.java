package com.jme3.vulkan.buffer;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

public interface EngineBuffer {

    /**
     * Updates this buffer. Changes made to the {@link #cache()} are flushed to the device.
     * If the cache has not been changed and {@link #invalidateCache()} has been called since
     * the last update, the cache is updated from the device.
     *
     * @param cmd rendering commands
     */
    void update(CommandBuffer cmd);

    /**
     * Gets the CPU cache of this buffer as long as it is {@link #isDeviceAccessible() device accessible}.
     *
     * @return host-side cache
     * @throws UnsupportedOperationException if this buffer is not {@link #isHostAccessible()
     * device accessible}
     */
    DataBuffer cache();

    /**
     * Submits a cache invalidation to be performed on the next {@link #update(CommandBuffer)}.
     * Invalidating makes changes on device visible in the {@link #cache()} on the host.
     */
    void invalidateCache();

    /**
     * Gets the capacity in bytes of this buffer.
     *
     * @return capacity in bytes
     */
    int capacity();

    /**
     * Gets the internal offset in bytes of this buffer. This is used if this buffer
     * defers to another buffer and applies an offset. Make sure that whenever
     * an EngineBuffer is interacted with using the graphics API to apply this offset.
     *
     * @return internal offset in bytes
     */
    int getBufferLocalOffset();

    /**
     * Gets the graphics API handle of this buffer.
     *
     * @return graphics API handle
     */
    long getHandle();

    /**
     * Gets the address of this buffer on the device.
     *
     * @return device address
     */
    long getDeviceAddress();

    /**
     * Gets the abilities of this buffer. This buffer may only perform tasks allowed
     * by its abilities.
     *
     * @return buffer abilities
     */
    Flag<BufferRole> getRoles();

    /**
     * Gets properties of the memory backing this buffer.
     *
     * @return memory properties
     */
    Flag<MemoryProp> getMemoryProperties();

    /**
     * Returns true if this buffer is accessible by the device through {@link #getHandle()} and {@link #getDeviceAddress()}.
     *
     * @return true if device accessible
     */
    boolean isDeviceAccessible();

    /**
     * Returns true if the memory backing this buffer is accessible by the host.
     *
     * @return true if host accessible
     */
    default boolean isHostAccessible() {
        return getMemoryProperties().contains(MemoryProp.HostVisible);
    }

}
