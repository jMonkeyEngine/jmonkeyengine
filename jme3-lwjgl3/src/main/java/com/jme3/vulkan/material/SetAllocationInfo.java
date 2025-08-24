package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.DescriptorSetLayout;

public class SetAllocationInfo {

    private final UniformSet set;
    private final DescriptorSetLayout layout;

    public SetAllocationInfo(UniformSet set, DescriptorSetLayout layout) {
        this.set = set;
        this.layout = layout;
    }

    public UniformSet getSet() {
        return set;
    }

    public DescriptorSetLayout getLayout() {
        return layout;
    }

}
