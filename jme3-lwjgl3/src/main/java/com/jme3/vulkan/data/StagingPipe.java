package com.jme3.vulkan.data;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import org.lwjgl.system.MemoryStack;

/**
 * Transfers data in a host-accessible buffer to another buffer.
 */
public class StagingPipe implements ThroughputDataPipe<VulkanBuffer, VulkanBuffer> {

    private final VulkanBuffer output;
    private DataPipe<? extends VulkanBuffer> input;

    public StagingPipe(VulkanBuffer output) {
        this.output = output;
    }

    @Override
    public VulkanBuffer execute(CommandBuffer cmd) {
        VulkanBuffer in = input.execute(cmd);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            output.recordCopy(stack, cmd, in, 0, 0, Math.min(in.size().getBytes(), output.size().getBytes()));
        }
        return output;
    }

    @Override
    public void setInput(DataPipe<? extends VulkanBuffer> input) {
        this.input = input;
    }

    @Override
    public DataPipe<? extends VulkanBuffer> getInput() {
        return input;
    }

}
