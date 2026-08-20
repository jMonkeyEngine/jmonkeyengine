package com.jme3.vulkan.descriptors.uniforms;

import com.jme3.texture.Texture;
import com.jme3.vulkan.descriptors.DescriptorBinding;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.DescriptorType;
import com.jme3.vulkan.images.newimage.EngineImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class TextureBinding extends DescriptorBinding<Texture> {

    public TextureBinding(int descriptors) {
        super(DescriptorType.CombinedImageSampler, new Texture[descriptors]);
    }

    public TextureBinding(DescriptorSetLayout.Binding layout) {
        this(layout.getDescriptors());
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
