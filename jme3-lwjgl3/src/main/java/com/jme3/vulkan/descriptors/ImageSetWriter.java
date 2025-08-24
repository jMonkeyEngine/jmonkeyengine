package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class ImageSetWriter extends BaseDescriptorWriter {

    private final ImageDescriptor[] descriptors;

    public ImageSetWriter(Descriptor type, int binding, int arrayElement, ImageDescriptor... descriptors) {
        super(type, binding, arrayElement, descriptors.length);
        this.descriptors = descriptors;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        super.populateWrite(stack, write);
        VkDescriptorImageInfo.Buffer info = VkDescriptorImageInfo.calloc(descriptors.length, stack);
        for (ImageDescriptor d : descriptors) {
            d.fillDescriptorInfo(info.get());
        }
        info.flip();
        write.pImageInfo(info);
    }

}
