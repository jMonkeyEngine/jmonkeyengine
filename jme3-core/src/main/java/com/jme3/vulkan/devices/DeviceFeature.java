package com.jme3.vulkan.devices;

import com.jme3.vulkan.util.PNextChain;
import org.lwjgl.vulkan.VkPhysicalDeviceDynamicRenderingFeatures;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceRobustness2FeaturesEXT;

public interface DeviceFeature {

    void enableFeature(PNextChain features);

    Float evaluateSupport(PNextChain features);

    static DeviceFeature anisotropy(Float pass) {
        return new BooleanDeviceFeature(pass) {
            @Override
            public void enableFeature(PNextChain features) {
                features.get(VkPhysicalDeviceFeatures2.class, f -> f.features().samplerAnisotropy(true));
            }
            @Override
            protected boolean isFeatureSupported(PNextChain features) {
                return features.get(VkPhysicalDeviceFeatures2.class, false, f -> f.features().samplerAnisotropy());
            }
        };
    }

    static DeviceFeature nullDescriptor(Float pass) {
        return new BooleanDeviceFeature(pass) {
            @Override
            public void enableFeature(PNextChain features) {
                features.get(VkPhysicalDeviceRobustness2FeaturesEXT.class, f -> f.nullDescriptor(true));
            }
            @Override
            protected boolean isFeatureSupported(PNextChain features) {
                return features.get(VkPhysicalDeviceRobustness2FeaturesEXT.class, false,
                        VkPhysicalDeviceRobustness2FeaturesEXT::nullDescriptor);
            }
        };
    }

    static DeviceFeature dynamicRendering(Float pass) {
        return new BooleanDeviceFeature(pass) {
            @Override
            protected boolean isFeatureSupported(PNextChain features) {
                return features.get(VkPhysicalDeviceDynamicRenderingFeatures.class, false,
                        VkPhysicalDeviceDynamicRenderingFeatures::dynamicRendering);
            }
            @Override
            public void enableFeature(PNextChain features) {
                features.get(VkPhysicalDeviceDynamicRenderingFeatures.class, f -> f.dynamicRendering(true));
            }
        };
    }

    abstract class BooleanDeviceFeature implements DeviceFeature {

        private final Float score;

        public BooleanDeviceFeature(Float score) {
            this.score = score;
        }

        @Override
        public Float evaluateSupport(PNextChain features) {
            boolean rejectOnFail = score == null;
            return isFeatureSupported(features) ? (rejectOnFail ? 0f : score) : (rejectOnFail ? null : 0f);
        }

        protected abstract boolean isFeatureSupported(PNextChain features);

    }

}
