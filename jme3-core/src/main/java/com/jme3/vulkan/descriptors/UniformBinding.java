package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.ExactBufferTracker;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;

import java.util.Objects;

public abstract class UniformBinding <T> {

    private final T[] slots;
    private final DescriptorType type;
    private final Flag<ShaderStage> stages;
    private final BufferTracker slotsToUpdate = new ExactBufferTracker();

    public UniformBinding(DescriptorType type, int descriptorCount, Flag<ShaderStage> stages) {
        //noinspection unchecked
        this.slots = (T[])new Object[descriptorCount];
        this.type = type;
        this.stages = stages;
    }

    public abstract DescriptorSetWriter createWriter(LogicalDevice<?> device, T value);

    public void fillLayoutBinding(VkDescriptorSetLayoutBinding layoutBinding) {
        layoutBinding.descriptorType(type.getEnum())
            .descriptorCount(slots.length)
            .stageFlags(stages.bits())
            .pImmutableSamplers(null); // potential optimization
    }

    public DescriptorType getType() {
        return type;
    }

    public Flag<ShaderStage> getStages() {
        return stages;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UniformBinding<?> that = (UniformBinding<?>) o;
        return descriptors == that.descriptors && type == that.type && Objects.equals(stages, that.stages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, descriptors, stages);
    }

}
