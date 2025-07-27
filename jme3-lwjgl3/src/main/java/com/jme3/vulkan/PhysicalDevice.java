package com.jme3.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.Supplier;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PhysicalDevice <T extends QueueFamilies> {

    private final VkPhysicalDevice device;
    private final T queues;
    private final VkQueueFamilyProperties.Buffer queueProperties;

    public PhysicalDevice(VkInstance instance, T queues, long id) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            this.queues = queues;
            this.device = new VkPhysicalDevice(id, instance);
            this.queueProperties = enumerateBuffer(stack, VkQueueFamilyProperties::malloc,
                    (count, buffer) -> vkGetPhysicalDeviceQueueFamilyProperties(device, count, buffer));
            this.queues.populate(this, getQueueFamilyProperties());
        }
    }

    public Float evaluate(Collection<? extends DeviceEvaluator> evaluators) {
        if (evaluators.isEmpty()) {
            return 0f;
        }
        float score = 0f;
        for (DeviceEvaluator e : evaluators) {
            Float s = e.evaluateDevice(this);
            if (s == null) return null;
            score += s;
        }
        return score;
    }

    public VkPhysicalDevice getDevice() {
        return device;
    }

    public T getQueueFamilies() {
        return queues;
    }

    public VkQueueFamilyProperties.Buffer getQueueFamilyProperties() {
        return queueProperties;
    }

    public VkPhysicalDeviceProperties getProperties() {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.create();
        vkGetPhysicalDeviceProperties(device, props);
        return props;
    }

    public VkPhysicalDeviceFeatures getFeatures() {
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.create();
        vkGetPhysicalDeviceFeatures(device, features);
        return features;
    }

    public VkExtensionProperties.Buffer getExtensions(MemoryStack stack) {
        return enumerateBuffer(stack, n -> VkExtensionProperties.malloc(n, stack), (count, buffer) ->
                vkEnumerateDeviceExtensionProperties(device, (ByteBuffer)null, count, buffer));
    }

    public int findSupportedFormat(int tiling, int features, int... candidates) {
        VkFormatProperties props = VkFormatProperties.create();
        for (int f : candidates) {
            vkGetPhysicalDeviceFormatProperties(device, f, props);
            if ((tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features)
                    || (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features)) {
                return f;
            }
        }
        throw new NullPointerException("Failed to find supported format.");
    }

    public int findMemoryType(int filter, int properties) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memProps = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device, memProps);
            for (int i = 0; i < memProps.memoryTypeCount(); i++) {
                if ((filter & (1 << i)) != 0 && (memProps.memoryTypes(i).propertyFlags() & properties) == properties) {
                    return i;
                }
            }
            throw new NullPointerException("Failed to find suitable memory type.");
        }
    }

    public boolean querySwapchainSupport(Surface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface.getNativeObject(), count, null);
            if (count.get(0) <= 0) {
                return false;
            }
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface.getNativeObject(), count, null);
            int n = count.get(0);
            return n > 0;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends QueueFamilies> PhysicalDevice<T> getPhysicalDevice(VkInstance instance,
                                                                                Collection<? extends DeviceEvaluator> evaluators,
                                                                                Supplier<T> queueFactory) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            System.out.println("Get physical device from instance: " + instance);
            PointerBuffer devices = enumerateBuffer(stack, stack::mallocPointer,
                    (count, buffer) -> check(vkEnumeratePhysicalDevices(instance, count, buffer),
                            "Failed to enumerate physical devices."));
            PhysicalDevice<T> device = null;
            float score = -1f;
            for (PhysicalDevice<T> d : iteratePointers(devices, ptr -> new PhysicalDevice(instance, queueFactory.get(), ptr))) {
                if (!d.queues.isComplete()) {
                    continue;
                }
                Float s = d.evaluate(evaluators);
                if (s != null && (device == null || s > score)) {
                    device = d;
                    score = s;
                }
            }
            if (device == null) {
                throw new NullPointerException("Failed to find suitable physical device.");
            }
            return device;
        }
    }

}
