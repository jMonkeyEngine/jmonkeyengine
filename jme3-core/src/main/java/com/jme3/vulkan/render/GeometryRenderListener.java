package com.jme3.vulkan.render;

import com.jme3.vulkan.render.batching.BatchElement;

public interface GeometryRenderListener <T extends BatchElement> {

    void preBatchRender();

    void elementRendered(T element);

    void postBatchRender();

}
