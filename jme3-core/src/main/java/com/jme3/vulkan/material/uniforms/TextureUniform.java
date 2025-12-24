package com.jme3.vulkan.material.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;

public class TextureUniform implements VulkanUniform<Texture<?, ?>> {

    private final IntEnum<VulkanImage.Layout> layout;
    private Texture<?, ?> value;

    public TextureUniform(IntEnum<VulkanImage.Layout> layout) {
        this.layout = layout;
    }

    @Override
    public DescriptorSetWriter createWriter(SetLayoutBinding binding) {
        if (value == null) {
            return null;
        }
        return new Writer(binding, value, layout);
    }

    @Override
    public SetLayoutBinding createBinding(IntEnum<Descriptor> type, int binding, Flag<ShaderStage> stages) {
        return new SetLayoutBinding(type, binding, 1, stages);
    }

    @Override
    public void set(Texture<?, ?> value) {
        this.value = value;
    }

    @Override
    public Texture<?, ?> get() {
        return value;
    }

    @Override
    public String getDefineValue() {
        return value == null ? null : Uniform.ENABLED_DEFINE;
    }

    public IntEnum<VulkanImage.Layout> getLayout() {
        return layout;
    }

    private static class Writer extends AbstractSetWriter {

        private final long samplerId, viewId;
        private final IntEnum<VulkanImage.Layout> layout;

        public Writer(SetLayoutBinding binding, Texture<?, ?> texture, IntEnum<VulkanImage.Layout> layout) {
            super(binding, 0, 1);
            this.samplerId = texture.getId();
            this.viewId = texture.getView().getId();
            this.layout = layout;
        }

        @Override
        public void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            write.pImageInfo(VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(viewId)
                    .sampler(samplerId)
                    .imageLayout(layout.getEnum()));
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return samplerId == writer.samplerId
                    && viewId == writer.viewId
                    && layout.is(writer.layout)
                    && Objects.equals(binding, writer.binding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(binding, samplerId, viewId, layout);
        }

    }

}
