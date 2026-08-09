package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.vulkan.VK10.vkFreeDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

public class DescriptorSet extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final Map<Integer, DescriptorBinding> bindings = new HashMap<>();

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

    public void setBinding(int bindingSlot, DescriptorBinding binding) {
        bindings.put(bindingSlot, binding);
    }

    public void update() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(bindings.size(), stack);
            for (Map.Entry<Integer, DescriptorBinding> e : bindings.entrySet()) {
                e.getValue().populateWriteInfo(stack, write, this, e.getKey());
            }
            if (write.position() > 0) {
                vkUpdateDescriptorSets(device.getNativeObject(), write.flip(), null);
            }
        }
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
