package com.jme3.vulkan.buffers.stream;

import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.buffers.VulkanBuffer;

public interface Streamable extends MappableBuffer, Updateable {

    VulkanBuffer getDstBuffer();

}
