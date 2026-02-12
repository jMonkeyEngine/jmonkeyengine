package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.Collection;

import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSet extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;

    public DescriptorSet(LogicalDevice<?> device, DescriptorPool pool, DescriptorSetLayout layout, long id) {
        this.device = device;
        this.pool = pool;
        this.layout = layout;
        this.object = id;
        if (pool.getFlags().contains(DescriptorPool.Create.FreeDescriptorSets)) {
            ref = DisposableManager.reference(this);
            pool.getNativeReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyer() {
        return () -> { try (MemoryStack stack = MemoryStack.stackPush()) {
            vkFreeDescriptorSets(device.getNativeObject(), pool.getNativeObject(), stack.longs(object));
        }};
    }

    public void write(Collection<DescriptorSetWriter> writers) {
        if (writers.isEmpty()) return;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(writers.size(), stack);
            for (DescriptorSetWriter w : writers) {
                w.populateWrite(stack, write.get()
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(object));
            }
            write.flip();
            vkUpdateDescriptorSets(device.getNativeObject(), write, null);
        }
    }

    @Deprecated
    public long getId() {
        return object;
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
