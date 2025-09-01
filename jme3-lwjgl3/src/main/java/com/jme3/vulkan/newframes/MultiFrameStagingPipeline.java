package com.jme3.vulkan.newframes;

import com.jme3.vulkan.buffers.VulkanBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.frames.UpdateFrameManager;

public class MultiFrameStagingPipeline implements Resource<VulkanBuffer, VulkanBuffer> {

    private final UpdateFrameManager frames;

    public MultiFrameStagingPipeline(UpdateFrameManager frames) {
        this.frames = frames;
    }

    @Override
    public void setInput(VulkanBuffer input) {

    }

    @Override
    public void execute(CommandBuffer cmd) {

    }

    @Override
    public VulkanBuffer getResult() {
        return null;
    }

}
