package com.jme3.vulkan.util;

public interface AdaptiveEnum <T extends IntEnum> extends IntEnum<T> {

    T set(int enumVal);

}
