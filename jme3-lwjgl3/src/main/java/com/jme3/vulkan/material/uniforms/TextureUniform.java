package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.data.DataPipe;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.images.Texture;
import com.jme3.vulkan.images.VulkanImage;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class TextureUniform extends AbstractUniform<Texture> {

    private final VulkanImage.Layout layout;
    private DataPipe<? extends Texture> pipe;
    private Texture texture;

    public TextureUniform(String name, VulkanImage.Layout layout, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, Descriptor.CombinedImageSampler, bindingIndex, stages);
        this.layout = layout;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(1, stack)
                .imageView(texture.getImage().getNativeObject())
                .sampler(texture.getNativeObject())
                .imageLayout(layout.getEnum());
        write.pImageInfo(info)
                .descriptorType(type.getVkEnum())
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorCount(1);
    }

    @Override
    public void update(CommandBuffer cmd) {
        texture = pipe.execute(cmd);
    }

    @Override
    public void setPipe(DataPipe<? extends Texture> pipe) {
        this.pipe = pipe;
    }

    @Override
    public DataPipe<? extends Texture> getPipe() {
        return pipe;
    }

    @Override
    public Texture getValue() {
        return texture;
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type == binding.getType()
            && bindingIndex == binding.getBinding()
            && binding.getDescriptors() == 1;
    }

    public VulkanImage.Layout getLayout() {
        return layout;
    }

}
