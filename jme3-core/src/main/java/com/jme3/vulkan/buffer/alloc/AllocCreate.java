package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.util.vma.Vma.*;

public enum AllocCreate implements Flag<AllocCreate> {

    HostAccessRandom(VMA_ALLOCATION_CREATE_HOST_ACCESS_RANDOM_BIT),
    HostAccessSequential(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT),
    HostAccessAllowTransferInstead(VMA_ALLOCATION_CREATE_HOST_ACCESS_ALLOW_TRANSFER_INSTEAD_BIT),
    CanAlias(VMA_ALLOCATION_CREATE_CAN_ALIAS_BIT),
    DedicatedMemory(VMA_ALLOCATION_CREATE_DEDICATED_MEMORY_BIT),
    DontBind(VMA_ALLOCATION_CREATE_DONT_BIND_BIT),
    CreateMapped(VMA_ALLOCATION_CREATE_MAPPED_BIT),
    NeverAllocate(VMA_ALLOCATION_CREATE_NEVER_ALLOCATE_BIT),
    StrategyBestFit(VMA_ALLOCATION_CREATE_STRATEGY_BEST_FIT_BIT),
    StrategyMinMemory(VMA_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT),
    StrategyMinTime(VMA_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT),
    UpperAddress(VMA_ALLOCATION_CREATE_UPPER_ADDRESS_BIT),
    UserDataCopyString(VMA_ALLOCATION_CREATE_USER_DATA_COPY_STRING_BIT),
    WithinBudget(VMA_ALLOCATION_CREATE_WITHIN_BUDGET_BIT);

    private final int bits;

    AllocCreate(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }

}
