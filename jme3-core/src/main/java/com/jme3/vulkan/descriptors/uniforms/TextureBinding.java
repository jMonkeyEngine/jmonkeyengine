package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.UniformBinding;
import com.jme3.vulkan.devices.LogicalDevice;
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

public class TextureBinding extends UniformBinding<Texture<VulkanImageView, VulkanImage>> {

    public TextureBinding(Flag<ShaderStage> stages) {
        super(Descriptor.CombinedImageSampler, 1, stages);
    }

    @Override
    public DescriptorSetWriter createWriter(Texture<VulkanImageView, VulkanImage> value) {
        return new Writer(value, VulkanImage.Layout.ShaderReadOnlyOptimal);
    }

    private class Writer implements DescriptorSetWriter {

        private final Texture<VulkanImageView, ? extends GpuImage> texture;
        private final IntEnum<VulkanImage.Layout> layout;

        public Writer(Texture<VulkanImageView, ? extends GpuImage> texture, IntEnum<VulkanImage.Layout> layout) {
            this.texture = texture;
            this.layout = layout;
        }

        @Override
        public void populateWrite(MemoryStack stack, LogicalDevice<?> device, VkWriteDescriptorSet write) {
            write.pImageInfo(VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(texture.getView().getId())
                    .sampler(texture.getId())
                    .imageLayout(layout.getEnum()));
            write.descriptorType(getType().getEnum())
                    .descriptorCount(1)
                    .dstArrayElement(0);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer)o;
            return texture == writer.texture && layout.is(writer.layout);
        }

        @Override
        public int hashCode() {
            return Objects.hash(System.identityHashCode(texture), layout.getEnum());
        }

    }

}
