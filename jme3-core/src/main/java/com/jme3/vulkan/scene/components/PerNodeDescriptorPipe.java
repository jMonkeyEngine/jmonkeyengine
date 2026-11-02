package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicArray;
import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.scene.Scene;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;

public abstract class PerNodeDescriptorPipe implements Component {

    private final Scene scene;
    private final DynamicArray<DescriptorSet> sets = new DynamicObjectArray<>(32);
    private final Collection<PropertyReceiver<?>> receivers = new ArrayList<>();
    private final Queue<DescriptorSet> recycle = new ArrayDeque<>();

    protected PerNodeDescriptorPipe(Scene scene) {
        this.scene = scene;
    }

    /**
     * Maps the property {@code type} to the descriptor at {@code binding}/{@code descriptor}.
     * Whenever a node's value for the property changes, that node's descriptor set (if it
     * has one) is given the new value and {@link DescriptorSet#update() updated}.
     *
     * @param type property type to map
     * @param binding binding slot
     * @param descriptor descriptor index within the binding
     */
    protected final <E, P extends Property<E>> void map(Class<P> type, int binding, int descriptor) {
        receivers.add(new PropertyReceiver<>(type, binding, descriptor).register());
    }

    private DescriptorSet createOrRecycleSet(int node) {
        DescriptorSet set = recycle.poll();
        if (set == null) {
            set = createDescriptorSet(node);
        }
        for (PropertyReceiver<?> r : receivers) {
            r.apply(set, node);
        }
        set.update();
        return set;
    }

    protected abstract DescriptorSet createDescriptorSet(int node);

    /**
     * Gets {@code node}'s descriptor set, creating (or recycling) one filled with the
     * node's current property values if it has none yet.
     *
     * @param node node id
     * @return node's descriptor set
     */
    public DescriptorSet getDescriptorSet(int node) {
        return sets.computeIfNull(node, this::createOrRecycleSet);
    }

    @Override
    public void onNodeDestroyed(int node) {
        sets.clear(node, recycle::add);
    }

    private class PropertyReceiver <E> implements PropertyListener<E> {

        private final Class<? extends Property<E>> type;
        private final int binding, descriptor;

        private PropertyReceiver(Class<? extends Property<E>> type, int binding, int descriptor) {
            this.type = type;
            this.binding = binding;
            this.descriptor = descriptor;
        }

        public PropertyReceiver<E> register() {
            scene.getComponent(type).addListener(this);
            return this;
        }

        public void apply(DescriptorSet set, int node) {
            set.set(binding, descriptor, scene.getComponent(type).get(node));
        }

        @Override
        public void onPropertyChanged(Property<E> property, int node) {
            DescriptorSet set = sets.get(node);
            if (set != null) {
                set.set(binding, descriptor, property.get(node));
            }
        }

    }

}
