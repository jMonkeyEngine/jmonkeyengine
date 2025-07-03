package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;

import java.util.Collection;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class LogicalDevice implements Native<VkDevice> {

    private final PhysicalDevice physical;
    private final NativeReference ref;
    private VkDevice device;

    public LogicalDevice(PhysicalDevice physical, PointerBuffer extensions, PointerBuffer layers) {
        this.physical = physical;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDeviceCreateInfo create = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(physical.getQueueFamilies().createLogicalBuffers(stack))
                    .pEnabledFeatures(physical.getFeatures());
            if (extensions != null) {
                create.ppEnabledExtensionNames(extensions);
            }
            if (layers != null) {
                create.ppEnabledLayerNames(layers);
            }
            PointerBuffer ptr = stack.mallocPointer(1);
            device = new VkDevice(check(vkCreateDevice(physical.getDevice(), create, null, ptr),
                    "Failed to create logical device."), physical.getDevice(), create);
            MemoryUtil.memFree(ptr);
        }
        ref = Native.get().register(this);
    }

    @Override
    public VkDevice getNativeObject() {
        return device;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyDevice(device, null);
    }

    @Override
    public void prematureNativeDestruction() {
        device = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public PhysicalDevice getPhysicalDevice() {
        return physical;
    }

}
