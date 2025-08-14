package com.jme3.vulkan.devices;

import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.images.Image;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public interface PhysicalDevice {

    boolean populateQueueFamilyIndices();

    VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack);

    void createQueues(LogicalDevice device);

    VulkanInstance getInstance();

    VkPhysicalDevice getPhysicalDevice();

    VkQueueFamilyProperties.Buffer getQueueFamilyProperties(MemoryStack stack);

    VkPhysicalDeviceProperties getProperties(MemoryStack stack);

    VkPhysicalDeviceFeatures getFeatures(MemoryStack stack);

    VkExtensionProperties.Buffer getExtensionProperties(MemoryStack stack);

    VkPhysicalDeviceMemoryProperties getMemoryProperties(MemoryStack stack);

    int findSupportedMemoryType(MemoryStack stack, int types, int flags);

    Image.Format findSupportedFormat(int tiling, int features, Image.Format... candidates);

    boolean querySwapchainSupport(Surface surface);

}
