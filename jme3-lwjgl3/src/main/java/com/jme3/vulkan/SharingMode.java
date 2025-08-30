package com.jme3.vulkan;

import com.jme3.vulkan.util.LibEnum;

import static org.lwjgl.vulkan.VK10.*;

public enum SharingMode implements LibEnum<SharingMode> {

    Exclusive(VK_SHARING_MODE_EXCLUSIVE),
    Concurrent(VK_SHARING_MODE_CONCURRENT);

    private final int vkEnum;

    SharingMode(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

    public static SharingMode concurrent(boolean concurrent) {
        return concurrent ? Concurrent : Exclusive;
    }

}
