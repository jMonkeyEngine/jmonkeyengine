package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.images.ImageView;
import com.jme3.vulkan.images.Sampler;
import com.jme3.vulkan.images.Texture;
import org.lwjgl.vulkan.VkDescriptorImageInfo;

public class ImageDescriptor {

    private final ImageView view;
    private final Sampler sampler;
    private final Image.Layout layout;

    public ImageDescriptor(Texture texture, Image.Layout layout) {
        this(texture.getImage(), texture, layout);
    }

    public ImageDescriptor(ImageView view, Sampler sampler, Image.Layout layout) {
        this.view = view;
        this.sampler = sampler;
        this.layout = layout;
    }

    public void fillDescriptorInfo(VkDescriptorImageInfo info) {
        info.imageView(view.getNativeObject())
                .sampler(sampler.getNativeObject())
                .imageLayout(layout.getVkEnum());
    }

    public ImageView getView() {
        return view;
    }

    public Sampler getSampler() {
        return sampler;
    }

    public Image.Layout getLayout() {
        return layout;
    }

}
