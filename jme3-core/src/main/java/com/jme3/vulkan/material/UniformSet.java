package com.jme3.vulkan.material;

import com.jme3.util.IntMap;
import com.jme3.vulkan.descriptors.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.Uniform;

import java.util.*;

public class UniformSet implements Iterable<Uniform> {

    private final int setIndex;
    private final Uniform[] uniforms;
    private final Collection<UniformConfig> configs = new ArrayList<>();
    private int configCacheTimeout = 60;

    public UniformSet(int setIndex, Uniform... uniforms) {
        this.setIndex = setIndex;
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

    public DescriptorSet acquire(DescriptorPool pool, List<DescriptorSetLayout> availableLayouts) {
        configs.removeIf(UniformConfig::cycleTimeout);
        DescriptorSetWriter[] writers = Arrays.stream(uniforms).map(Uniform::createWriter).toArray(DescriptorSetWriter[]::new);
        UniformConfig data = configs.stream().filter(f -> f.isCurrent(writers)).findAny().orElse(null);
        if (data == null) {
            configs.add(data = new UniformConfig(writers));
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

    public void setConfigCacheTimeout(int configCacheTimeout) {
        this.configCacheTimeout = configCacheTimeout;
    }

    public int getSetIndex() {
        return setIndex;
    }

    public int getConfigCacheTimeout() {
        return configCacheTimeout;
    }

    /**
     * Maps {@link DescriptorSetLayout DescriptorSetLayouts} to {@link DescriptorSet DescriptorSets}
     * by the ID of the DescriptorSetLayout for a particular combination of uniform values. If no
     * available layout has a mapped set, a new set is allocated with an available compatible layout
     * and mapped with it.
     */
    private class UniformConfig {

        private final DescriptorSetWriter[] writers;
        private final Map<Long, DescriptorSet> sets = new HashMap<>();
        private int timeout = configCacheTimeout;

        public UniformConfig(DescriptorSetWriter[] writers) {
            this.writers = writers;
        }

        public boolean cycleTimeout() {
            return timeout-- <= 0;
        }

        public boolean isCurrent(DescriptorSetWriter[] writers) {
            for (int i = 0; i < writers.length; i++) {
                if (!this.writers[i].equals(writers[i])) {
                    return false;
                }
            }
            return true;
        }

        public DescriptorSet get(DescriptorPool pool, List<DescriptorSetLayout> availableLayouts) {
            timeout = configCacheTimeout;
            // check layouts against cached sets
            for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                DescriptorSet set = sets.get(layout.getNativeObject());
                if (set != null) {
                    it.remove();
                    return set;
                }
            }
            // check layouts for a compatible layout to create a new set
            for (Iterator<DescriptorSetLayout> it = availableLayouts.iterator(); it.hasNext();) {
                DescriptorSetLayout layout = it.next();
                if (isLayoutCompatible(layout)) {
                    it.remove();
                    DescriptorSet set = pool.allocateSets(layout)[0];
                    sets.put(set.getLayout().getNativeObject(), set);
                    set.write(writers);
                    return set;
                }
            }
            return null;
        }

        private boolean isLayoutCompatible(DescriptorSetLayout layout) {
            for (Uniform u : uniforms) {
                for (IntMap.Entry<SetLayoutBinding> b : layout.getBindings()) {
                    if (u.isBindingCompatible(b.getValue())) {
                        return true;
                    }
                }
            }
            return false;
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
