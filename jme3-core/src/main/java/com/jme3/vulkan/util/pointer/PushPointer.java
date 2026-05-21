package com.jme3.vulkan.util.pointer;

import java.util.ArrayList;
import java.util.Collection;

public class PushPointer <T> implements Pointer<T> {

    private T value;
    private final Collection<Memory<? super T>> downstream = new ArrayList<>();

    public PushPointer(T value) {
        this.value = value;
    }

    @Override
    public void addDownstream(Memory<? super T> ptr) {
        downstream.add(ptr);
        ptr.set(value);
    }

    @Override
    public void set(T value) {
        this.value = value;
        for (Memory<? super T> ptr : downstream) {
            ptr.set(value);
        }
    }

    @Override
    public T get() {
        return value;
    }

}
