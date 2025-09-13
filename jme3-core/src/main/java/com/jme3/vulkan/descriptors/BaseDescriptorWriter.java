package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class BaseDescriptorWriter implements DescriptorSetWriter {

    private final Descriptor type;
    private final int binding, arrayElement, descriptorCount;

    public BaseDescriptorWriter(Descriptor type, int binding, int arrayElement, int descriptorCount) {
        this.type = type;
        this.binding = binding;
        this.arrayElement = arrayElement;
        this.descriptorCount = descriptorCount;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorType(type.getEnum()).dstBinding(binding)
                .dstArrayElement(arrayElement)
                .descriptorCount(descriptorCount);
    }

    public Descriptor getType() {
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
