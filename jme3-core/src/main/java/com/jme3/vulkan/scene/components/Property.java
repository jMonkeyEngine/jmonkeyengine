package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicArray;
import com.jme3.vulkan.scene.InheritMode;
import com.jme3.vulkan.scene.Scene;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

// very descriptive and helpful name, I know
public class Property <T> implements Component {

    private static final int PUSH = 0;
    private static final int PULL = 1;
    private static final int IGNORE_PARENT = 2;
    private static final int WORLD_PUSH = 3;
    private static final int FLAG_COUNT = 4;

    private final Scene scene;
    private final DynamicArray<T> localValues, worldValues;
    private final BitSet flags = new BitSet();
    private final Collection<PropertyListener<T>> listeners = new ArrayList<>();
    private boolean worldNeeded = false;

    public Property(Scene scene, DynamicArray<T> localValues, DynamicArray<T> worldValues) {
        this.scene = scene;
        this.localValues = localValues;
        this.worldValues = worldValues;
    }

    public Property(Scene scene, Supplier<DynamicArray<T>> arraySupplier) {
        this(scene, arraySupplier.get(), arraySupplier.get());
    }

    public void set(int node, T value) {
        if (!Objects.equals(localValues.get(node), value)) {
            updateWorld(node);
        }
    }

    public T get(int node) {
        if (!worldValues.isNull(node)) return worldValues.get(node);
        else return localValues.get(node);
    }

    public boolean isNull(int node) {
        return localValues.isNull(node) && worldValues.isNull(node);
    }

    public InheritMode setInheritMode(int node, InheritMode mode) {
        int n = node * FLAG_COUNT;
        switch (mode) {
            case Off: {
                flags.clear(n + PULL);
                flags.clear(n + PUSH);
                flags.clear(n + IGNORE_PARENT);
            } break;
            case Pull: {
                flags.set(n + PULL);
                flags.clear(n + PUSH);
                flags.clear(n + IGNORE_PARENT);
                worldNeeded = true;
            } break;
            case Push: {
                flags.clear(n + PULL);
                flags.set(n + PUSH);
                flags.clear(n + IGNORE_PARENT);
                worldNeeded = true;
            } break;
            case OffIgnoreParent: {
                flags.clear(n + PULL);
                flags.clear(n + PUSH);
                flags.set(n + IGNORE_PARENT);
            } break;
            case PushIgnoreParent: {
                flags.clear(n + PULL);
                flags.set(n + PUSH);
                flags.set(n + IGNORE_PARENT);
                worldNeeded = true;
            } break;
        }
        updateWorld(node);
        return mode;
    }

    public void addListener(PropertyListener<T> listener) {
        listeners.add(listener);
    }

    @Override
    public void onNodeDestroyed(int node) {
        int n = node * FLAG_COUNT;
        flags.clear(n, n + FLAG_COUNT);
        localValues.set(node, null);
        updateWorld(node);
    }

    protected void updateWorld(int node) {
        if (!worldNeeded) {
            return;
        }
        Scene.DepthFirstIterator it = scene.depthFirst(node);
        for (int n : it) {
            int parent = scene.getParentId(n);
            int srcIndex = n;
            if (parent >= 0 && !worldValues.isNull(parent) && !isFlag(n, IGNORE_PARENT) && (isFlag(n, PULL) || isFlag(parent, PUSH))) {
                srcIndex = parent;
            }
            T current = worldValues.get(n);
            T newValue = (srcIndex == n ? localValues : worldValues).get(srcIndex);
            if (!Objects.equals(current, newValue)) for (PropertyListener<T> l : listeners) {
                l.onPropertyChanged(this, n);
            }
            flags.set(n * FLAG_COUNT + WORLD_PUSH, parent >= 0 && (isFlag(parent, PUSH) || isFlag(parent,  WORLD_PUSH)));
        }
    }

    private boolean isFlag(int node, int flag) {
        return flags.get(node * FLAG_COUNT + flag);
    }

}
