package com.jme3.vulkan.material.experimental;

import java.util.Objects;

public class SetBindCommand<T extends ShaderBindingSet> {

    private final T set;
    private final int dynamicOffset;

    public SetBindCommand(T set) {
        this(set, 0);
    }

    public SetBindCommand(T set, int dynamicOffset) {
        this.set = set;
        this.dynamicOffset = dynamicOffset;
    }

    public T getSet() {
        return set;
    }

    public int getDynamicOffset() {
        return dynamicOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SetBindCommand setBind = (SetBindCommand) o;
        return dynamicOffset == setBind.dynamicOffset && set == setBind.set;
    }

    @Override
    public int hashCode() {
        return Objects.hash(System.identityHashCode(set), dynamicOffset);
    }

}
