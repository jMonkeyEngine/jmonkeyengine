package com.jme3.vulkan;

public class Handle <T> {

    private final T object;
    private int refs = 1;

    public Handle(T object) {
        this.object = object;
    }

    public T reference() {
        refs++;
        return object;
    }

    public T get() {
        return object;
    }

    public void release() {
        refs--;
    }

}
