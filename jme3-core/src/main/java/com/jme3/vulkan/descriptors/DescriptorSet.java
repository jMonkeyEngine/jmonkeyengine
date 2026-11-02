package com.jme3.vulkan.descriptors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;

import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

public class DescriptorSet {

    private final long handle;
    private final DescriptorPool pool;
    private final DescriptorSetLayout layout;
    private final Map<Integer, Object[]> resources = new HashMap<>();

    public DescriptorSet(DescriptorPool pool, DescriptorSetLayout layout, long id) {
        this.pool = pool;
        this.layout = layout;
        this.handle = id;
    }

    public void set(int binding, Object value) {
        set(binding, 0, value);
    }

    public void set(int binding, int element, Object value) {
        resources.computeIfAbsent(binding, k -> new Object[layout.getBindings().get(binding).getDescriptors() * 2])[element * 2] = value;
    }

    public void set(int binding, int firstElement, Collection<Object> values) {
        Object[] array = resources.computeIfAbsent(binding, k -> new Object[layout.getBindings().get(binding).getDescriptors() * 2]);
        int i = firstElement * 2;
        for (Object o : values) {
            array[i] = o;
            i += 2;
        }
    }

    public void set(int binding, int firstElement, Object... values) {
        set(binding, firstElement, Arrays.asList(values));
    }

    /**
     * Has every {@link BindingLogic binding logic} of this set's layout
     * emit its artifacts, and writes each artifact that differs from the one currently bound
     * to the device. Consecutive changed descriptors of a binding are written together.
     */
    public void update() {
        Collection<Write<?>> writes = new ArrayList<>();
        for (Map.Entry<Integer, DescriptorSetLayout.Binding> e : layout.getBindings().entrySet()) {
            DescriptorSetLayout.Binding b = e.getValue();
            collectWrites(writes, e.getKey(), e.getValue().getLogic(),
                    resources.computeIfAbsent(e.getKey(), k -> new Object[e.getValue().getDescriptors() * 2]));
        }
        if (!writes.isEmpty()) try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer vkWrites = VkWriteDescriptorSet.calloc(writes.size(), stack);
            for (Write<?> w : writes) {
                w.populate(stack, vkWrites.get());
            }
            vkUpdateDescriptorSets(pool.getDevice().getHandle(), vkWrites.flip(), null);
        }
    }

    public boolean updateNeeded() {
        for (Map.Entry<Integer, DescriptorSetLayout.Binding> e : layout.getBindings().entrySet()) {
            Object[] bound = resources.computeIfAbsent(e.getKey(), k -> new Object[e.getValue().getDescriptors() * 2]);
            for (int i = 0; i < bound.length; i += 2) {
                Object artifact = e.getValue().getLogic().emit(bound[i]);
                if (!Objects.equals(artifact, bound[i + 1])) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Copies the resources assigned to this set into {@code target}, replacing every resource
     * assigned to {@code target}. Only CPU-side state is changed: what is currently written to
     * {@code target} on the device is kept, so {@code target}'s next {@link #update()} writes only
     * the descriptors that differ.
     *
     * @param target set to copy resources into, of the same layout
     */
    public void copyResourcesTo(DescriptorSet target) {
        if (target.layout != layout) {
            throw new IllegalArgumentException("Descriptor sets have different layouts.");
        }
        for (Map.Entry<Integer, Object[]> e : target.resources.entrySet()) {
            Object[] src = resources.get(e.getKey());
            Object[] dst = e.getValue();
            for (int i = 0; i < dst.length; i += 2) {
                dst[i] = src != null ? src[i] : null;
            }
        }
        for (Map.Entry<Integer, Object[]> e : resources.entrySet()) {
            Object[] src = e.getValue();
            Object[] dst = target.resources.computeIfAbsent(e.getKey(), k -> new Object[src.length]);
            for (int i = 0; i < src.length; i += 2) {
                dst[i] = src[i];
            }
        }
    }

    private <T> void collectWrites(Collection<Write<?>> writes, int binding, BindingLogic<Object, T> logic, Object[] bound) {
        Write<T> run = null;
        for (int i = 0; i < bound.length; i += 2) {
            T artifact = logic.emit(bound[i]);
            if (Objects.equals(artifact, bound[i + 1])) {
                run = null;
                continue;
            }
            bound[i + 1] = artifact;
            if (run == null) {
                writes.add(run = new Write<>(binding, i / 2, logic));
            }
            run.artifacts.add(artifact);
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

    /**
     * Run of consecutive changed descriptors within one binding.
     */
    private class Write <T> {

        private final int binding, start;
        private final BindingLogic<Object, T> logic;
        private final List<T> artifacts = new ArrayList<>();

        private Write(int binding, int start, BindingLogic<Object, T> logic) {
            this.binding = binding;
            this.start = start;
            this.logic = logic;
        }

        private void populate(MemoryStack stack, VkWriteDescriptorSet write) {
            logic.populate(stack, write, artifacts);
            write.sType$Default()
                    .descriptorType(layout.getBindings().get(binding).getType().getEnum())
                    .dstSet(handle)
                    .dstBinding(binding)
                    .dstArrayElement(start)
                    .descriptorCount(artifacts.size());
        }

    }

}
