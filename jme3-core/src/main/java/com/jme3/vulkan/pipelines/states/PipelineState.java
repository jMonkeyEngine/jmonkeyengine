package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;

public interface PipelineState <T> {

    T toStruct(MemoryStack stack);

    @Deprecated
    default int difference(PipelineState<T> state) {
        return 0;
    }

}
