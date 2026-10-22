package com.jme3.vulkan.shadergen;

import java.util.Objects;

public class ConstantId {

    private final int id;

    public ConstantId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ConstantId that = (ConstantId) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

}
