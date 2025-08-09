package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class DescriptorSetWriter {

    private final int type, binding, arrayElement, descriptorCount;

    public DescriptorSetWriter(int type, int binding, int arrayElement, int descriptorCount) {
        this.type = type;
        this.binding = binding;
        this.arrayElement = arrayElement;
        this.descriptorCount = descriptorCount;
    }

    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorType(type).dstBinding(binding)
                .dstArrayElement(arrayElement)
                .descriptorCount(descriptorCount);
    }

    public int getType() {
        return type;
    }

    public int getBinding() {
        return binding;
    }

    public int getArrayElement() {
        return arrayElement;
    }

    public int getDescriptorCount() {
        return descriptorCount;
    }

}
