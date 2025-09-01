package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.newframes.Resource;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class BufferUniform extends AbstractUniform<GpuBuffer> {

    private Resource<GpuBuffer> resource;
    private GpuBuffer buffer;
    private long variant = 0L;

    public BufferUniform(String name, Descriptor type, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, type, bindingIndex, stages);
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(buffer.getId())
                .offset(0L)
                .range(buffer.size().getBytes());
        write.pBufferInfo(info)
                .descriptorCount(1)
                .dstArrayElement(0)
                .dstBinding(bindingIndex)
                .descriptorType(type.getVkEnum());
    }

    @Override
    public void update(LogicalDevice<?> device) {
        buffer = resource.execute();
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type == binding.getType()
            && bindingIndex == binding.getBinding()
            && binding.getDescriptors() == 1;
    }

    @Override
    public void setResource(Resource<GpuBuffer> resource) {
        if (this.resource != resource) {
            this.resource = resource;
            variant++;
        }
    }

    @Override
    public Resource<GpuBuffer> getResource() {
        return resource;
    }

    @Override
    public GpuBuffer getValue() {
        return buffer;
    }

    @Override
    public long getVariant() {
        return variant;
    }

}
