package com.jme3.vulkan.material.experimental;

public class SetLayout <T extends ShaderBindingSet> {

    private final T set;
    private final int location;

    public SetLayout(T set, int location) {
        this.set = set;
        this.location = location;
    }

    public T getSet() {
        return set;
    }

    public int getLocation() {
        return location;
    }

}
