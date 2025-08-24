package com.jme3.vulkan.devices;

import com.jme3.vulkan.commands.Queue;
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
    private Integer graphicsIndex, presentIndex;
    private Queue graphics, present;

    public GeneralPhysicalDevice(VulkanInstance instance, Surface surface, long id) {
        super(instance, id);
        this.surface = surface;
    }

    @Override
    public boolean populateQueueFamilyIndices() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueueFamilyProperties.Buffer properties = getQueueFamilyProperties(stack);
            IntBuffer ibuf = stack.callocInt(1);
            for (int i = 0; i < properties.limit(); i++) {
                VkQueueFamilyProperties props = properties.get(i);
                int flags = props.queueFlags();
                if (graphicsIndex == null && (flags & VK_QUEUE_GRAPHICS_BIT) > 0) {
                    graphicsIndex = i;
                } else if (presentIndex == null) {
                    KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(getDeviceHandle(),
                            i, surface.getNativeObject(), ibuf);
                    if (ibuf.get(0) == VK_TRUE) {
                        presentIndex = i;
                    }
                }
                if (allQueuesAvailable()) {
                    return true;
                }
            }
        }
        return allQueuesAvailable();
    }

    @Override
    public VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack) {
        VkDeviceQueueCreateInfo.Buffer create = VkDeviceQueueCreateInfo.calloc(2, stack);
        create.get(0).sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(graphicsIndex)
                .pQueuePriorities(stack.floats(1f));
        create.get(1).sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(presentIndex)
                .pQueuePriorities(stack.floats(1f));
        return create;
    }

    @Override
    public void createQueues(LogicalDevice device) {
        graphics = new Queue(device, graphicsIndex, 0);
        present = new Queue(device, presentIndex, 0);
    }

    @Override
    public Queue getGraphics() {
        return graphics;
    }

    @Override
    public Queue getPresent() {
        return present;
    }

    @Override
    public Queue getCompute() {
        return graphics;
    }

    @Override
    public Queue getDataTransfer() {
        return graphics;
    }

    public boolean allQueuesAvailable() {
        return graphicsIndex != null && presentIndex != null;
    }

}
