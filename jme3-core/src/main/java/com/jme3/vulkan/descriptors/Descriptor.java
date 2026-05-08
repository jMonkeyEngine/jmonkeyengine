package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.util.IntEnum;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public enum Descriptor implements IntEnum<Descriptor> {

    UniformBuffer(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER),
    UniformBufferDynamic(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC, true),
    StorageBuffer(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER),
    StorageBufferDynamic(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER_DYNAMIC, true),
    CombinedImageSampler(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER),
    Sampler(VK_DESCRIPTOR_TYPE_SAMPLER),
    SampledImage(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE),
    InputAttachment(VK_DESCRIPTOR_TYPE_INPUT_ATTACHMENT),
    StorageImage(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE),
    StorageTexelBuffer(VK_DESCRIPTOR_TYPE_STORAGE_TEXEL_BUFFER),
    UniformTexelBuffer(VK_DESCRIPTOR_TYPE_UNIFORM_TEXEL_BUFFER);

    private final int vkEnum;
    private final boolean dynamicOffsets;

    Descriptor(int vkEnum) {
        this(vkEnum, false);
    }

    Descriptor(int vkEnum, boolean dynamicOffsets) {
        this.vkEnum = vkEnum;
        this.dynamicOffsets = dynamicOffsets;
    }

    @Override
    public int getEnum() {
        return vkEnum;
    }

    public boolean isDynamicOffsets() {
        return dynamicOffsets;
    }

    private static final Map<String, IntEnum<Descriptor>> custom = new HashMap<>();

}
