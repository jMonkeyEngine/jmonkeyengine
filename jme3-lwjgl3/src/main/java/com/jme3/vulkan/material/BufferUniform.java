package com.jme3.vulkan.material;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.descriptors.Descriptor;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Struct;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class BufferUniform <T extends GpuBuffer> extends Uniform<T> {

    public BufferUniform(Descriptor type, int bindingIndex) {
        super(type, bindingIndex);
    }

    public BufferUniform(String name, Descriptor type, int bindingIndex) {
        super(name, type, bindingIndex);
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        super.populateWrite(stack, write);
        VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(1, stack)
                .buffer(value.getNativeObject())
                .offset(0L)
                .range(value.size().getBytes());
        write.pBufferInfo(info);
    }

    public void setStruct(Struct<?> struct) {
        if (value == null) {
            throw new NullPointerException("Buffer value is null.");
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            value.copy(stack, struct);
        }
    }

}
