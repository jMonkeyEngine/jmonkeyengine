package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.texture.ImageView;

import java.util.List;

public interface FrameBuffer <T extends RenderTarget> {

    T createColorTarget(ImageView view);

    T createDepthTarget(ImageView view);

    void addColorTarget(T target);

    void addColorTarget(int i, T target);

    void setColorTarget(int i, T target);

    boolean removeColorTarget(T target);

    void clearColorTargets();

    void setDepthTarget(T target);

    List<T> getColorTargets();

    T getDepthTarget();

    boolean isUsingStencil();

    float getWidth();

    float getHeight();

}
