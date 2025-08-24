package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.Uniform;

import java.util.*;

public class UniformSet implements Iterable<Uniform> {

    private final Uniform[] uniforms;
    private final FrameIndex currentFrame;
    private final Map<FrameIndex, FrameData> frames = new HashMap<>();

    public UniformSet(Uniform... uniforms) {
        this.uniforms = Objects.requireNonNull(uniforms);
        this.currentFrame = new FrameIndex(this.uniforms, false);
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

    public DescriptorSet update(LogicalDevice<?> device, DescriptorPool pool, List<DescriptorSetLayout> availableLayouts) {
        for (Uniform<?> u : uniforms) {
            u.update(device);
            if (u.getValue() == null) {
                throw new NullPointerException("Uniform \"" + u.getName() + "\" contains no value.");
            }
        }
        currentFrame.update(uniforms);
        FrameData data = frames.get(currentFrame);
        if (data == null) {
            frames.put(currentFrame.copy(), data = new FrameData());
        }
        return data.get(pool, availableLayouts).update(uniforms);
    }

    public DescriptorSetLayout createLayout(LogicalDevice<?> device) {
        SetLayoutBinding[] bindings = new SetLayoutBinding[uniforms.length];
        for (int i = 0; i < uniforms.length; i++) {
            bindings[i] = uniforms[i].createBinding();
        }
        return new DescriptorSetLayout(device, bindings);
    }

    @Override
    public Iterator<Uniform> iterator() {
        return new SetIterator();
    }

    /**
     * Maps {@link DescriptorSetLayout DescriptorSetLayouts} to {@link SupportedSet SupportedSets}
     * by the ID of the DescriptorSetLayout. If no available layout has a mapped set, a new set
     * is allocated with an available compatible layout and mapped with it.
     */
    private class FrameData {

        private final Map<Long, SupportedSet> sets = new HashMap<>();

        public SupportedSet get(DescriptorPool pool, List<DescriptorSetLayout> availableLayouts) {
            for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                SupportedSet set = sets.get(layout.getNativeObject());
                if (set != null) {
                    it.remove();
                    return set;
                }
            }
            for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                if (isLayoutCompatible(layout)) {
                    it.remove();
                    SupportedSet set = new SupportedSet(pool.allocateSets(layout)[0]);
                    sets.put(set.set.getLayout().getNativeObject(), set);
                    return set;
                }
            }
            throw new UnsupportedOperationException("Material is not compatible with the pipeline (uniform set not supported).");
        }

        private boolean isLayoutCompatible(DescriptorSetLayout layout) {
            for (Uniform u : uniforms) {
                for (SetLayoutBinding b : layout.getBindings()) {
                    if (u.isBindingCompatible(b)) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    /**
     * Identifies a FrameData by the resource versions the uniforms are or were using.
     */
    private static class FrameIndex {

        private final int[] versions;

        private FrameIndex(Uniform[] uniforms, boolean update) {
            versions = new int[uniforms.length];
            if (update) {
                update(uniforms);
            }
        }

        private FrameIndex(FrameIndex index) {
            versions = new int[index.versions.length];
            copy(index);
        }

        public void update(Uniform[] uniforms) {
            if (versions.length != uniforms.length) {
                throw new IllegalArgumentException("Index must contain one version per uniform.");
            }
            for (int i = 0; i < uniforms.length; i++) {
                versions[i] = uniforms[i].getValue().getCurrentVersionIndex();
            }
        }

        public void copy(FrameIndex source) {
            if (versions.length != source.versions.length) {
                throw new IllegalArgumentException("Copy source and target do not have the same number of versions.");
            }
            System.arraycopy(source.versions, 0, versions, 0, versions.length);
        }

        public FrameIndex copy() {
            return new FrameIndex(this);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            FrameIndex that = (FrameIndex)o;
            if (versions == that.versions) return true;
            if (versions.length != that.versions.length) return false;
            for (int i = 0; i < versions.length; i++) {
                if (versions[i] != that.versions[i]) return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(versions);
        }

    }

    private static class SupportedSet {

        private final DescriptorSet set;
        private long[] variants;

        public SupportedSet(DescriptorSet set) {
            this.set = set;
        }

        public DescriptorSet update(Uniform[] uniforms) {
            if (variants == null) {
                // write all uniforms
                variants = new long[uniforms.length];
                set.write(uniforms);
            } else {
                // write only uniforms that have changed since the last update with this set
                final ArrayList<Uniform> writers = new ArrayList<>(uniforms.length);
                for (int i = 0; i < variants.length; i++) {
                    if (variants[i] < uniforms[i].getVariant()) {
                        writers.add(uniforms[i]);
                    }
                }
                if (!writers.isEmpty()) {
                    set.write(writers.toArray(new Uniform[0]));
                }
            }
            // update variant counters to indicate that all uniforms have been
            // evaluated since they were last updated
            for (int i = 0; i < variants.length; i++) {
                variants[i] = uniforms[i].getVariant();
            }
            return set;
        }

    }

    private class SetIterator implements Iterator<Uniform> {

        private int index = 0;

        @Override
        public boolean hasNext() {
            return index < uniforms.length;
        }

        @Override
        public Uniform next() {
            return uniforms[index++];
        }

    }

}
