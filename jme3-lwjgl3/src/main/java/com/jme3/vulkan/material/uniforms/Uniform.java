package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public abstract class Uniform<T> implements DescriptorSetWriter {

    private final Descriptor type;
    private final int bindingIndex;
    private boolean updateFlag = true;
    protected T value;

    public Uniform(Descriptor type, int bindingIndex) {
        this.type = type;
        this.bindingIndex = bindingIndex;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorType(type.getVkEnum())
                .dstBinding(bindingIndex)
                .dstArrayElement(0)
                .descriptorCount(1);
    }

    @Override
    public boolean isUpdateNeeded() {
        return updateFlag;
    }

    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return binding.getType() == type
                && binding.getBinding() == bindingIndex
                && binding.getDescriptors() == 1;
    }

    public void setValue(T value) {
        if (this.value != value) {
            setUpdateNeeded();
        }
        this.value = value;
    }

    public void setUpdateNeeded() {
        updateFlag = true;
    }

    public void clearUpdateNeeded() {
        updateFlag = false;
    }

    public Descriptor getType() {
        return type;
    }

    public int getBindingIndex() {
        return bindingIndex;
    }

    public T getValue() {
        return value;
    }

}
