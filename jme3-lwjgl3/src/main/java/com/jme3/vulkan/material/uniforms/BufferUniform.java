package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Objects;
import java.util.function.Function;

public class BufferUniform extends AbstractUniform<GpuBuffer> {

    private GpuBuffer buffer;
    private boolean updateFlag = true;
    private Function<LogicalDevice<?>, GpuBuffer> bufferFactory;

    public BufferUniform(String name, Descriptor type, int bindingIndex) {
        super(name, type, bindingIndex);
    }

    public BufferUniform(String name, Descriptor type, int bindingIndex, GpuBuffer buffer) {
        super(name, type, bindingIndex);
        this.buffer = buffer;
    }

    public BufferUniform(String name, Descriptor type, int bindingIndex, Function<LogicalDevice<?>, GpuBuffer> bufferFactory) {
        super(name, type, bindingIndex);
        this.bufferFactory = bufferFactory;
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
                buffer = bufferFactory.apply(device);
                updateFlag = true;
            } else {
                throw new NullPointerException("Uniform buffer is null.");
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

    public void setStruct(Struct<?> struct) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Objects.requireNonNull(buffer, "Uniform buffer is null.").copy(stack, struct);
        }
    }

    public Function<LogicalDevice<?>, GpuBuffer> getBufferFactory() {
        return bufferFactory;
    }

}
