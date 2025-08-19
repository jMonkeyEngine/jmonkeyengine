package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;
import java.util.function.Function;

public class BufferUniform extends AbstractUniform<GpuBuffer> {

    private static final String BUFFER_NULL_ERROR = "Uniform buffer is null.";

    private GpuBuffer buffer;
    private boolean updateFlag = true;
    private Function<LogicalDevice<?>, GpuBuffer> bufferFactory;

    public BufferUniform(String name, Descriptor type, int bindingIndex, Flag<ShaderStage> stages) {
        super(name, type, bindingIndex, stages);
    }

    public BufferUniform(String name, Descriptor type, int bindingIndex, Flag<ShaderStage> stages, GpuBuffer buffer) {
        super(name, type, bindingIndex, stages);
        this.buffer = buffer;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(buffer.getNativeObject())
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
        if (buffer == null) {
            if (bufferFactory != null) {
                buffer = Objects.requireNonNull(bufferFactory.apply(device), "Buffer factory produced null.");
                updateFlag = true;
            } else {
                throw new NullPointerException(BUFFER_NULL_ERROR);
            }
        }
        return updateFlag;
    }

    @Override
    public void setValue(GpuBuffer value) {
        if (this.buffer != value) {
            this.buffer = value;
            updateFlag = true;
        }
    }

    @Override
    public GpuBuffer getValue() {
        return buffer;
    }

    @Override
    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return type == binding.getType()
            && bindingIndex == binding.getBinding()
            && binding.getDescriptors() == 1;
    }

    public void setBufferFactory(Function<LogicalDevice<?>, GpuBuffer> bufferFactory) {
        this.bufferFactory = bufferFactory;
    }

    public Function<LogicalDevice<?>, GpuBuffer> getBufferFactory() {
        return bufferFactory;
    }

}
