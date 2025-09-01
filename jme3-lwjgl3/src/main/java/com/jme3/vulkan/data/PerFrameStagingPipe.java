package com.jme3.vulkan.data;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.frames.UpdateFrameManager;
import org.lwjgl.system.MemoryStack;

import java.util.function.IntFunction;

public class PerFrameStagingPipe implements ThroughputDataPipe<VulkanBuffer, VulkanBuffer> {

    private final UpdateFrameManager frames;
    private final CommandBuffer commands;
    private final VulkanBuffer[] outputs;
    private DataPipe<VulkanBuffer> input;

    public PerFrameStagingPipe(UpdateFrameManager frames, CommandBuffer commands, IntFunction<VulkanBuffer> generator) {
        this.frames = frames;
        this.commands = commands;
        this.outputs = new VulkanBuffer[frames.getTotalFrames()];
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = generator.apply(i);
        }
    }

    @Override
    public VulkanBuffer execute() {
        VulkanBuffer in = input.execute();
        VulkanBuffer out = outputs[frames.getCurrentFrame()];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            out.recordCopy(stack, commands, in, 0, 0, in.size().getBytes());
        }
        return out;
    }

    @Override
    public void setInput(DataPipe<VulkanBuffer> input) {
        this.input = input;
    }

}
