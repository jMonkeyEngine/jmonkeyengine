package com.jme3.vulkan;

import org.lwjgl.system.MemoryStack;
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
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkExtensionProperties.Buffer exts = device.getExtensions(stack);
                System.out.println("available:");
                for (VkExtensionProperties p : exts) {
                    System.out.println("  " + p.extensionNameString());
                }
                if (extensions.stream().allMatch(e -> {
                    System.out.println("trying " + e + " extension...");
                    return exts.stream().anyMatch(
                        p -> p.extensionNameString().equals(e)); })) return 0f;
                System.out.println("Reject device by extensions");
                return null;
            }
        }

    }

    class DeviceSwapchainSupport implements DeviceEvaluator {

        private final Surface surface;

        public DeviceSwapchainSupport(Surface surface) {
            this.surface = surface;
        }

        @Override
        public Float evaluateDevice(PhysicalDevice device) {
            if (device.querySwapchainSupport(surface)) {
                return 0f;
            }
            System.out.println("Reject device by swapchain support");
            return null;
        }

    }

}
