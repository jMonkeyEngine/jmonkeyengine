package com.jme3.system.vulkan;

import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

public interface DeviceEvaluator {

    Float evaluateDevice(VkPhysicalDevice device, VkPhysicalDeviceProperties props, VkPhysicalDeviceFeatures features);

}
