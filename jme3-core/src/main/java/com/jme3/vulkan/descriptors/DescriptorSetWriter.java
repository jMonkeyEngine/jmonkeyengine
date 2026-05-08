package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public interface DescriptorSetWriter {

    /**
     * Populates write information.
     *
     * @param stack memory stack
     * @param device logical device
     * @param write write information to be populated
     */
    void populateWrite(MemoryStack stack, LogicalDevice<?> device, VkWriteDescriptorSet write);

}
