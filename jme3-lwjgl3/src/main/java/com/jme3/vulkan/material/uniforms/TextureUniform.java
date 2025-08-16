package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.images.Texture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class TextureUniform extends AbstractUniform<Texture> {

    private final Image.Layout layout;
    private Texture texture;
    private boolean updateFlag = true;

    public TextureUniform(String name, Image.Layout layout, int bindingIndex) {
        super(name, Descriptor.CombinedImageSampler, bindingIndex);
        this.layout = layout;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1, stack)
                .imageView(texture.getImage().getNativeObject())
                .sampler(texture.getNativeObject())
                .imageLayout(layout.getVkEnum());
        write.pImageInfo(info)
                .descriptorType(type.getVkEnum())
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorCount(1);
    }

    @Override
    public boolean update(LogicalDevice<?> device) {
        if (texture == null) {
            throw new NullPointerException("Uniform texture is null.");
        }
        return updateFlag;
    }

    @Override
    public void setValue(Texture value) {
        if (this.texture != value) {
            this.texture = value;
            updateFlag = true;
        }
    }

    @Override
    public Texture getValue() {
        return texture;
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type == binding.getType()
            && bindingIndex == binding.getBinding()
            && binding.getDescriptors() == 1;
    }

    public Image.Layout getLayout() {
        return layout;
    }

}
