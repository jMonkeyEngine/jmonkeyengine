package com.jme3.vulkan.material.uniforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.backend.Engine;
import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.AbstractSetWriter;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.material.uniforms.def.UniformDef;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;
import java.util.function.Function;

public class TextureUniform <T extends Texture> implements VulkanUniform<T> {

    private final IntEnum<VulkanImage.Layout> layout;
    private T value;

    public TextureUniform(IntEnum<VulkanImage.Layout> layout) {
        this.layout = layout;
    }

    public TextureUniform(JsonNode json) {
        layout = VulkanImage.Layout.valueOf(json.get("layout").asText());
    }

    @Override
    public DescriptorSetWriter createWriter(SetLayoutBinding binding) {
        if (value == null) {
            return null;
        }
        return new Writer(binding, value, layout);
    }

    @Override
    public SetLayoutBinding createBinding(int binding, Flag<ShaderStage> scope) {
        return new SetLayoutBinding(Descriptor.CombinedImageSampler, binding, 1, scope);
    }

    @Override
    public void set(T value) {
        this.value = value;
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public String getDefineValue() {
        return value == null ? null : Uniform.ENABLED_DEFINE;
    }

    @Override
    public TextureUniform clone() {
        try {
            return (TextureUniform)super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
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
