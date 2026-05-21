package com.jme3.vulkan.material.experimental;

public interface ShaderBindingSet {

    void stage(int binding, Object value);

    void write();

    default SetBindCommand bind(int dynamicOffset) {
        return new SetBindCommand(this, dynamicOffset);
    }

    default SetBindCommand bind() {
        return new SetBindCommand(this, 0);
    }

}
