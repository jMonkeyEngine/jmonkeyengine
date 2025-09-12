package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class BufferSetWriter extends BaseDescriptorWriter {

    private final BufferDescriptor[] descriptors;

    public BufferSetWriter(Descriptor type, int binding, int arrayElement, BufferDescriptor... descriptors) {
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

}
