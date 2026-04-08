package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

import java.util.Objects;

public abstract class UniformBinding <T> {

    private final IntEnum<Descriptor> type;
    private final int binding, descriptors;
    private final Flag<ShaderStage> stages;

    public UniformBinding(IntEnum<Descriptor> type, int binding, int descriptors, Flag<ShaderStage> stages) {
        this.type = type;
        this.binding = binding;
        this.descriptors = descriptors;
        this.stages = stages;
    }

    public abstract DescriptorSetWriter createWriter(T value);

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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UniformBinding that = (UniformBinding) o;
        return binding == that.binding
                && descriptors == that.descriptors
                && Objects.equals(type, that.type)
                && Objects.equals(stages, that.stages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, binding, descriptors, stages);
    }

}
