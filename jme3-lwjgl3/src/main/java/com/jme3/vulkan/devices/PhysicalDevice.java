package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.Queue;
import com.jme3.vulkan.memory.MemoryFlag;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public interface PhysicalDevice {

    boolean populateQueueFamilyIndices();

    VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack);

    void createQueues(LogicalDevice device);

    VulkanInstance getInstance();

    VkPhysicalDevice getDeviceHandle();

    VkQueueFamilyProperties.Buffer getQueueFamilyProperties(MemoryStack stack);

    VkPhysicalDeviceProperties getProperties(MemoryStack stack);

    VkPhysicalDeviceFeatures getFeatures(MemoryStack stack);

    VkExtensionProperties.Buffer getExtensionProperties(MemoryStack stack);

    VkPhysicalDeviceMemoryProperties getMemoryProperties(MemoryStack stack);

    int findSupportedMemoryType(MemoryStack stack, int types, Flag<MemoryFlag> flags);

    Image.Format findSupportedFormat(Image.Tiling tiling, int features, Image.Format... candidates);

    boolean querySwapchainSupport(Surface surface);

    Queue getCompute();

    Queue getDataTransfer();

}
