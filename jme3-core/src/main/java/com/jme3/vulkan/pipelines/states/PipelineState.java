package com.jme3.vulkan.pipelines.states;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;

public interface PipelineState <T extends Struct> {

    T toStruct(MemoryStack stack);

}
