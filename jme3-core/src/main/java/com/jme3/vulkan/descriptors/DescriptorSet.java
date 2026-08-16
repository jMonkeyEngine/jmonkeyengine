package com.jme3.vulkan.descriptors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;
import java.util.function.Function;

import static org.lwjgl.vulkan.VK10.vkFreeDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

public class DescriptorSet {

    private final long handle;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final Map<Integer, DescriptorBinding> bindings = new HashMap<>();
    private final BitSet usedBindings = new BitSet();

    public DescriptorSet(DescriptorPool pool, DescriptorSetLayout layout, long id) {
        this.pool = pool;
        this.layout = layout;
        this.handle = id;
    }

    /**
     * Assigns {@code binding} to the specified binding slot.
     *
     * @param bindingSlot target binding slot
     * @param binding binding to assign, or null to not have anything assigned to that slot
     */
    public void setBinding(int bindingSlot, @NonNull DescriptorBinding binding) {
        bindings.put(bindingSlot, binding);
        usedBindings.set(bindingSlot);
        binding.setWriteNeeded();
    }

    public void setBinding(int bindingSlot, @NonNull Function<DescriptorSetLayout.Binding, DescriptorBinding> binding) {
        DescriptorBinding b = binding.apply(layout.getInfo().getBindings().get(bindingSlot));
        bindings.put(bindingSlot, b);
        usedBindings.set(bindingSlot);
        b.setWriteNeeded();
    }

    public void clearBinding(int bindingSlot) {
        bindings.remove(bindingSlot);
        usedBindings.clear(bindingSlot);
    }

    @SuppressWarnings("unchecked")
    public DescriptorBinding<Object> getBinding(int bindingSlot) {
        return bindings.get(bindingSlot);
    }

    /**
     * Writes all changes to {@link DescriptorBinding DescriptorBindings} in this set to
     * the device.
     */
    public void update() {
        int writesNeeded = 0;
        for (DescriptorBinding b : bindings.values()) {
            writesNeeded += b.getDescriptorWritesNeeded();
        }
        if (writesNeeded > 0) try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(writesNeeded, stack);
            for (Map.Entry<Integer, DescriptorBinding> e : bindings.entrySet()) {
                e.getValue().populateWriteInfo(stack, write, this, e.getKey());
            }
            if (write.position() > 0) {
                vkUpdateDescriptorSets(pool.getDevice().getHandle(), write.flip(), null);
            }
        }
    }

    /**
     * Frees this descriptor set. If multiple descriptor sets from the same {@link DescriptorPool}
     * need to be reset at once, it is recommended to use {@link DescriptorPool#free(Collection)}
     * instead.
     */
    public void free() {
        pool.free(Collections.singleton(this));
    }

    public long getHandle() {
        return handle;
    }

    public DescriptorPool getPool() {
        return pool;
    }

    public DescriptorSetLayout getLayout() {
        return layout;
    }

}
