package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.VulkanObject;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSet {

    private final LogicalDevice<?> device;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final long id;
    private final Collection<DescriptorSetWriter> writers = new ArrayList<>();

    public DescriptorSet(LogicalDevice<?> device, DescriptorPool pool, DescriptorSetLayout layout, long id) {
        this.device = device;
        this.pool = pool;
        this.layout = layout;
        this.id = id;
    }

    public void update() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int updating = 0;
            for (DescriptorSetWriter w : writers) {
                if (w.isUpdateNeeded()) {
                    updating++;
                }
            }
            if (updating > 0) {
                VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(updating, stack);
                for (DescriptorSetWriter w : writers) {
                    if (w.isUpdateNeeded()) {
                        w.populateWrite(stack, write.get()
                                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                                .dstSet(id));
                    }
                }
                write.flip();
                vkUpdateDescriptorSets(device.getNativeObject(), write, null);
            }
        }
    }

    public void write(DescriptorSetWriter... writers) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(writers.length, stack);
            for (DescriptorSetWriter w : writers) {
                w.populateWrite(stack, write.get()
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .dstSet(id));
            }
            write.flip();
            vkUpdateDescriptorSets(device.getNativeObject(), write, null);
        }
    }

    public void free() {
        if (pool.getNativeReference().isDestroyed()) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkFreeDescriptorSets(device.getNativeObject(), pool.getNativeObject(), stack.longs(id));
        }
    }

    public long getId() {
        return id;
    }

    public LogicalDevice<?> getDevice() {
        return device;
    }

    public DescriptorPool getPool() {
        return pool;
    }

    public DescriptorSetLayout getLayout() {
        return layout;
    }

}
