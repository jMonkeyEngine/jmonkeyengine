package com.jme3.vulkan.devices;

import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

public interface DeviceFeature {

    void enableFeature(VkPhysicalDeviceFeatures features);

    Float evaluateFeatureSupport(VkPhysicalDeviceFeatures features);

    static DeviceFeature anisotropy(Float pass, boolean rejectOnFail) {
        return new DeviceFeature() {
            @Override
            public void enableFeature(VkPhysicalDeviceFeatures features) {
                features.samplerAnisotropy(true);
            }
            @Override
            public Float evaluateFeatureSupport(VkPhysicalDeviceFeatures features) {
                return features.samplerAnisotropy() ? pass : (rejectOnFail ? null : 0f);
            }
        };
    }

}
