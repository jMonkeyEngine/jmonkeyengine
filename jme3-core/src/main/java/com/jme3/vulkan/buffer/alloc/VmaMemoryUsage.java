package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.util.vma.Vma.*;

/**
 * Wraps VMA's {@code VmaMemoryUsage}, passed as {@code VmaAllocationCreateInfo::usage}
 * to tell VMA what an allocation will be used for.
 * <p>
 * Unlike {@code VmaAllocationCreateFlagBits} (see {@link VmaAllocationFlag}), these
 * values are mutually exclusive rather than combinable bits, so this implements
 * {@link IntEnum} rather than {@link com.jme3.vulkan.util.Flag}.
 */
public enum VmaMemoryUsage implements IntEnum<VmaMemoryUsage> {

    /**
     * No intended usage specified; memory requirements are instead taken entirely from
     * the other fields of {@code VmaAllocationCreateInfo}.
     */
    Unknown(VMA_MEMORY_USAGE_UNKNOWN),

    /**
     * Lazily allocated memory, with {@code VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT}.
     * Mostly available on mobile platforms; allocation fails on devices with no such
     * memory type. Intended for transient attachment images (color, depth, etc.)
     * created with {@code VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT}. Always allocated
     * as if {@link VmaAllocation#DedicatedMemory} were set.
     */
    GpuLazilyAllocated(VMA_MEMORY_USAGE_GPU_LAZILY_ALLOCATED),

    /**
     * Selects the best memory type automatically; recommended for most common cases.
     * To map the allocation, {@link VmaAllocation#HostAccessSequentialWrite} or
     * {@link VmaAllocation#HostAccessRandom} must also be set. Only valid with
     * calls that provide a {@code VkBufferCreateInfo} or {@code VkImageCreateInfo},
     * such as {@code vmaCreateBuffer}/{@code vmaCreateImage}, not with generic memory
     * allocation functions.
     */
    Auto(VMA_MEMORY_USAGE_AUTO),

    /**
     * Like {@link #Auto}, but prefers GPU (device) memory when choosing automatically.
     * Carries the same host-access-flag and buffer/image-only requirements as
     * {@link #Auto}.
     */
    AutoPreferDevice(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE),

    /**
     * Like {@link #Auto}, but prefers CPU (host) memory when choosing automatically.
     * Carries the same host-access-flag and buffer/image-only requirements as
     * {@link #Auto}.
     */
    AutoPreferHost(VMA_MEMORY_USAGE_AUTO_PREFER_HOST);

    private final int vmaEnum;

    VmaMemoryUsage(int vmaEnum) {
        this.vmaEnum = vmaEnum;
    }

    @Override
    public int getEnum() {
        return vmaEnum;
    }

}
