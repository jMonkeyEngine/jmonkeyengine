package com.jme3.vulkan.material.uniforms;

import com.jme3.vulkan.descriptors.DescriptorSetWriter;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;

public interface Uniform <T> extends DescriptorSetWriter {

    // true to trigger update of descriptor sets
    boolean update(LogicalDevice<?> device);

    void setValue(T value);

    T getValue();

    int getBindingIndex();

    boolean isBindingCompatible(SetLayoutBinding binding);

    String getName();

}
