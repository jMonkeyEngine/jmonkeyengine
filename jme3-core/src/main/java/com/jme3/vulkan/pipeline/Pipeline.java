package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.util.natives.AbstractNative;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pipeline.states.BasePipelineState;
import com.jme3.vulkan.pipeline.states.PipelineState;
import com.jme3.vulkan.util.Flag;

import java.util.concurrent.atomic.AtomicLong;

import static org.lwjgl.vulkan.VK10.*;

public interface Pipeline {

    void bind(CommandBuffer cmd);

    PipelineLayout getLayout();

    PipelineBindPoint getBindPoint();

    /**
     * A unique ID by which pipeline's may be sorted.
     *
     * @return
     */
    int getSortId();

}