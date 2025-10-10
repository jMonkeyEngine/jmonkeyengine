package com.jme3.vulkan.allocation;

import com.jme3.vulkan.pipelines.GraphicsPipeline;

import java.util.HashMap;
import java.util.Map;

public class GraphicsPipelineAllocator implements Allocator<GraphicsPipeline, GraphicsState> {

    private final Map<Object, GraphicsPipeline> pipelines = new HashMap<>();

    @Override
    public GraphicsPipeline allocate(GraphicsState key) {
        return null;
    }

    @Override
    public void release(GraphicsPipeline resource) {

    }

}
