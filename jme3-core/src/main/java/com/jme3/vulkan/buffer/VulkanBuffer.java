package com.jme3.vulkan.buffer;

import com.jme3.vulkan.util.Flag;

public interface VulkanBuffer extends DeviceBuffer {

    Flag<EngineBuffer.Role> getUsage();

}
