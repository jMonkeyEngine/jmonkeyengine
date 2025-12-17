package com.jme3.vulkan.buffers.stream;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.buffers.VulkanBuffer;

public interface Streamable extends GpuBuffer {

    VulkanBuffer getDstBuffer();

    DirtyRegions getUpdateRegions();

}
