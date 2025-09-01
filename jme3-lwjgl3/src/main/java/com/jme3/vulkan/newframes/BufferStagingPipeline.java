package com.jme3.vulkan.newframes;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import org.lwjgl.system.MemoryStack;

/**
 * Transfers data in a host-accessible buffer to another buffer.
 */
public class BufferStagingPipeline implements Resource<VulkanBuffer, VulkanBuffer> {

    private final VulkanBuffer result;
    private VulkanBuffer input;

    public BufferStagingPipeline(VulkanBuffer result) {
        this.result = result;
    }

    @Override
    public void setInput(VulkanBuffer input) {
        this.input = input;
    }

    @Override
    public void execute(CommandBuffer cmd) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            result.recordCopy(stack, cmd, input, 0, 0, input.size().getBytes());
        }
    }

    @Override
    public VulkanBuffer getResult() {
        return result;
    }

}
