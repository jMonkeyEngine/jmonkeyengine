package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class BufferUniform extends AbstractUniform<GpuBuffer> {

    private static final String BUFFER_NULL_ERROR = "Uniform buffer is null.";

    private VersionedResource<GpuBuffer> resource;
    private boolean updateFlag = true;

    public BufferUniform(String name, Descriptor type, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, type, bindingIndex, stages);
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        GpuBuffer buffer = resource.getVersion();
        VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(buffer.getId())
                .offset(0L)
                .range(buffer.size().getBytes());
        write.pBufferInfo(info)
                .descriptorCount(1)
                .dstArrayElement(0)
                .dstBinding(bindingIndex)
                .descriptorType(type.getVkEnum());
        updateFlag = false;
    }

    @Override
    public boolean update(LogicalDevice<?> device) {
        if (resource == null) {
            throw new NullPointerException(BUFFER_NULL_ERROR);
        }
        return updateFlag;
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type == binding.getType()
            && bindingIndex == binding.getBinding();
    }

    @Override
    public void setValue(VersionedResource<GpuBuffer> value) {
        if (this.resource != value) {
            this.resource = value;
            updateFlag = true;
        }
    }

    @Override
    public VersionedResource<GpuBuffer> getValue() {
        return resource;
    }

}
