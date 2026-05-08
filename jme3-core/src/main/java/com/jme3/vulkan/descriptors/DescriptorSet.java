package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.experimental.ShaderBindingSet;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;

import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSet extends AbstractNative<Long> implements ShaderBindingSet {

    private final LogicalDevice<?> device;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final Map<Integer, DescriptorSetWriter> writers = new HashMap<>();
    private final Map<Integer, DescriptorSetWriter> staged = new HashMap<>();

    public DescriptorSet(LogicalDevice<?> device, DescriptorPool pool, DescriptorSetLayout layout, long id) {
        this.device = device;
        this.pool = pool;
        this.layout = layout;
        this.object = id;
        if (pool.getFlags().contains(DescriptorPool.Create.FreeDescriptorSets)) {
            ref = DisposableManager.reference(this);
            pool.getReference().addDependent(ref);
        }
    }

    @Override
    public Runnable createDestroyer() {
        return () -> { try (MemoryStack stack = MemoryStack.stackPush()) {
            vkFreeDescriptorSets(device.getNativeObject(), pool.getNativeObject(), stack.longs(object));
        }};
    }

    @Override
    public void stage(int binding, Object value) {
        staged.put(binding, layout.getBindings().get(binding).createWriter(value));
    }

    @Override
    public void write() {
        if (staged.isEmpty()) {
            return;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(staged.size(), stack);
            for (Map.Entry<Integer, DescriptorSetWriter> e : staged.entrySet()) {
                if (Objects.equals(e.getValue(), writers.get(e.getKey()))) {
                    continue;
                }
                VkWriteDescriptorSet nextWrite = write.get();
                e.getValue().populateWrite(stack, device, nextWrite);
                nextWrite.sType$Default()
                        .dstSet(object)
                        .dstBinding(e.getKey());
                writers.put(e.getKey(), e.getValue());
            }
            if (write.position() > 0) {
                vkUpdateDescriptorSets(device.getNativeObject(), write.flip(), null);
            }
        }
        staged.clear();
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
