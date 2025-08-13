package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.SetLayoutBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class UniformSet {

    private final Uniform[] uniforms;
    private final List<DescriptorSet> sets = new ArrayList<>();
    private DescriptorSet activeSet;
    private boolean updateFlag = true;

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
        if (this.activeSet == activeSet) {
            updateFlag = true;
        }
        this.activeSet = activeSet;
        sets.add(activeSet);
    }

    public DescriptorSetLayout selectActiveSet(List<DescriptorSetLayout> availableLayouts) {
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

    public void update() {
        if (activeSet == null) {
            throw new NullPointerException("No descriptor set selected.");
        }
        activeSet.update(updateFlag, uniforms);
        updateFlag = false;
    }

    public void setActiveSet(DescriptorSet activeSet) {
        this.activeSet = activeSet;
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
