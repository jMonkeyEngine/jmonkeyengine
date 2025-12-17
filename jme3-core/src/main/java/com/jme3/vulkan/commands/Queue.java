package com.jme3.vulkan.commands;

import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class Queue {

    private final LogicalDevice<?> device;
    private final VkQueue queue;
    private final int familyIndex, queueIndex;
    private final Executor executor = Executors.newCachedThreadPool();

    public Queue(LogicalDevice<?> device, int familyIndex, int queueIndex) {
        this.device = device;
        this.familyIndex = familyIndex;
        this.queueIndex = queueIndex;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer ptr = stack.mallocPointer(1);
            vkGetDeviceQueue(device.getNativeObject(), familyIndex, queueIndex, ptr);
            queue = new VkQueue(ptr.get(0), device.getNativeObject());
        }
    }

    public void submit(VkSubmitInfo.Buffer info) {
        submit(info, null);
    }

    public void submit(VkSubmitInfo.Buffer info, Fence fence) {
        check(vkQueueSubmit(queue, info, fence != null ? fence.getNativeObject() : VK_NULL_HANDLE),
                "Failed to submit to queue.");
    }

    public void waitIdle() {
        vkQueueWaitIdle(queue);
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public VkQueue getQueue() {
        return queue;
    }

    public int getFamilyIndex() {
        return familyIndex;
    }

    public int getQueueIndex() {
        return queueIndex;
    }

    public Executor getExecutor() {
        return executor;
    }

}
