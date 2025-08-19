package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;

public interface Uniform <T> extends DescriptorSetWriter {

    boolean update(LogicalDevice<?> device);

    boolean isBindingCompatible(SetLayoutBinding binding);

    SetLayoutBinding createBinding();

    void setValue(T value);

    T getValue();

    String getName();

    int getBindingIndex();

}
