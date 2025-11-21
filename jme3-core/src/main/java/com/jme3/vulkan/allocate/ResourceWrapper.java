package com.jme3.vulkan.allocate;

public interface ResourceWrapper <T> {

    T get();

    void release();

}
