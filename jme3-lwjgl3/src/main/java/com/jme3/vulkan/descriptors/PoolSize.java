package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

import static org.lwjgl.vulkan.VK10.*;

public class PoolSize {

    private final Descriptor type;
    private final int size;

    public PoolSize(Descriptor type, int size) {
        this.type = type;
        this.size = size;
    }

    public Descriptor getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public static VkDescriptorPoolSize.Buffer aggregate(MemoryStack stack, PoolSize... sizes) {
        VkDescriptorPoolSize.Buffer buffer = VkDescriptorPoolSize.calloc(sizes.length, stack);
        for (PoolSize poolSize : sizes) {
            buffer.get().set(poolSize.type.getVkEnum(), poolSize.size);
        }
        buffer.flip();
        return buffer;
    }

}
