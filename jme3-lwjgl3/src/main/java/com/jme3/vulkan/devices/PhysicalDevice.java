package com.jme3.vulkan.devices;

import com.jme3.vulkan.surface.Surface;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.images.Image;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public abstract class PhysicalDevice {

    private final VulkanInstance instance;
    private final VkPhysicalDevice physicalDevice;

    public PhysicalDevice(VulkanInstance instance, long id) {
        this.instance = instance;
        this.physicalDevice = new VkPhysicalDevice(id, instance.getNativeObject());
    }

    protected abstract boolean populateQueueFamilyIndices();

    protected abstract VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack);

    protected abstract void createQueues(LogicalDevice device);

    public VulkanInstance getInstance() {
        return instance;
    }

    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    public VkQueueFamilyProperties.Buffer getQueueFamilyProperties(MemoryStack stack) {
        return enumerateBuffer(stack, n -> VkQueueFamilyProperties.calloc(n, stack), (count, buffer)
                -> vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, buffer));
    }

    public VkPhysicalDeviceProperties getProperties(MemoryStack stack) {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(physicalDevice, props);
        return props;
    }

    public VkPhysicalDeviceFeatures getFeatures(MemoryStack stack) {
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceFeatures(physicalDevice, features);
        return features;
    }

    public VkExtensionProperties.Buffer getExtensionProperties(MemoryStack stack) {
        return enumerateBuffer(stack, n -> VkExtensionProperties.malloc(n, stack), (count, buffer) ->
                vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, count, buffer));
    }

    public VkPhysicalDeviceMemoryProperties getMemoryProperties(MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties mem = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, mem);
        return mem;
    }

    public int findSupportedMemoryType(MemoryStack stack, int types, int flags) {
        VkPhysicalDeviceMemoryProperties mem = getMemoryProperties(stack);
        for (int i = 0; i < mem.memoryTypeCount(); i++) {
            if ((types & (1 << i)) != 0 && (mem.memoryTypes().get(i).propertyFlags() & flags) != 0) {
                return i;
            }
        }
        throw new NullPointerException("Suitable memory type not found.");
    }

    public Image.Format findSupportedFormat(int tiling, int features, Image.Format... candidates) {
        VkFormatProperties props = VkFormatProperties.create();
        for (Image.Format f : candidates) {
            vkGetPhysicalDeviceFormatProperties(physicalDevice, f.getVkEnum(), props);
            if ((tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features)
                    || (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features)) {
                return f;
            }
        }
        throw new NullPointerException("Failed to find supported format.");
    }

    public boolean querySwapchainSupport(Surface surface) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface.getNativeObject(), count, null);
            if (count.get(0) <= 0) {
                return false;
            }
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface.getNativeObject(), count, null);
            return count.get(0) > 0;
        }
    }

}
