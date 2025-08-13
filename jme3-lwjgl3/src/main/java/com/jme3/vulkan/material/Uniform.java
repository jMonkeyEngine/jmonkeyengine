package com.jme3.vulkan.material;

import com.jme3.vulkan.descriptors.Descriptor;
import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public abstract class Uniform <T> implements DescriptorSetWriter {

    private final String name;
    private final Descriptor type;
    private final int setIndex;
    private final int bindingIndex;
    private SetLayoutBinding binding;
    private boolean updateFlag = true;
    protected T value;

    public Uniform(String name, Descriptor type, int setIndex, int bindingIndex) {
        this.name = name;
        this.type = type;
        this.setIndex = setIndex;
        this.bindingIndex = bindingIndex;
    }

    @Override
    public void populateWrite(MemoryStack stack, VkWriteDescriptorSet write) {
        write.descriptorType(type.getVkEnum())
                .dstBinding(binding.getBinding())
                .dstArrayElement(0)
                .descriptorCount(1);
    }

    @Override
    public boolean isUpdateNeeded() {
        return updateFlag;
    }

    public boolean isBindingCompatible(SetLayoutBinding binding) {
        return binding.getType() == type && binding.getBinding() == bindingIndex;
    }

    public void setBinding(SetLayoutBinding binding) {
        if (!isBindingCompatible(binding)) {
            throw new IllegalArgumentException("Incompatible binding provided.");
        }
        this.binding = binding;
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

    public String getName() {
        return name;
    }

    public Descriptor getType() {
        return type;
    }

    public SetLayoutBinding getBinding() {
        return binding;
    }

    public int getSetIndex() {
        return setIndex;
    }

    public int getBindingIndex() {
        return bindingIndex;
    }

    public T getValue() {
        return value;
    }

}
