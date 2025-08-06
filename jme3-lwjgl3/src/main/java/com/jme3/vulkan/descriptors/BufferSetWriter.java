package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.*;

public class BufferSetWriter extends DescriptorSetWriter {

    private final BufferDescriptor[] descriptors;

    public BufferSetWriter(int type, int binding, int arrayElement, BufferDescriptor... descriptors) {
        super(type, binding, arrayElement, descriptors.length);
        this.descriptors = descriptors;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        super.populateWrite(stack, write);
        VkDescriptorBufferInfo.Buffer info = VkDescriptorBufferInfo.calloc(descriptors.length, stack);
        for (BufferDescriptor d : descriptors) {
            d.fillDescriptorInfo(info.get());
        }
        info.flip();
        write.pBufferInfo(info);
    }

    public static BufferSetWriter uniformBuffers(int binding, int arrayElement, BufferDescriptor... descriptors) {
        return new BufferSetWriter(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, binding, arrayElement, descriptors);
    }

    public static BufferSetWriter storageBuffers(int binding, int arrayElement, BufferDescriptor... descriptors) {
        return new BufferSetWriter(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, binding, arrayElement, descriptors);
    }

}
