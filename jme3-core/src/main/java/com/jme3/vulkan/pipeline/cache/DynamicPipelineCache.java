package com.jme3.vulkan.pipeline.cache;

import com.jme3.material.RenderState;
import com.jme3.vulkan.pipeline.graphics.CompatGraphicsPipeline;

import java.util.HashMap;
import java.util.Map;

public class DynamicPipelineCache {

    private final Map<RenderState, CompatGraphicsPipeline> pipelines = new HashMap<>();

    public CompatGraphicsPipeline create(RenderState state) {
        CompatGraphicsPipeline pipeline = pipelines.get(state);
        if (pipeline == null) {
            pipeline = CompatGraphicsPipeline.build()
        }
    }

}
