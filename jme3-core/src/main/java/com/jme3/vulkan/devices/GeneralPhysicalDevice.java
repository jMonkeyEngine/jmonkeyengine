package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.CommandQueue;
import com.jme3.vulkan.VulkanInstance;
import com.jme3.vulkan.surface.Surface;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class GeneralPhysicalDevice extends AbstractPhysicalDevice implements GraphicalDevice, PresentDevice {

    private final Surface surface;
    private final QueueInfo graphics = new QueueInfo(1f);
    private final QueueInfo present = new QueueInfo(2f);

    public GeneralPhysicalDevice(VulkanInstance instance, Surface surface, long id) {
        super(instance, id);
        this.surface = surface;
    }

    @Override
    public boolean populateQueueFamilyIndices() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueueFamilyProperties.Buffer properties = getQueueFamilyProperties();
            IntBuffer intBuf = stack.callocInt(1);
            for (int i = 0; i < properties.limit(); i++) {
                VkQueueFamilyProperties props = properties.get(i);
                if (!graphics.hasFamily() && (props.queueFlags() & VK_QUEUE_GRAPHICS_BIT) > 0) {
                    graphics.setFamilyIndex(i);
                }
                if (!present.hasFamily()) {
                    KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(getDeviceHandle(), i, surface.getNativeObject(), intBuf);
                    if (intBuf.get(0) == VK_TRUE) {
                        present.setFamilyIndex(i);
                    }
                }
                if (graphics.hasFamily() && present.hasFamily()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack) {
        return createQueueFamilyInfo(stack, graphics, present);
    }

    @Override
    public void createQueues(LogicalDevice device) {
        graphics.generate(device);
        present.generate(device);
    }

    @Override
    public CommandQueue getGraphics() {
        return graphics.getQueue();
    }

    @Override
    public CommandQueue getPresent() {
        return present.getQueue();
    }

    @Override
    public CommandQueue getCompute() {
        return graphics.getQueue();
    }

}
