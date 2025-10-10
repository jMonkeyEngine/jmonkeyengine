package com.jme3.vulkan.pipelines;

import com.jme3.vulkan.pipelines.newstate.PipelineState;

public interface PipelineCache {

    Pipeline acquire(PipelineState state);

}
