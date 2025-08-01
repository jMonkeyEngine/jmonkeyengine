package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSet {

    private final LogicalDevice device;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final long id;

    public DescriptorSet(LogicalDevice device, DescriptorPool pool, DescriptorSetLayout layout, long id) {
        this.device = device;
        this.pool = pool;
        this.layout = layout;
        this.id = id;
    }

    public void write(DescriptorSetWriter... writers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(writers.length, stack);
            for (DescriptorSetWriter w : writers) {
                w.populateWrite(stack, write.get().sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(id));
            }
            write.flip();
            vkUpdateDescriptorSets(device.getNativeObject(), write, null);
        }
    }

    public void free() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkFreeDescriptorSets(device.getNativeObject(), pool.getNativeObject(), stack.longs(id));
        }
    }

    public long getId() {
        return id;
    }

}
