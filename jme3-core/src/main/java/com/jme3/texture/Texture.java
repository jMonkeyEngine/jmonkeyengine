package com.jme3.texture;

import com.jme3.vulkan.images.GpuImage;

public interface Texture <V extends ImageView<? extends I>, I extends GpuImage> {

    long getId();

    V getView();

    default I getImage() {
        return getView().getImage();
    }

}
