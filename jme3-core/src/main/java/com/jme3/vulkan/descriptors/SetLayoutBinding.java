package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

public class SetLayoutBinding {

    private final IntEnum<Descriptor> type;
    private final int binding, descriptors;
    private final Flag<ShaderStage> stages;

    public SetLayoutBinding(IntEnum<Descriptor> type, int binding, int descriptors) {
        this(type, binding, descriptors, ShaderStage.All);
    }

    public SetLayoutBinding(IntEnum<Descriptor> type, int binding, int descriptors, Flag<ShaderStage> stages) {
        this.type = type;
        this.binding = binding;
        this.descriptors = descriptors;
        this.stages = stages;
    }

    @SuppressWarnings("DataFlowIssue")
    public void fillLayoutBinding(VkDescriptorSetLayoutBinding layoutBinding) {
        layoutBinding.descriptorType(type.getEnum())
                .binding(binding)
                .descriptorCount(descriptors)
                .stageFlags(stages.bits())
                .pImmutableSamplers(null);
    }

    public IntEnum<Descriptor> getType() {
        return type;
    }

    public int getBinding() {
        return binding;
    }

    public int getDescriptors() {
        return descriptors;
    }

    public Flag<ShaderStage> getStages() {
        return stages;
    }

}
