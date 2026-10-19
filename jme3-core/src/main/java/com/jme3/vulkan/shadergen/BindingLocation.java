package com.jme3.vulkan.shadergen;

public class BindingLocation {

    private final int set, binding;

    public BindingLocation(int binding) {
        this(-1, binding);
    }

    public BindingLocation(int set, int binding) {
        this.set = set;
        this.binding = binding;
    }

    @Override
    public String toString() {
        return (set >= 0 ? "set=0," : "") + ",binding=" + binding;
    }

    public int getSet() {
        return set;
    }

    public int getBinding() {
        return binding;
    }

}
