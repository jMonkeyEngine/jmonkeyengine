package com.jme3.vulkan.devices;

import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.surface.Surface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.enumerateBuffer;
import static org.lwjgl.vulkan.VK10.*;

public abstract class AbstractPhysicalDevice implements PhysicalDevice {

    private final VulkanInstance instance;
    private final VkPhysicalDevice physicalDevice;

    public AbstractPhysicalDevice(VulkanInstance instance, long id) {
        this.instance = instance;
        this.physicalDevice = new VkPhysicalDevice(id, instance.getNativeObject());
    }

    @Override
    public VulkanInstance getInstance() {
        return instance;
    }

    @Override
    public VkPhysicalDevice getPhysicalDevice() {
        return physicalDevice;
    }

    @Override
    public VkQueueFamilyProperties.Buffer getQueueFamilyProperties(MemoryStack stack) {
        return enumerateBuffer(stack, n -> VkQueueFamilyProperties.calloc(n, stack), (count, buffer)
                -> vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, count, buffer));
    }

    @Override
    public VkPhysicalDeviceProperties getProperties(MemoryStack stack) {
        VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.malloc(stack);
        vkGetPhysicalDeviceProperties(physicalDevice, props);
        return props;
    }

    @Override
    public VkPhysicalDeviceFeatures getFeatures(MemoryStack stack) {
        VkPhysicalDeviceFeatures features = VkPhysicalDeviceFeatures.malloc(stack);
        vkGetPhysicalDeviceFeatures(physicalDevice, features);
        return features;
    }

    @Override
    public VkExtensionProperties.Buffer getExtensionProperties(MemoryStack stack) {
        return enumerateBuffer(stack, n -> VkExtensionProperties.malloc(n, stack), (count, buffer) ->
                vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, count, buffer));
    }

    @Override
    public VkPhysicalDeviceMemoryProperties getMemoryProperties(MemoryStack stack) {
        VkPhysicalDeviceMemoryProperties mem = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, mem);
        return mem;
    }

    @Override
    public int findSupportedMemoryType(MemoryStack stack, int types, int flags) {
        VkPhysicalDeviceMemoryProperties mem = getMemoryProperties(stack);
        for (int i = 0; i < mem.memoryTypeCount(); i++) {
            if ((types & (1 << i)) != 0 && (mem.memoryTypes().get(i).propertyFlags() & flags) != 0) {
                return i;
            }
        }
        throw new NullPointerException("Suitable memory type not found.");
    }

    @Override
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

    @Override
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
