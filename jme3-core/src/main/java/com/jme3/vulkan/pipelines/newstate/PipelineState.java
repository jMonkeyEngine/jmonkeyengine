package com.jme3.vulkan.pipelines.newstate;

import org.lwjgl.system.MemoryStack;

public interface PipelineState <T> {

    void apply(MemoryStack stack, T parent);

}
