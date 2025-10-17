package com.jme3.vulkan.material.uniforms;

import com.fasterxml.jackson.databind.JsonNode;
import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.FlagParser;
import com.jme3.vulkan.util.IntEnum;
import com.jme3.vulkan.util.ReflectionArgs;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;

public class TextureUniform extends AbstractUniform<Texture> {

    private final IntEnum<VulkanImage.Layout> layout;

    public TextureUniform(String name, IntEnum<VulkanImage.Layout> layout, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, Descriptor.CombinedImageSampler, bindingIndex, stages);
        this.layout = layout;
    }

    public TextureUniform(ReflectionArgs args) {
        super(Descriptor.CombinedImageSampler, args);
        this.layout = FlagParser.parseEnum(VulkanImage.Layout.class, args.getProperties().get("layout").asText());
        if (args.getProperties().has("default")) {
            value = args.getAssetManager().loadTexture(args.getProperties().get("default").asText());
        }
    }

    @Override
    public DescriptorSetWriter createWriter() {
        if (value == null) {
            throw new NullPointerException("Cannot write null value.");
        }
        return new Writer(value);
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

    private class Writer implements DescriptorSetWriter {

        private final long samplerId, viewId;

        public Writer(Texture texture) {
            this.samplerId = texture.getId();
            this.viewId = texture.getView().getId();
        }

        @Override
        public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
            VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1, stack)
                    .imageView(viewId)
                    .sampler(samplerId)
                    .imageLayout(layout.getEnum());
            write.pImageInfo(info)
                    .descriptorType(type.getEnum())
                    .dstBinding(bindingIndex)
                    .dstArrayElement(0)
                    .descriptorCount(1);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Writer writer = (Writer) o;
            return samplerId == writer.samplerId && viewId == writer.viewId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(samplerId, viewId);
        }

    }

}
