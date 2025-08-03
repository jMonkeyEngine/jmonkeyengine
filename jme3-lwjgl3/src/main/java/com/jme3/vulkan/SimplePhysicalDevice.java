package com.jme3.vulkan;

import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.devices.PhysicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

public class SimplePhysicalDevice extends PhysicalDevice {

    private final Surface surface;
    private Integer graphicsIndex, presentIndex;
    private Queue graphics, present;

    public SimplePhysicalDevice(VulkanInstance instance, Surface surface, long id) {
        super(instance, id);
        this.surface = surface;
    }

    @Override
    protected boolean populateQueueFamilyIndices() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkQueueFamilyProperties.Buffer properties = getQueueFamilyProperties(stack);
            IntBuffer ibuf = stack.callocInt(1);
            for (int i = 0; i < properties.limit(); i++) {
                VkQueueFamilyProperties props = properties.get(i);
                if (graphicsIndex == null && (props.queueFlags() & VK13.VK_QUEUE_GRAPHICS_BIT) > 0) {
                    graphicsIndex = i;
                } else if (presentIndex == null) {
                    KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(
                            getPhysicalDevice(), i, surface.getNativeObject(), ibuf);
                    if (ibuf.get(0) == VK13.VK_TRUE) {
                        presentIndex = i;
                    }
                }
                if (graphicsIndex != null && presentIndex != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected VkDeviceQueueCreateInfo.Buffer createQueueFamilyInfo(MemoryStack stack) {
        VkDeviceQueueCreateInfo.Buffer create = VkDeviceQueueCreateInfo.calloc(2, stack); // one element for each queue
        create.get(0).sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(graphicsIndex)
                .pQueuePriorities(stack.floats(1f));
        create.get(1).sType(VK13.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                .queueFamilyIndex(presentIndex)
                .pQueuePriorities(stack.floats(1f));
        return create;
    }

    @Override
    protected void createQueues(LogicalDevice device) {
        graphics = new Queue(device, graphicsIndex, 0);
        present = new Queue(device, presentIndex, 0);
    }

    public Integer getGraphicsIndex() {
        return graphicsIndex;
    }

    public Integer getPresentIndex() {
        return presentIndex;
    }

    public Queue getGraphics() {
        return graphics;
    }

    public Queue getPresent() {
        return present;
    }

}
