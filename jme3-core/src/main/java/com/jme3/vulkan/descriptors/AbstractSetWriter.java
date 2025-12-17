package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public abstract class AbstractSetWriter implements DescriptorSetWriter {

    protected final SetLayoutBinding binding;
    protected final int arrayElement, count;

    protected AbstractSetWriter(SetLayoutBinding binding, int arrayElement, int count) {
        this.binding = binding;
        this.arrayElement = arrayElement;
        this.count = count;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorCount(count)
            .dstArrayElement(arrayElement)
            .dstBinding(binding.getBinding())
            .descriptorType(binding.getType().getEnum());
        populate(stack, write);
    }

    protected abstract void populate(MemoryStack stack, VkWriteDescriptorSet write);

}
