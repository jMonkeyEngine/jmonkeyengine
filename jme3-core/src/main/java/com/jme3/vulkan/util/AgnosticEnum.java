package com.jme3.vulkan.util;

public interface AgnosticEnum<T extends IntEnum> extends IntEnum<T> {

    T set(int enumVal);

}
