package com.jme3.vulkan.scene.components;

import com.jme3.util.collections.DynamicArray;
import com.jme3.util.collections.DynamicObjectArray;
import com.jme3.vulkan.descriptors.DescriptorSet;
import com.jme3.vulkan.scene.InheritMode;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

public class TextureComponentType implements Component<DescriptorSet> {

    private final Supplier<DescriptorSet> factory;
    private final DynamicArray<DescriptorSet> sets = new DynamicObjectArray<>(32);

    @Override
    public void set(int node, @Nullable DescriptorSet value) {}

    @Override
    public @Nullable DescriptorSet get(int node) {
        return null;
    }

    @Override
    public boolean isPresent(int node) {
        return false;
    }

    @Override
    public InheritMode setInheritMode(int node, InheritMode mode) {
        return InheritMode.OffIgnoreParent;
    }

    @Override
    public void reset(int node) {

    }

    @Override
    public void addPipe(ComponentPipe pipe) {

    }

    @Override
    public void componentChangeObserved(Component type, int node) {
        if (sets.isNull(node)) {
            DescriptorSet s = factory.get();
            sets.set(node, s);
            s.getBinding(0).set(0, type.get(node));
        }
    }

}
