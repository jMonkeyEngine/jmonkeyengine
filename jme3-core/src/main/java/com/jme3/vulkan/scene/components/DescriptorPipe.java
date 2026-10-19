package com.jme3.vulkan.scene.components;

import com.jme3.util.Tuple2;
import com.jme3.util.collections.DynamicArray;
import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.scene.Scene;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

public abstract class DescriptorPipe implements ComponentPipe {

    private final Scene scene;
    private final DynamicArray<DescriptorSet> sets = new DynamicObjectArray<>(32);
    private final Map<Class<? extends Component>, Tuple2<Integer, Integer>> mappings = new HashMap<>();
    private final Queue<DescriptorSet> recycle = new ArrayDeque<>();

    protected DescriptorPipe(Scene scene) {
        this.scene = scene;
    }

    @Override
    public void onComponentChanged(Component type, int node) {
        DescriptorSet s = sets.get(node);
        if (s != null) {
            Tuple2<Integer, Integer> m = mappings.get(type.getClass());
            if (m != null) {
                s.getBinding(m.item1).set(m.item2, type.get(node));
            }
        }
    }

    @Override
    public void activate(int node) {
        if (sets.fillIfNull(node, this::createOrRecycleSet)) {
            for (Class<? extends Component> t : getObservedTypes()) {
                onComponentChanged(scene.getComponent(t), node);
            }
        }
    }

    @Override
    public void deactivate(int node) {
        sets.clear(node, recycle::add);
    }

    protected final void map(Class<? extends Component> type, int binding, int descriptor) {
        mappings.put(type, new Tuple2<>(binding, descriptor));
    }

    private DescriptorSet createOrRecycleSet(int node) {
        DescriptorSet r = recycle.poll();
        if (r == null) {
            r = createDescriptorSet(node);
        }
        return r;
    }

    protected abstract DescriptorSet createDescriptorSet(int node);

    public DescriptorSet getDescriptorSet(int node) {
        return sets.get(node);
    }

}
