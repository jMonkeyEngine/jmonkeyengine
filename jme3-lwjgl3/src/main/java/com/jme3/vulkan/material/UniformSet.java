package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.Uniform;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.util.*;

public class UniformSet {

    private final Uniform[] uniforms;
    private final List<DescriptorSet> sets = new ArrayList<>();
    private DescriptorSet activeSet;

    public UniformSet(Uniform... uniforms) {
        this.uniforms = uniforms;
        // ensure duplicate binding indices are not present
        BitSet bindings = new BitSet();
        for (Uniform u : uniforms) {
            int i = u.getBindingIndex();
            if (bindings.get(i)) {
                throw new IllegalArgumentException("Duplicate binding index in set: " + u.getBindingIndex());
            }
            bindings.set(i);
        }
    }

    public DescriptorSetLayout createLayout(LogicalDevice<?> device) {
        return new DescriptorSetLayout(device, Arrays.stream(uniforms)
                .map(Uniform::createBinding).toArray(SetLayoutBinding[]::new));
    }

    public void addActiveSet(DescriptorSet activeSet) {
        this.activeSet = activeSet;
        sets.add(activeSet);
    }

    public SetAllocationInfo selectExistingActiveSet(List<DescriptorSetLayout> availableLayouts) {
        if (activeSet == null || !availableLayouts.remove(activeSet.getLayout())) {
            for (DescriptorSet s : sets) {
                if (availableLayouts.remove(s.getLayout())) {
                    this.activeSet = s;
                    return null; // no allocation necessary
                }
            }
            // Search for a layout that is compatible with the set definition
            layoutLoop: for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                requiredLoop: for (Uniform u : uniforms) {
                    // find a layout binding that matches the requirement
                    for (SetLayoutBinding available : layout.getBindings()) {
                        if (u.isBindingCompatible(available)) {
                            // compatible binding found
                            continue requiredLoop;
                        }
                    }
                    // Layout does not contain a binding at the requested index.
                    // Layout is incompatible, try the next layout.
                    continue layoutLoop;
                }
                // Layout is compatible with the set definition
                it.remove();
                return new SetAllocationInfo(this, layout); // allocate new descriptor set
            }
            throw new IllegalStateException("Pipeline layout does not support uniform set.");
        }
        // no allocation necessary
        return null;
    }

    public void update(LogicalDevice<?> device) {
        ArrayList<Uniform<?>> writers = new ArrayList<>(uniforms.length);
        for (Uniform<?> u : uniforms) {
            if (u.update(device)) {
                writers.add(u);
            }
        }
        if (!writers.isEmpty()) try (MemoryStack stack = MemoryStack.stackPush()) {
            VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(writers.size() * sets.size(), stack);
            for (DescriptorSet s : sets) {
                s.populateWriteBuffer(stack, write, writers.toArray(new Uniform[0]));
            }
            VK10.vkUpdateDescriptorSets(device.getNativeObject(), write, null);
        }
    }

    public Uniform[] getUniforms() {
        return uniforms;
    }

    public List<DescriptorSet> getAllocatedSets() {
        return sets;
    }

    public DescriptorSet getActiveSet() {
        return activeSet;
    }

}
