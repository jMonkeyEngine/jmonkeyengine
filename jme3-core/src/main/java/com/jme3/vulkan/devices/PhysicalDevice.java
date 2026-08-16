package com.jme3.vulkan.devices;

import com.jme3.util.natives.NativeHandle;
import com.jme3.vulkan.formats.Format;
import com.jme3.vulkan.FormatFeature;
import com.jme3.vulkan.commands.CommandQueue;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

public interface PhysicalDevice extends NativeHandle<VkPhysicalDevice> {

    boolean populateQueueFamilyIndices();

    VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack);

    void createQueues(LogicalDevice device);

    VulkanInstance getInstance();

    VkQueueFamilyProperties.Buffer getQueueFamilyProperties();

    VkPhysicalDeviceProperties getProperties();

    VkPhysicalDeviceFeatures getFeatures(VkPhysicalDeviceFeatures features);

    VkPhysicalDeviceFeatures2 getFeatures(VkPhysicalDeviceFeatures2 features);

    VkExtensionProperties.Buffer getExtensionProperties();

    VkPhysicalDeviceMemoryProperties getMemoryProperties();

    int findSupportedMemoryType(int types, Flag<MemoryProp> flags);

    Format findSupportedFormat(EngineImage.Tiling tiling, Flag<FormatFeature> features, Format... candidates);

    boolean querySwapchainSupport(Surface surface);

    CommandQueue getCompute();

}
