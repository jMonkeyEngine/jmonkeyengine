package com.jme3.vulkan.data;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import org.lwjgl.system.MemoryStack;

/**
 * Transfers data in a host-accessible buffer to another buffer.
 */
public class StagingPipe implements ThroughputDataPipe<VulkanBuffer, VulkanBuffer> {

    private final VulkanBuffer output;
    private final CommandBuffer commands;
    private DataPipe<VulkanBuffer> input;

    public StagingPipe(VulkanBuffer output, CommandBuffer commands) {
        this.output = output;
        this.commands = commands;
    }

    @Override
    public VulkanBuffer execute() {
        VulkanBuffer in = input.execute();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            output.recordCopy(stack, commands, in, 0, 0, in.size().getBytes());
        }
        return output;
    }

    @Override
    public void setInput(DataPipe<VulkanBuffer> input) {
        this.input = input;
    }

}
