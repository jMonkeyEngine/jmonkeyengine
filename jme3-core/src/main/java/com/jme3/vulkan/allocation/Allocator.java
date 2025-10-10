package com.jme3.vulkan.allocation;

public interface Allocator <T, K> {

    T allocate(K key);

    void release(T resource);

}
