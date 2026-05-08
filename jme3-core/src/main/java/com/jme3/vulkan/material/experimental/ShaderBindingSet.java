package com.jme3.vulkan.material.experimental;

public interface ShaderBindingSet {

    void stage(int binding, Object value);

    void write();

    default SetBind bind() {
        return new SetBind(this);
    }

    default SetBind bind(int offset) {
        return new SetBind(this, offset);
    }

    default SetLayout layout(int location) {
        return new SetLayout(this, location);
    }

}
