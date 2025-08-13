package com.jme3.vulkan.descriptors;

import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

import static org.lwjgl.vulkan.VK10.*;

public class SetLayoutBinding {

    private final Descriptor type;
    private final int binding, descriptors, stages;

    public SetLayoutBinding(Descriptor type, int binding, int descriptors, int stages) {
        this.type = type;
        this.binding = binding;
        this.descriptors = descriptors;
        this.stages = stages;
    }

    @SuppressWarnings("DataFlowIssue")
    public void fillLayoutBinding(VkDescriptorSetLayoutBinding layoutBinding) {
        layoutBinding.descriptorType(type.getVkEnum())
                .binding(binding)
                .descriptorCount(descriptors)
                .stageFlags(stages)
                .pImmutableSamplers(null);
    }

    public Descriptor getType() {
        return type;
    }

    public int getBinding() {
        return binding;
    }

    public int getDescriptors() {
        return descriptors;
    }

    public int getStages() {
        return stages;
    }

}
