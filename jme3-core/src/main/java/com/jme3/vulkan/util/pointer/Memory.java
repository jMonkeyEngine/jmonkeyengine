package com.jme3.vulkan.util.pointer;

public interface Memory <T> {

    void set(T value);

    T get();

}
