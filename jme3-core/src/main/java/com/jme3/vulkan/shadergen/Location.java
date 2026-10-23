package com.jme3.vulkan.shadergen;

import java.util.Objects;

public class Location {

    private final int set, binding;

    public Location(int binding) {
        this(0, binding);
    }

    public Location(int set, int binding) {
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
        Location location1 = (Location) o;
        return set == location1.set && binding == location1.binding;
    }

    @Override
    public int hashCode() {
        return Objects.hash(set, binding);
    }

    public boolean bindingUnknown() {
        return binding < 0;
    }

}
