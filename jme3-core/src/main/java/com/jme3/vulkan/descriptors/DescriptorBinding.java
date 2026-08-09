package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.ExactBufferTracker;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.BitSet;

public abstract class DescriptorBinding <T> {

    protected final T[] resources;
    private final DescriptorType type;
    private final Flag<ShaderStage> stages;
    private final BufferTracker writesNeeded = new ExactBufferTracker();
    private final BitSet usedSlots = new BitSet();

    public DescriptorBinding(DescriptorType type, T[] resources, Flag<ShaderStage> stages) {
        this.type = type;
        this.resources = resources;
        this.stages = stages;
    }

    public void populateLayoutInfo(VkDescriptorSetLayoutBinding layoutBinding) {
        layoutBinding.descriptorType(type.getEnum())
            .descriptorCount(resources.length)
            .stageFlags(stages.bits());
    }

    public void populateWriteInfo(MemoryStack stack, VkWriteDescriptorSet.Buffer write, DescriptorSet set, int binding) {
        for (BufferTracker.Island i : writesNeeded) {
            VkWriteDescriptorSet w = write.get();
            populateElementInfo(stack, w, i.getStart(), i.getSize());
            w.sType$Default()
                .descriptorType(type.getEnum())
                .dstArrayElement(i.getStart())
                .descriptorCount(i.getSize())
                .dstSet(set.getNativeObject())
                .dstBinding(binding);
        }
    }

    protected abstract void populateElementInfo(MemoryStack stack, VkWriteDescriptorSet write, int start, int count);

    public void set(int descriptor, T resource) {
        resources[descriptor] = resource;
        writesNeeded.add(descriptor, 1);
        usedSlots.set(descriptor, resource != null);
    }

    public int setInFirstUnused(T resource) {
        int i = usedSlots.nextClearBit(0);
        set(i, resource);
        return i;
    }

    public void setWriteNeeded(int descriptor) {
        writesNeeded.add(descriptor, 1);
    }

    public void setWriteNeeded(int firstDescriptor, int descriptorCount) {
        writesNeeded.add(firstDescriptor, descriptorCount);
    }

    public T get(int descriptor) {
        return resources[descriptor];
    }

    public void clear(int descriptor) {
        set(descriptor, null);
    }

}
