package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import static org.lwjgl.vulkan.VK10.*;

public class PoolSize {

    private final int type;
    private final int size;

    public PoolSize(int type, int size) {
        this.type = type;
        this.size = size;
    }

    public int getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public static PoolSize uniformBuffers(int size) {
        return new PoolSize(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, size);
    }

    public static PoolSize combinedImageSamplers(int size) {
        return new PoolSize(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, size);
    }

    public static VkDescriptorPoolSize.Buffer aggregate(MemoryStack stack, PoolSize... sizes) {
        VkDescriptorPoolSize.Buffer buffer = VkDescriptorPoolSize.calloc(sizes.length, stack);
        for (PoolSize poolSize : sizes) {
            buffer.get().set(poolSize.type, poolSize.size);
        }
        buffer.flip();
        return buffer;
    }

}
