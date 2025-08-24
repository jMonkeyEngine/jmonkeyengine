package com.jme3.vulkan.descriptors;

import static org.lwjgl.vulkan.VK10.*;

public enum Descriptor {

    UniformBuffer(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER),
    UniformBufferDynamic(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC),
    StorageBuffer(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER),
    StorageBufferDynamic(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC),
    CombinedImageSampler(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER),
    Sampler(VK_DESCRIPTOR_TYPE_SAMPLER),
    SampledImage(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE),
    InputAttachment(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT),
    StorageImage(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE),
    StorageTexelBuffer(VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER),
    UniformTexelBuffer(VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER);

    private final int vkEnum;

    Descriptor(int vkEnum) {
        this.vkEnum = vkEnum;
    }

    public int getVkEnum() {
        return vkEnum;
    }

}
