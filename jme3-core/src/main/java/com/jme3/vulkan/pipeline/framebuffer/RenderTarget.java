package com.jme3.vulkan.pipeline.framebuffer;

import com.jme3.texture.ImageView;

public interface RenderTarget <T extends ImageView> {

    RenderTarget<T> setView(T view);

    T getView();

}
