package com.jme3.vulkan.pipelines.newnewstates;

import org.lwjgl.system.MemoryStack;

public interface PipelineState <T> {

    T create(MemoryStack stack);

    T fill(MemoryStack stack, T struct);

}
