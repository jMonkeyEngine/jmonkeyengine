package com.jme3.vulkan.images;

import com.jme3.vulkan.LogicalDevice;

public class Texture extends Sampler {

    private final ImageView image;

    public Texture(LogicalDevice device, ImageView image, int min, int mag, int edgeMode, int mipmapMode) {
        super(device, min, mag, edgeMode, mipmapMode);
        this.image = image;
    }

    public ImageView getImage() {
        return image;
    }

}
