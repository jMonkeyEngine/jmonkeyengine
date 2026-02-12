package com.jme3.vulkan.devices;

import com.jme3.vulkan.util.PNextChain;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures2;
import org.lwjgl.vulkan.VkPhysicalDeviceRobustness2FeaturesEXT;

public interface DeviceFeature {

    void enableFeature(PNextChain features);

    Float evaluateSupport(PNextChain features);

    static DeviceFeature anisotropy(float pass, boolean rejectOnFail) {
        return new BooleanDeviceFeature(pass, rejectOnFail) {
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

    static DeviceFeature nullDescriptor(float pass, boolean rejectOnFail) {
        return new BooleanDeviceFeature(pass, rejectOnFail) {
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

    abstract class BooleanDeviceFeature implements DeviceFeature {

        private final Float pass;
        private final boolean rejectOnFail;

        public BooleanDeviceFeature(float pass, boolean rejectOnFail) {
            this.pass = pass;
            this.rejectOnFail = rejectOnFail;
        }

        @Override
        public Float evaluateSupport(PNextChain features) {
            return isFeatureSupported(features) ? pass : (rejectOnFail ? null : 0f);
        }

        protected abstract boolean isFeatureSupported(PNextChain features);

    }

}
