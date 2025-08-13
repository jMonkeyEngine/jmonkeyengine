package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public interface DescriptorSetWriter {

    void populateWrite(MemoryStack stack, VkWriteDescriptorSet write);

    boolean isUpdateNeeded();

}
