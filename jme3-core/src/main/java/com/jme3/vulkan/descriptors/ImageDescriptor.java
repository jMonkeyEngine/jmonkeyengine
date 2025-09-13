package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.images.*;
import org.lwjgl.vulkan.VkDescriptorImageInfo;

public class ImageDescriptor {

    private final ImageView view;
    private final Sampler sampler;
    private final VulkanImage.Layout layout;

    public ImageDescriptor(Texture texture, VulkanImage.Layout layout) {
        this(texture.getImage(), texture, layout);
    }

    public ImageDescriptor(ImageView view, Sampler sampler, VulkanImage.Layout layout) {
        this.view = view;
        this.sampler = sampler;
        this.layout = layout;
    }

    public void fillDescriptorInfo(VkDescriptorImageInfo info) {
        info.imageView(view.getNativeObject())
                .sampler(sampler.getNativeObject())
                .imageLayout(layout.getEnum());
    }

    public ImageView getView() {
        return view;
    }

    public Sampler getSampler() {
        return sampler;
    }

    public VulkanImage.Layout getLayout() {
        return layout;
    }

}
