package com.jme3.vulkan.memory;

import com.jme3.vulkan.util.Flag;

import static org.lwjgl.vulkan.VK14.*;

public enum MemHeapProp implements Flag<MemHeapProp> {

    DeviceLocal(VK_MEMORY_HEAP_DEVICE_LOCAL_BIT),
    MultiInstance(VK_MEMORY_HEAP_MULTI_INSTANCE_BIT);

    private final int bits;

    MemHeapProp(int bits) {
        this.bits = bits;
    }

    @Override
    public int bits() {
        return bits;
    }

}
