package com.jme3.vulkan.pipeline;

public interface VertexPipeline extends Pipeline {

    int getNumAttributes();

    Integer getAttributeLocation(String attributeName);

}
