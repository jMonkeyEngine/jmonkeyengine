package com.jme3.vulkan.util.pointer;

public interface Pointer <T> extends Memory<T> {

    void addDownstream(Memory<? super T> ptr);

}
