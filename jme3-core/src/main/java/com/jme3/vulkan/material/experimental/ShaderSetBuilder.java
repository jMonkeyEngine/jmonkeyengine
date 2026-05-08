package com.jme3.vulkan.material.experimental;

import com.jme3.vulkan.descriptors.UniformBinding;

public interface ShaderSetBuilder {

    void addBinding(int location, UniformBinding binding);

}
