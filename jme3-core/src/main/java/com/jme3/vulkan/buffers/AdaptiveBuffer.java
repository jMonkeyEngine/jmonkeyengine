package com.jme3.vulkan.buffers;

import com.jme3.scene.GlVertexBuffer;

public interface AdaptiveBuffer extends GpuBuffer {

    void setAccessMode(GlVertexBuffer.Usage mode);

    GlVertexBuffer.Usage getAccessMode();

}
