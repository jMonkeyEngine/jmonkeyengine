package com.jme3.vulkan.commands;

import com.jme3.vulkan.sync.SyncGroup;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.vulkan.VK10.*;

public class TransientCommandBuffer extends CommandBuffer {

    private boolean active = false;

    public TransientCommandBuffer(CommandPool pool) {
        super(pool);
    }

    @Override
    public void begin() {
        if (recording) {
            throw new IllegalStateException("Command buffer already recording.");
        }
        if (active) {
            throw new IllegalStateException("One-time command buffer has already been used.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkCommandBufferBeginInfo begin = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            vkBeginCommandBuffer(buffer, begin);
        }
        recording = true;
    }

    @Override
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
                        .pWaitSemaphores(sync.toWaitBuffer(stack))
                        .pWaitDstStageMask(sync.toDstStageBuffer(stack));
            }
            if (sync.containsSignals()) {
                submit.pSignalSemaphores(sync.toSignalBuffer(stack));
            }
            pool.getQueue().submit(submit, sync.getFence());
            pool.getQueue().waitIdle();
            vkFreeCommandBuffers(pool.getDevice().getNativeObject(), pool.getNativeObject(), buffer);
        }
        active = false;
        recording = false;
    }

    public boolean isActive() {
        return active;
    }

}
