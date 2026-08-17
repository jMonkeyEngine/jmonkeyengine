package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.buffer.tracking.BufferTracker;
import com.jme3.vulkan.buffer.tracking.ExactBufferTracker;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public abstract class DescriptorBinding <T> {

    protected final T[] resources;
    private final DescriptorType type;
    private final BufferTracker writesNeeded = new ExactBufferTracker();

    public DescriptorBinding(DescriptorType type, T[] resources) {
        this.type = type;
        this.resources = resources;
        writesNeeded.add(0, resources.length);
    }

    public void populateWriteInfo(MemoryStack stack, VkWriteDescriptorSet.Buffer write, DescriptorSet set, int binding) {
        for (BufferTracker.Island i : writesNeeded) {
            VkWriteDescriptorSet w = write.get();
            populateElementInfo(stack, w, i.getStart(), i.getSize());
            w.sType$Default()
                .descriptorType(type.getEnum())
                .dstArrayElement(i.getStart())
                .descriptorCount(i.getSize())
                .dstSet(set.getHandle())
                .dstBinding(binding);
        }
    }

    protected abstract void populateElementInfo(MemoryStack stack, VkWriteDescriptorSet write, int start, int count);

    public void set(int descriptor, T resource) {
        resources[descriptor] = resource;
        writesNeeded.add(descriptor, 1);
    }

    public void setWriteNeeded() {
        writesNeeded.add(0, resources.length);
    }

    public void setWriteNeeded(int descriptor) {
        writesNeeded.add(descriptor, 1);
    }

    public void setWriteNeeded(int firstDescriptor, int descriptorCount) {
        writesNeeded.add(firstDescriptor, descriptorCount);
    }

    public int getDescriptorWritesNeeded() {
        return writesNeeded.getNumIslands();
    }

    public T get(int descriptor) {
        return resources[descriptor];
    }

    public void clear(int descriptor) {
        set(descriptor, null);
    }

    public int getDescriptors() {
        return resources.length;
    }

    public DescriptorType getType() {
        return type;
    }

}
