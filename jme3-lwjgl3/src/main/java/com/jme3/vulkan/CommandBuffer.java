package com.jme3.vulkan;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class CommandBuffer {

    protected final CommandPool pool;
    protected final VkCommandBuffer buffer;
    protected boolean recording = false;

    public CommandBuffer(CommandPool pool) {
        this.pool = pool;
        VkCommandBufferAllocateInfo allocate = VkCommandBufferAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(pool.getNativeObject())
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1);
        PointerBuffer ptr = MemoryUtil.memAllocPointer(1);
        check(vkAllocateCommandBuffers(pool.getDevice().getNativeObject(), allocate, ptr),
                "Failed to allocate command buffer");
        buffer = new VkCommandBuffer(ptr.get(0), pool.getDevice().getNativeObject());
        allocate.close();
        MemoryUtil.memFree(ptr);
    }

    public void begin() {
        if (recording) {
            throw new IllegalStateException("Command buffer already recording.");
        }
        VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        check(vkBeginCommandBuffer(buffer, begin), "Failed to begin command buffer");
        begin.close();
        recording = true;
    }

    public void end() {
        check(vkEndCommandBuffer(buffer), "Failed to record command buffer");
    }

    public void submit(Semaphore wait, Semaphore signal, Fence fence) {
        if (!recording) {
            throw new IllegalStateException("Command buffer has not begun recording.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo.Buffer submit = VkSubmitInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(buffer));
            if (wait != null) {
                submit.waitSemaphoreCount(1).pWaitSemaphores(stack.longs(wait.getNativeObject()))
                        .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));
            }
            if (signal != null) {
                submit.pSignalSemaphores(stack.longs(signal.getNativeObject()));
            }
            pool.getQueue().submit(submit, fence);
        }
        recording = false;
    }

    public void reset() {
        vkResetCommandBuffer(buffer, 0);
    }

    public CommandPool getPool() {
        return pool;
    }

    public VkCommandBuffer getBuffer() {
        return buffer;
    }

    public boolean isRecording() {
        return recording;
    }

}
