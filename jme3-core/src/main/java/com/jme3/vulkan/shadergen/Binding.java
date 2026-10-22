package com.jme3.vulkan.shadergen;

import java.util.Objects;

public class Binding {

    private final int set, binding;

    public Binding(int binding) {
        this(0, binding);
    }

    public Binding(int set, int binding) {
        this.set = set;
        this.binding = binding;
    }

    public int getSet() {
        return set;
    }

    public int getBinding() {
        return binding;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Binding binding1 = (Binding) o;
        return set == binding1.set && binding == binding1.binding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(set, binding);
    }

}
