package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.images.GpuImage;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.images.VulkanImageView;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;

public class TextureUniformBinding extends UniformBinding<Texture<VulkanImageView, VulkanImage>> {

    public TextureUniformBinding(int binding, Flag<ShaderStage> stages) {
        super(Descriptor.CombinedImageSampler, binding, 1, stages);
    }

    @Override
    public DescriptorSetWriter createWriter(Texture<VulkanImageView, VulkanImage> value) {
        return null;
    }

    private class Writer extends AbstractSetWriter {

        private final Texture<VulkanImageView, ? extends GpuImage> texture;
        private final IntEnum<VulkanImage.Layout> layout;

        public Writer(Texture<VulkanImageView, ? extends GpuImage> texture, IntEnum<VulkanImage.Layout> layout) {
            super(TextureUniformBinding.this, 0, 1);
            this.texture = texture;
            this.layout = layout;
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pImageInfo(VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(texture.getView().getId())
                    .sampler(texture.getId())
                    .imageLayout(layout.getEnum()));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer)o;
            return texture.getView().getId() == writer.texture.getView().getId()
                    && texture.getId() == writer.texture.getId()
                    && layout.is(writer.layout)
                    && Objects.equals(binding, writer.binding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binding, texture.getView().getId(), texture.getId(), layout);
        }

    }

}
