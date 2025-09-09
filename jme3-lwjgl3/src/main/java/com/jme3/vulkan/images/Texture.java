package com.jme3.vulkan.images;

import com.jme3.vulkan.devices.LogicalDevice;

import java.util.Objects;

public class Texture extends Sampler {

    private final ImageView image;

    public Texture(LogicalDevice device, ImageView image, int min, int mag, int edgeMode, int mipmapMode) {
        super(device, min, mag, edgeMode, mipmapMode);
        this.image = Objects.requireNonNull(image);
    }

    public ImageView getImage() {
        return image;
    }

}
