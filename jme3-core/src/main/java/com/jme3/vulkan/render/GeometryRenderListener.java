package com.jme3.vulkan.render;

public interface GeometryRenderListener <T extends BatchElement> {

    void preBatchRender();

    void elementRendered(T element);

    void postBatchRender();

}
