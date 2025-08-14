package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.pipelines.Pipeline;

import java.util.*;

public class UniformSet {

    private final Uniform[] uniforms;
    private final List<DescriptorSet> sets = new ArrayList<>();
    private final Collection<DescriptorSet> outdatedSets = new ArrayList<>();
    private DescriptorSet activeSet;

    public UniformSet(Uniform... uniforms) {
        this.uniforms = uniforms;
        // ensure duplicate binding indices are not present
        HashSet<Integer> bindings = new HashSet<>();
        for (Uniform u : uniforms) {
            if (!bindings.add(u.getBindingIndex())) {
                throw new IllegalArgumentException("Duplicate binding index in set: " + u.getBindingIndex());
            }
        }
    }

    public void addActiveSet(DescriptorSet activeSet) {
        this.activeSet = activeSet;
        sets.add(activeSet);
    }

    public DescriptorSetLayout selectExistingActiveSet(List<DescriptorSetLayout> availableLayouts) {
        if (activeSet == null || !availableLayouts.remove(activeSet.getLayout())) {
            for (DescriptorSet s : sets) {
                if (availableLayouts.remove(s.getLayout())) {
                    this.activeSet = s;
                    return null;
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
                return layout;
            }
            throw new IllegalStateException("Pipeline layout does not support uniform set.");
        }
        return null;
    }

    public void update(Pipeline pipeline) {
        for (DescriptorSet set : sets) {
            set.update(false, uniforms);
        }
        for (Uniform u : uniforms) {
            u.clearUpdateNeeded();
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
