package com.jme3.vulkan.devices;

import com.jme3.vulkan.surface.Surface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtensionProperties;

import java.util.Arrays;
import java.util.Collection;

public interface DeviceFilter {

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

    static DeviceAnisotropySupport anisotropy() {
        return new DeviceAnisotropySupport();
    }

    class DeviceExtensionSupport implements DeviceFilter {

        private final Collection<String> extensions;

        public DeviceExtensionSupport(Collection<String> extensions) {
            this.extensions = extensions;
        }

        @Override
        public Float evaluateDevice(PhysicalDevice device) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkExtensionProperties.Buffer exts = device.getExtensionProperties(stack);
                if (extensions.stream().allMatch(e -> exts.stream().anyMatch(
                    p -> p.extensionNameString().equals(e)))) {
                    return 0f;
                }
                return null;
            }
        }

    }

    class DeviceSwapchainSupport implements DeviceFilter {

        private final Surface surface;

        public DeviceSwapchainSupport(Surface surface) {
            this.surface = surface;
        }

        @Override
        public Float evaluateDevice(PhysicalDevice device) {
            if (device.querySwapchainSupport(surface)) {
                return 0f;
            }
            return null;
        }

    }

    class DeviceAnisotropySupport implements DeviceFilter {

        @Override
        public Float evaluateDevice(PhysicalDevice device) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                if (device.getFeatures(stack).samplerAnisotropy()) {
                    return 0f;
                }
                return null;
            }
        }

    }

}
