package com.jme3.texture;

import com.jme3.vulkan.images.GpuImage;

public interface ImageView <T extends GpuImage> {

    long getId();

    T getImage();

    int getBaseMipmap();

    int getMipmapCount();

    int getBaseLayer();

    int getLayerCount();

}
