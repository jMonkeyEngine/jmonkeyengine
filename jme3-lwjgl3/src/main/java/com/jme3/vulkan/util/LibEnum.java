package com.jme3.vulkan.util;

public interface LibEnum <T extends LibEnum> {

    int getEnum();

    default boolean is(LibEnum<T> vk) {
        return vk != null && getEnum() == vk.getEnum();
    }

}
