package com.jme3.vulkan.devices;

import org.lwjgl.vulkan.VkPhysicalDeviceProperties;

public interface DeviceProperty {

    Float evaluateDeviceProperties(VkPhysicalDeviceProperties properties);

    static DeviceProperty maxAnisotropy(float threshold) {
        return p -> p.limits().maxSamplerAnisotropy() >= threshold ? 0f : null;
    }

}
