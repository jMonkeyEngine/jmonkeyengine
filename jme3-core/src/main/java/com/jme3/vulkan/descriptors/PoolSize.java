package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.util.IntEnum;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorPoolSize;

public class PoolSize {

    private final IntEnum<Descriptor> type;
    private final int size;

    public PoolSize(IntEnum<Descriptor> type, int size) {
        this.type = type;
        this.size = size;
    }

    public IntEnum<Descriptor> getType() {
        return type;
    }

    public int getSize() {
        return size;
    }

    public static VkDescriptorPoolSize.Buffer aggregate(MemoryStack stack, PoolSize... sizes) {
        VkDescriptorPoolSize.Buffer buffer = VkDescriptorPoolSize.calloc(sizes.length, stack);
        for (PoolSize poolSize : sizes) {
            buffer.get().set(poolSize.type.getEnum(), poolSize.size);
        }
        buffer.flip();
        return buffer;
    }

}
