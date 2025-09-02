package com.jme3.vulkan.data;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.frames.UpdateFrameManager;
import org.lwjgl.system.MemoryStack;

import java.util.function.IntFunction;

public class PerFrameStagingPipe implements ThroughputDataPipe<VulkanBuffer, VulkanBuffer> {

    private final UpdateFrameManager frames;
    private final VulkanBuffer[] outputs;
    private DataPipe<? extends VulkanBuffer> input;

    public PerFrameStagingPipe(UpdateFrameManager frames, IntFunction<VulkanBuffer> generator) {
        this.frames = frames;
        this.outputs = new VulkanBuffer[frames.getTotalFrames()];
        for (int i = 0; i < outputs.length; i++) {
            outputs[i] = generator.apply(i);
        }
    }

    @Override
    public VulkanBuffer execute(CommandBuffer cmd) {
        VulkanBuffer in = input.execute(cmd);
        VulkanBuffer out = outputs[frames.getCurrentFrame()];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            out.recordCopy(stack, cmd, in, 0, 0, Math.min(in.size().getBytes(), out.size().getBytes()));
        }
        return out;
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
