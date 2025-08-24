package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.images.Image;
import com.jme3.vulkan.images.Texture;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class TextureUniform extends AbstractUniform<Texture> {

    private final Image.Layout layout;
    private VersionedResource<Texture> texture;
    private long variant = 0L;

    public TextureUniform(String name, Image.Layout layout, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, Descriptor.CombinedImageSampler, bindingIndex, stages);
        this.layout = layout;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        Texture tex = texture.getVersion();
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1, stack)
                .imageView(tex.getImage().getNativeObject())
                .sampler(tex.getNativeObject())
                .imageLayout(layout.getVkEnum());
        write.pImageInfo(info)
                .descriptorType(type.getVkEnum())
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorCount(1);
    }

    @Override
    public void update(LogicalDevice<?> device) {}

    @Override
    public void setValue(VersionedResource<Texture> value) {
        if (this.texture != value) {
            this.texture = value;
            variant++;
        }
    }

    @Override
    public VersionedResource<Texture> getValue() {
        return texture;
    }

    @Override
    public long getVariant() {
        return variant;
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
