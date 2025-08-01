package com.jme3.vulkan.descriptors;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

public class SetLayoutBinding {

    private final int type, binding, descriptors, stages;

    public SetLayoutBinding(int type, int binding, int descriptors, int stages) {
        this.type = type;
        this.binding = binding;
        this.descriptors = descriptors;
        this.stages = stages;
    }

    @SuppressWarnings("DataFlowIssue")
    public void fillLayoutBinding(VkDescriptorSetLayoutBinding layoutBinding) {
        layoutBinding.descriptorType(type)
                .binding(binding)
                .descriptorCount(descriptors)
                .stageFlags(stages)
                .pImmutableSamplers(null);
    }

    public int getType() {
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

    public static SetLayoutBinding uniformBuffer(int binding, int descriptors, int stages) {
        return new SetLayoutBinding(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, binding, descriptors, stages);
    }

    public static SetLayoutBinding combinedImageSampler(int binding, int descriptors, int stages) {
        return new SetLayoutBinding(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, binding, descriptors, stages);
    }

}
