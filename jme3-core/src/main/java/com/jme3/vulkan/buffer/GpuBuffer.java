package com.jme3.vulkan.buffer;

import com.jme3.util.destruction.Destructable;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK10.*;

public interface GpuBuffer extends EngineBuffer, Destructable {

    enum Role implements Flag<Role> {

        None(0),
        Uniform(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT),
        Index(VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
        Storage(VK_BUFFER_USAGE_STORAGE_BUFFER_BIT),
        StorageTexel(VK_BUFFER_USAGE_STORAGE_TEXEL_BUFFER_BIT),
        Indirect(VK_BUFFER_USAGE_INDIRECT_BUFFER_BIT),
        TransferDst(VK_BUFFER_USAGE_TRANSFER_DST_BIT),
        TransferSrc(VK_BUFFER_USAGE_TRANSFER_SRC_BIT),
        UniformTexel(VK_BUFFER_USAGE_UNIFORM_TEXEL_BUFFER_BIT),
        Vertex(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);

        private final int vkEnum;

        Role(int vkEnum) {
            this.vkEnum = vkEnum;
        }

        @Override
        public int bits() {
            return vkEnum;
        }

    }

    /**
     * Flushes this buffer's cache to the device.
     */
    void flushCache();

    /**
     * Invalidates this buffer's cache so that it is refreshed from the device.
     */
    void invalidateCache();

    /**
     * Gets the capacity in bytes of this buffer.
     *
     * @return capacity in bytes
     */
    int capacity();

    /**
     * Offset of the region described by this buffer object relative
     * to the actual buffer start.
     *
     * @return offset in bytes
     */
    int offset();

    /**
     * Gets the graphics API handle of this buffer.
     *
     * @return graphics API handle
     */
    long getHandle();

    /**
     * Gets the abilities of this buffer. This buffer may only perform tasks allowed
     * by its abilities.
     *
     * @return buffer abilities
     */
    Flag<Role> getRoles();

    /**
     * Gets properties of the memory backing this buffer.
     *
     * @return memory properties
     */
    Flag<MemoryProp> getMemoryProperties();

    /**
     * Returns true if the memory backing this buffer is accessible by the host.
     *
     * @return true if host accessible
     */
    default boolean isHostAccessible() {
        return getMemoryProperties().contains(MemoryProp.HostVisible);
    }

}
