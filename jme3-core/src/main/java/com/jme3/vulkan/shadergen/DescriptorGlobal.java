package com.jme3.vulkan.shadergen;

import com.jme3.vulkan.descriptors.BindingLogic;
import com.jme3.vulkan.descriptors.DescriptorType;
import com.jme3.vulkan.scene.components.Property;
import com.jme3.vulkan.shadergen.vars.Var;

/**
 * A {@link Global} read through descriptors: it occupies the binding of its
 * {@link #getKey() location} in a descriptor set, and resources are assigned to its
 * descriptors per key.
 *
 * @param <T> type of the value
 */
public abstract class DescriptorGlobal <T extends Var> extends BlockedGlobal<Location, T> {

    private DescriptorType type;
    private final int descriptorCount;

    /**
     * @param type descriptor type
     * @param descriptorCount number of descriptors in the binding, at least 1
     * @param location descriptor set and binding
     * @param value value declared by this global
     * @throws IllegalArgumentException if {@code location} is not a descriptor location or
     *                                  {@code descriptorCount} is less than 1
     */
    public DescriptorGlobal(DescriptorType type, int descriptorCount, Location location, T value) {
        super(location, value);
        if (!location.isDescriptor()) {
            throw new IllegalArgumentException("Descriptor global requires a descriptor location: " + location);
        }
        if (descriptorCount < 1) {
            throw new IllegalArgumentException("Descriptor count must be at least 1: " + descriptorCount);
        }
        this.type = type;
        this.descriptorCount = descriptorCount;
    }

    public void set(int descriptor, Object resource) {
        set(descriptor, 0, resource);
    }

    public void set(int descriptor, Object key, Object resource) {
        getShader().setDescriptorResource(this, descriptor, key, resource);
    }

    public void set(Object key, Object resource) {
        set(0, key, resource);
    }

    public void set(Object resource) {
        set(0, 0, resource);
    }

    public void setFrom(int descriptor, int node, Property<?> property) {
        set(descriptor, node, property.get(node));
    }

    public void setFrom(int node, Property<?> property) {
        set(0, node, property.get(node));
    }

    /**
     * Makes this a dynamic uniform or storage buffer descriptor. Has no effect on other types.
     */
    public void makeDynamic() {
        if (type == DescriptorType.UniformBuffer) {
            type = DescriptorType.UniformBufferDynamic;
        } else if (type == DescriptorType.StorageBuffer) {
            type = DescriptorType.StorageBufferDynamic;
        }
    }

    /**
     * Gets the logic that turns the resource mapped to this global into the artifact
     * written to its descriptors.
     *
     * @return binding logic for this global's descriptor type
     * @throws UnsupportedOperationException if this global's type has no logic yet
     */
    public BindingLogic getBindingLogic() {
        switch (type) {
            case UniformBuffer:
            case UniformBufferDynamic:
            case StorageBuffer:
            case StorageBufferDynamic:
                return BindingLogic.BUFFER;
            case CombinedImageSampler:
                return BindingLogic.TEXTURE;
        }
        throw new UnsupportedOperationException("No binding logic for descriptor type " + type + ".");
    }

    public DescriptorType getType() {
        return type;
    }

    public int getDescriptorCount() {
        return descriptorCount;
    }

    /**
     * @return descriptor set this global is bound in
     */
    public int getSet() {
        return getKey().getSet();
    }

    /**
     * @return binding this global occupies within its descriptor set
     */
    public int getBinding() {
        return getKey().getBinding();
    }

}
