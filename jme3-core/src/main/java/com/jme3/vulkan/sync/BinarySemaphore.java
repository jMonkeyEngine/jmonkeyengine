package com.jme3.vulkan.sync;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK12.*;

public class BinarySemaphore extends AbstractNative<Long> implements Semaphore {

    private final LogicalDevice<?> device;

    public BinarySemaphore(LogicalDevice<?> device) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreTypeCreateInfo type = VkSemaphoreTypeCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                    .semaphoreType(VK_SEMAPHORE_TYPE_BINARY);
            VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(type);
            object = getLong(stack, ptr -> check(vkCreateSemaphore(device.getNativeObject(), create, null, ptr),
                    "Failed to create semaphore."));
        }
        ref = DisposableManager.reference(this);
        device.getReference().addDependent(ref);
    }

    @Override
    public long getSemaphoreObject() {
        return object;
    }

    @Override
    public long getTargetPayload() {
        return 0;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroySemaphore(device.getNativeObject(), object, null);
    }

}
