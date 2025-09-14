package com.jme3.vulkan.images;

import com.jme3.texture.Texture;
import com.jme3.vulkan.devices.LogicalDevice;

import java.util.Objects;

public class VulkanTexture extends Sampler implements Texture<VulkanImageView, VulkanImage> {

    private final VulkanImageView image;

    public VulkanTexture(LogicalDevice device, VulkanImageView image) {
        super(device);
        this.image = Objects.requireNonNull(image);
    }

    @Override
    public long getId() {
        return object;
    }

    @Override
    public VulkanImageView getView() {
        return image;
    }

}
