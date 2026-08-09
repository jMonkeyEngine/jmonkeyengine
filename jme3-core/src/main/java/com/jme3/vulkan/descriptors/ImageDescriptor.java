package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.images.*;
import com.jme3.vulkan.images.newimage.EngineImage;
import org.lwjgl.vulkan.VkDescriptorImageInfo;

public class ImageDescriptor {

    private final VulkanImageView view;
    private final Sampler sampler;
    private final EngineImage.Layout layout;

    public ImageDescriptor(VulkanTexture texture, EngineImage.Layout layout) {
        this(texture.getView(), texture, layout);
    }

    public ImageDescriptor(VulkanImageView view, Sampler sampler, EngineImage.Layout layout) {
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

    public EngineImage.Layout getLayout() {
        return layout;
    }

}
