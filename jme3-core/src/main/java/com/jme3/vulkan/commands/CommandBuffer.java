package com.jme3.vulkan.commands;

import com.jme3.vulkan.sync.Semaphore;
import com.jme3.vulkan.sync.SyncGroup;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferAllocateInfo allocate = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(pool.getNativeObject())
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);
            PointerBuffer ptr = stack.mallocPointer(1);
            check(vkAllocateCommandBuffers(pool.getDevice().getNativeObject(), allocate, ptr),
                    "Failed to allocate command buffer");
            buffer = new VkCommandBuffer(ptr.get(0), pool.getDevice().getNativeObject());
        }
    }

    public void begin() {
        if (recording) {
            throw new IllegalStateException("Command buffer already recording.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            check(vkBeginCommandBuffer(buffer, begin), "Failed to begin command buffer");
            recording = true;
        }
    }

    public void resetAndBegin() {
        reset();
        begin();
    }

    public void end() {
        if (!recording) {
            throw new IllegalStateException("Command buffer has not begun recording.");
        }
        check(vkEndCommandBuffer(buffer), "Failed to record command buffer");
        recording = false;
    }

    public void submit(SyncGroup sync) {
        if (recording) {
            throw new IllegalStateException("Command buffer is still recording.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSubmitInfo.Buffer submit = VkSubmitInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(buffer));
            if (sync.containsWaits()) {
                submit.waitSemaphoreCount(sync.getWaits().length)
                        .pWaitSemaphores(sync.onRegisterWait(stack))
                        .pWaitDstStageMask(sync.toDstStageBuffer(stack));
            }
            if (sync.containsSignals()) {
                submit.pSignalSemaphores(sync.onRegisterSignal(stack));
            }
            pool.getQueue().submit(submit, sync.getFence());
        }
    }

    public void endAndSubmit(SyncGroup sync) {
        end();
        submit(sync);
    }

    public void reset() {
        if (!pool.getFlags().contains(CommandPool.Create.ResetCommandBuffer)) {
            throw new UnsupportedOperationException("Command buffer resetting is not supported by the allocating pool.");
        }
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
