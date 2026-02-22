package com.jme3.vulkan.buffers;

import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

public interface VulkanBuffer extends MappableBuffer {

    void upload(CommandBuffer cmd, BufferStream stream);

    long getBufferId(LogicalDevice<?> device);

    Flag<BufferUsage> getUsage();

    Flag<MemoryProp> getMemoryProperties();

    IntEnum<SharingMode> getSharingMode();

}
