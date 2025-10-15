package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.images.*;
import org.lwjgl.vulkan.VkDescriptorImageInfo;

public class ImageDescriptor {

    private final VulkanImageView view;
    private final Sampler sampler;
    private final VulkanImage.Layout layout;

    public ImageDescriptor(VulkanTexture texture, VulkanImage.Layout layout) {
        this(texture.getView(), texture, layout);
    }

    public ImageDescriptor(VulkanImageView view, Sampler sampler, VulkanImage.Layout layout) {
        this.view = view;
        this.sampler = sampler;
        this.layout = layout;
    }

    public void fillDescriptorInfo(VkDescriptorImageInfo info) {
        info.imageView(view.getNativeObject())
                .sampler(sampler.getNativeObject())
                .imageLayout(layout.getEnum());
    }

    public VulkanImageView getView() {
        return view;
    }

    public Sampler getSampler() {
        return sampler;
    }

    public VulkanImage.Layout getLayout() {
        return layout;
    }

}
