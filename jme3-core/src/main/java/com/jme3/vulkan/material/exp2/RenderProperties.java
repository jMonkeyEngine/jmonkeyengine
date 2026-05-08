package com.jme3.vulkan.material.exp2;

import com.jme3.renderer.ViewPortArea;
import com.jme3.vulkan.pipeline.framebuffer.FrameBuffer;

public interface RenderProperties extends AutoCloseable {

    @Override
    void close();

    void pushFrameBuffer(FrameBuffer fbo);

    void popFrameBuffer();

    void pushViewPort(ViewPortArea area);

    void popViewPort();

}
