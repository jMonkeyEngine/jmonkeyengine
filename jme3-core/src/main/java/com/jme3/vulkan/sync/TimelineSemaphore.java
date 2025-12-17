package com.jme3.vulkan.sync;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.PipelineStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;

import java.nio.LongBuffer;
import java.util.concurrent.TimeUnit;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK12.*;

public class TimelineSemaphore extends Semaphore {

    public TimelineSemaphore(LogicalDevice<?> device, long payload) {
        super(device);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreTypeCreateInfo typeStruct = VkSemaphoreTypeCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO)
                    .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE)
                    .initialValue(payload);
            VkSemaphoreCreateInfo create = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                    .pNext(typeStruct);
            object = getLong(stack, ptr -> check(vkCreateSemaphore(device.getNativeObject(), create, null, ptr),
                    "Failed to create semaphore."));
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    public void block(long waitValue, long waitMillis) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            block(stack, waitValue, waitMillis);
        }
    }

    public void block(MemoryStack stack, long waitValue, long waitMillis) {
        LongBuffer sem = stack.mallocLong(1);
        LongBuffer vals = stack.mallocLong(1);
        sem.put(object).flip();
        vals.put(waitValue).flip();
        VkSemaphoreWaitInfo wait = VkSemaphoreWaitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                .pSemaphores(sem)
                .pValues(vals);
        check(vkWaitSemaphores(device.getNativeObject(), wait, TimeUnit.MICROSECONDS.toNanos(waitMillis)),
                "Error on semaphore wait.");
    }

}
