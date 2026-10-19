package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.util.vma.Vma.*;

/**
 * Wraps VMA's {@code VmaAllocationCreateFlagBits}, passed as
 * {@code VmaAllocationCreateInfo::flags} to control how and where VMA places an
 * allocation.
 */
public enum VmaAllocation implements Flag<VmaAllocation> {

    /**
     * Gives the allocation its own dedicated {@code VkDeviceMemory} block instead of
     * sharing one with other allocations. Intended for large, special-purpose
     * resources, such as full-screen attachment images.
     */
    DedicatedMemory(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT),

    /**
     * Only allows this allocation to be placed in an existing {@code VkDeviceMemory}
     * block; fails with an out-of-memory error rather than creating a new block.
     * Should not be combined with {@link #DedicatedMemory}.
     */
    NeverAllocate(VMA_ALLOCATION_CREATE_NEVER_ALLOCATE_BIT),

    /**
     * Keeps the allocation persistently mapped, with the pointer available through
     * the allocation info, instead of requiring an explicit map call. Silently
     * ignored, leaving the memory unmapped, if the allocation isn't host-visible, so
     * it's safe to combine with a device-local-preferring usage that only sometimes
     * falls back to mappable memory.
     */
    Mapped(VMA_ALLOCATION_CREATE_MAPPED_BIT),

    /**
     * Preserved for backward compatibility; prefer naming the allocation directly
     * instead. Treats the allocation's user data as a null-terminated string and
     * stores a private copy of it rather than just the pointer.
     */
    UserDataCopyString(VMA_ALLOCATION_CREATE_USER_DATA_COPY_STRING_BIT),

    /**
     * Allocates from the upper stack of a double-stack custom pool. Only valid for
     * pools created with VMA's linear allocation algorithm.
     */
    UpperAddress(VMA_ALLOCATION_CREATE_UPPER_ADDRESS_BIT),

    /**
     * Creates the buffer or image and its memory without binding them together,
     * leaving that for the caller to do manually (e.g. to use an extension-specific
     * bind). Combine with {@link #CanAlias} if the allocation might end up in its own
     * dedicated block and the resource shouldn't be tied to it exclusively.
     */
    DontBind(VMA_ALLOCATION_CREATE_DONT_BIND_BIT),

    /**
     * Fails the allocation with an out-of-memory error rather than exceeding the
     * device's current memory budget, instead of leaving that decision to the driver.
     * Useful for resources that aren't essential, such as lower mip levels of a
     * texture streamed in over time.
     */
    WithinBudget(VMA_ALLOCATION_CREATE_WITHIN_BUDGET_BIT),

    /**
     * Declares that the allocated memory will have other resources aliased onto it.
     * Prevents VMA from marking a {@link #DedicatedMemory} allocation as dedicated at
     * the Vulkan level, which would otherwise make it unsuitable for aliasing.
     */
    CanAlias(VMA_ALLOCATION_CREATE_CAN_ALIAS_BIT),

    /**
     * Requests that the allocation be mappable, declaring that the mapped memory will
     * only ever be written sequentially (e.g. via a straight {@code memcpy}), never
     * read or written out of order. Lets VMA prefer uncached, write-combined memory,
     * which is fast to write but very slow to read back from. Required to map an
     * allocation made with an {@code AUTO} memory usage; for other usages it's ignored
     * and mapping just works whenever the memory is host-visible.
     */
    HostAccessSequentialWrite(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT),

    /**
     * Requests that the allocation be mappable, declaring that the mapped memory may
     * be read, written, and accessed in any order. Lets VMA prefer host-cached memory,
     * which is efficient for CPU reads. Required to map an allocation made with an
     * {@code AUTO} memory usage; for other usages it's ignored and mapping just works
     * whenever the memory is host-visible.
     */
    HostAccessRandom(VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT),

    /**
     * Combined with {@link #HostAccessSequentialWrite} or {@link #HostAccessRandom},
     * allows VMA to still pick non-host-visible memory if that would be faster
     * overall. The caller is then responsible for checking whether the allocation
     * actually landed in host-visible memory and, if not, staging the transfer
     * through a separate buffer (which needs matching transfer usage flags of its
     * own).
     */
    HostAccessAllowTransferInstead(VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT),

    /**
     * Allocation strategy that searches for the smallest free range that fits, to
     * minimize memory usage and fragmentation at the cost of allocation time.
     */
    StrategyMinMemory(VMA_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT),

    /**
     * Allocation strategy that takes the first free range that fits, prioritizing
     * fast allocation over how well-packed the result is.
     */
    StrategyMinTime(VMA_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT),

    /**
     * Allocation strategy that always chooses the lowest available offset, producing
     * tightly packed allocations at some cost to allocation speed. Used internally by
     * defragmentation; not recommended for general use.
     */
    StrategyMinOffset(VMA_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT);

    private final int vmaEnum;

    VmaAllocation(int vmaEnum) {
        this.vmaEnum = vmaEnum;
    }

    @Override
    public int bits() {
        return vmaEnum;
    }

}
