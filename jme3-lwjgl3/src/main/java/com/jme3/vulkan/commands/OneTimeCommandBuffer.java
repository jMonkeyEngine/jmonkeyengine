package com.jme3.vulkan.commands;

import com.jme3.vulkan.sync.Fence;
import com.jme3.vulkan.sync.Semaphore;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkSubmitInfo;

import static org.lwjgl.vulkan.VK10.*;

public class OneTimeCommandBuffer extends CommandBuffer {

    private boolean active = false;

    public OneTimeCommandBuffer(CommandPool pool) {
        super(pool);
    }

    @Override
    public void begin() {
        if (recording) {
            throw new IllegalStateException("Command buffer already recording.");
        }
        if (active) {
            throw new IllegalStateException("Buffer already freed.");
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
    public void submit(Semaphore wait, Semaphore signal, Fence fence) {
        if (recording) {
            throw new IllegalStateException("Command buffer is still recording.");
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
