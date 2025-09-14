package com.jme3.vulkan.material.uniforms;

import com.jme3.material.GlMaterial;
import com.jme3.renderer.opengl.GLRenderer;
import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class TextureUniform extends AbstractUniform<Texture> {

    private final IntEnum<VulkanImage.Layout> layout;

    public TextureUniform(String name, IntEnum<VulkanImage.Layout> layout, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, Descriptor.CombinedImageSampler, bindingIndex, stages);
        this.layout = layout;
    }

    @Override
    public void uploadToProgram(GLRenderer renderer, GlMaterial.BindUnits units) {
        renderer.setTexture(units.textureUnit++, resource.get());
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        Texture tex = resource.get();
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1, stack)
                .imageView(tex.getView().getId())
                .sampler(tex.getId())
                .imageLayout(layout.getEnum());
        write.pImageInfo(info)
                .descriptorType(type.getEnum())
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorCount(1);
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type.is(binding.getType())
            && bindingIndex == binding.getBinding()
            && binding.getDescriptors() == 1;
    }

    public IntEnum<VulkanImage.Layout> getLayout() {
        return layout;
    }

}
