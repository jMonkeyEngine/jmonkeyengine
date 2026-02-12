package com.jme3.vulkan.pipeline;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.util.IntEnum;

public interface Pipeline {

    void bind(CommandBuffer cmd);

    PipelineLayout getLayout();

    PipelineBindPoint getBindPoint();

    long getSortId();

    boolean isDynamic(IntEnum<DynamicState> state);

}