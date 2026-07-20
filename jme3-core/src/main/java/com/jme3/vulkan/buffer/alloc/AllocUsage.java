package com.jme3.vulkan.buffer.alloc;

import static org.lwjgl.util.vma.Vma.*;

public enum AllocUsage {

    Auto(VMA_MEMORY_USAGE_AUTO),
    PreferHost(VMA_MEMORY_USAGE_AUTO_PREFER_HOST),
    PreferDevice(VMA_MEMORY_USAGE_AUTO_PREFER_DEVICE);

    private final int e;

    AllocUsage(int e) {
        this.e = e;
    }

    public int getEnum() {
        return e;
    }

}
