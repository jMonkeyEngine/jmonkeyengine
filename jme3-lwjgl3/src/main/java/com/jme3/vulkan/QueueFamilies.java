package com.jme3.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

public interface QueueFamilies {

    boolean populate(PhysicalDevice device, VkQueueFamilyProperties.Buffer properties);

    VkDeviceQueueCreateInfo.Buffer createLogicalBuffers(MemoryStack stack);

    void createQueues(LogicalDevice device);

    boolean isComplete();

    IntBuffer getSwapchainConcurrentBuffers(MemoryStack stack);

}
