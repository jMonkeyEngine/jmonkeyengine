package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.images.newimage.EngineImage;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class TextureBinding extends DescriptorBinding<Texture> {

    public TextureBinding(int descriptors, Flag<ShaderStage> stages) {
        super(DescriptorType.CombinedImageSampler, new Texture[descriptors], stages);
    }

    public TextureBinding(DescriptorSetLayout.Binding layout) {
        this(layout.getDescriptors(), layout.getStages());
    }

    @Override
    protected void populateElementInfo(MemoryStack stack, VkWriteDescriptorSet write, int start, int count) {
        VkDescriptorImageInfo.Buffer bufInfo = VkDescriptorImageInfo.calloc(count, stack);
        for (int i = 0; i < count; i++) {
            Texture t = resources[start + i];
            if (t != null) {
                bufInfo.get().imageView(t.getView().getHandle())
                    .sampler(t.getSampler().getHandle())
                    .imageLayout(t.getImage().getLayout().getEnum());
            } else { // requires enabling null descriptors feature/extension
                bufInfo.get().imageView(VK_NULL_HANDLE)
                    .sampler(VK_NULL_HANDLE)
                    .imageView(EngineImage.Layout.Undefined.getEnum());
            }
        }
        write.pImageInfo(bufInfo);
    }

}
