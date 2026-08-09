package com.jme3.vulkan.material.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.DescriptorBinding;
import com.jme3.vulkan.descriptors.DescriptorType;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class TextureUniform extends DescriptorBinding<Texture> {

    private final IntEnum<VulkanImage.Layout> layout;

    public TextureUniform(DescriptorType type, int descriptorCount, Flag<ShaderStage> stages, IntEnum<VulkanImage.Layout> layout) {
        super(type, new Texture[descriptorCount], stages);
        this.layout = layout;
    }

    @Override
    protected void populateElementInfo(MemoryStack stack, VkWriteDescriptorSet write, int start, int count) {
        VkDescriptorImageInfo.Buffer imgInfo = VkDescriptorImageInfo.calloc(count, stack);
        for (int i = 0; i < count; i++) {
            Texture tex = resources[start + i];
            if (tex != null) {
                imgInfo.get().imageView(tex.getView().getId()).sampler(tex.getId()).imageLayout(layout.getEnum());
            } else {
                imgInfo.get().imageView(VK10.VK_NULL_HANDLE);
            }
        }
        write.pImageInfo(imgInfo);
    }

    public IntEnum<VulkanImage.Layout> getLayout() {
        return layout;
    }

}
