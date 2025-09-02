package com.jme3.vulkan.material;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.Uniform;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.*;

public class UniformSet implements Iterable<Uniform> {

    private final Uniform[] uniforms;
    private final Collection<FrameData> activeFrames = new ArrayList<>();
    private int frameCacheTimeout = 10;

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

    @Override
    public Iterator<Uniform> iterator() {
        return new IteratorImpl();
    }

    public void update(CommandBuffer cmd) {
        for (Uniform<?> u : uniforms) {
            u.update(cmd);
            if (u.getValue() == null) {
                throw new NullPointerException("Uniform \"" + u.getName() + "\" contains no value.");
            }
        }
    }

    public DescriptorSet acquireSet(DescriptorPool pool, List<DescriptorSetLayout> availableLayouts) {
        activeFrames.removeIf(FrameData::cycleTimeout);
        FrameData data = activeFrames.stream().filter(FrameData::isCurrent).findAny().orElse(null);
        if (data == null) {
            activeFrames.add(data = new FrameData());
        }
        return data.get(pool, availableLayouts);
    }

    public DescriptorSetLayout createLayout(LogicalDevice<?> device) {
        SetLayoutBinding[] bindings = new SetLayoutBinding[uniforms.length];
        for (int i = 0; i < uniforms.length; i++) {
            bindings[i] = uniforms[i].createBinding();
        }
        return new DescriptorSetLayout(device, bindings);
    }

    public void setFrameCacheTimeout(int frameCacheTimeout) {
        this.frameCacheTimeout = frameCacheTimeout;
    }

    public int getFrameCacheTimeout() {
        return frameCacheTimeout;
    }

    /**
     * Maps {@link DescriptorSetLayout DescriptorSetLayouts} to {@link DescriptorSet DescriptorSets}
     * by the ID of the DescriptorSetLayout for a particular combination of uniform values. If no
     * available layout has a mapped set, a new set is allocated with an available compatible layout
     * and mapped with it.
     */
    private class FrameData {

        private final FrameIndex index = new FrameIndex();
        private final Map<Long, DescriptorSet> sets = new HashMap<>();
        private int timeout = frameCacheTimeout;

        public DescriptorSet get(DescriptorPool pool, List<DescriptorSetLayout> availableLayouts) {
            timeout = frameCacheTimeout;
            for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                DescriptorSet set = sets.get(layout.getNativeObject());
                if (set != null) {
                    it.remove();
                    return set;
                }
            }
            for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                if (isLayoutCompatible(layout)) {
                    it.remove();
                    DescriptorSet set = pool.allocateSets(layout)[0];
                    sets.put(set.getLayout().getNativeObject(), set);
                    set.write(uniforms);
                    return set;
                }
            }
            throw new UnsupportedOperationException("Material is not compatible with the pipeline (uniform set not supported).");
        }

        public boolean cycleTimeout() {
            return --timeout <= 0 || index.isOutOfDate();
        }

        public boolean isCurrent() {
            return index.isCurrent();
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
     * Represents a frame by the values used for that frame. If any resources have
     * been reclaimed, the frame is considered out of date and will be removed.
     */
    private class FrameIndex {

        private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
        private final WeakReference[] versions;

        private FrameIndex() {
            versions = new WeakReference[uniforms.length];
            update();
        }

        public void update() {
            for (int i = 0; i < uniforms.length; i++) {
                versions[i] = new WeakReference<>(uniforms[i].getValue(), queue);
            }
        }

        public boolean isCurrent() {
            for (int i = 0; i < versions.length; i++) {
                // todo: consider using refersTo() from Java 16 to not interfere with the GC
                if (versions[i].get() != uniforms[i].getValue()) {
                    return false;
                }
            }
            return true;
        }

        public int getAccuracy() {
            int accuracy = 0;
            for (int i = 0; i < versions.length; i++) {
                if (versions[i].get() != uniforms[i].getPipe()) {
                    accuracy++;
                }
            }
            return accuracy;
        }

        public boolean isOutOfDate() {
            return queue.poll() != null;
        }

    }

    private class IteratorImpl implements Iterator<Uniform> {

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
