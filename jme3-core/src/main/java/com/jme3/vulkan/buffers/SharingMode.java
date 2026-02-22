package com.jme3.vulkan.buffers;

import com.jme3.vulkan.util.IntEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum SharingMode implements IntEnum<SharingMode> {

    Exclusive(VK_SHARING_MODE_EXCLUSIVE),
    Concurrent(VK_SHARING_MODE_CONCURRENT);

    private final int vk;

    SharingMode(int vk) {
        this.vk = vk;
    }

    @Override
    public int getEnum() {
        return vk;
    }

}
