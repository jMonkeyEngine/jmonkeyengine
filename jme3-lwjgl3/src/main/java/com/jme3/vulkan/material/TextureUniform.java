package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.images.Texture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class TextureUniform extends Uniform<Texture> {

    private final Image.Layout layout;

    public TextureUniform(Image.Layout layout, int bindingIndex) {
        super(Descriptor.CombinedImageSampler, bindingIndex);
        this.layout = layout;
    }

    public TextureUniform(Image.Layout layout, String name, int bindingIndex) {
        super(name, Descriptor.CombinedImageSampler, bindingIndex);
        this.layout = layout;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        super.populateWrite(stack, write);
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1, stack)
                .imageView(value.getImage().getNativeObject())
                .sampler(value.getNativeObject())
                .imageLayout(layout.getVkEnum());
        write.pImageInfo(info);
    }

    public Image.Layout getLayout() {
        return layout;
    }

}
