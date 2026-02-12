package com.jme3.vulkan.devices;

import com.jme3.vulkan.util.PNextChain;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;

public interface LegacyDeviceFeature extends DeviceFeature {

    void enableFeature(VkPhysicalDeviceFeatures features);

    Float evaluateSupport(VkPhysicalDeviceFeatures features);

    @Override
    default void enableFeature(PNextChain features) {
        enableFeature(features.get(VkPhysicalDeviceFeatures2.class).features());
    }

    @Override
    default Float evaluateSupport(PNextChain features) {
        return evaluateSupport(features.get(VkPhysicalDeviceFeatures2.class).features());
    }

}
