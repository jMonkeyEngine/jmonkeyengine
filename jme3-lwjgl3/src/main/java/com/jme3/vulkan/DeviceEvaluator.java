package com.jme3.vulkan;

import org.lwjgl.vulkan.VkExtensionProperties;

import java.util.Arrays;
import java.util.Collection;

public interface DeviceEvaluator {

    Float evaluateDevice(PhysicalDevice device);

    static DeviceExtensionSupport extensions(String... exts) {
        return new DeviceExtensionSupport(Arrays.asList(exts));
    }

    static DeviceExtensionSupport extensions(Collection<String> exts) {
        return new DeviceExtensionSupport(exts);
    }

    static DeviceSwapchainSupport swapchain(Surface surface) {
        return new DeviceSwapchainSupport(surface);
    }

    class DeviceExtensionSupport implements DeviceEvaluator {

        private final Collection<String> extensions;

        public DeviceExtensionSupport(Collection<String> extensions) {
            this.extensions = extensions;
        }

        @Override
        public Float evaluateDevice(PhysicalDevice device) {
            VkExtensionProperties.Buffer exts = device.getExtensions();
            if (extensions.stream().allMatch(e -> exts.stream().anyMatch(
                    p -> p.extensionNameString().equals(e)))) return 0f;
            return null;
        }

    }

    class DeviceSwapchainSupport implements DeviceEvaluator {

        private final Surface surface;

        public DeviceSwapchainSupport(Surface surface) {
            this.surface = surface;
        }

        @Override
        public Float evaluateDevice(PhysicalDevice device) {
            return device.querySwapchainSupport(surface) ? 0f : null;
        }

    }

}
