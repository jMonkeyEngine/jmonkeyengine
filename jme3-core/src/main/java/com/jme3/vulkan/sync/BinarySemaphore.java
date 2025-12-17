package com.jme3.vulkan.sync;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK12.*;

public class BinarySemaphore extends Semaphore {

    public BinarySemaphore(LogicalDevice<?> device) {
        super(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreTypeCreateInfo typeStruct = VkSemaphoreTypeCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                    .semaphoreType(VK_SEMAPHORE_TYPE_BINARY);
            VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(typeStruct);
            object = getLong(stack, ptr -> check(vkCreateSemaphore(device.getNativeObject(), create, null, ptr),
                    "Failed to create semaphore."));
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

}
