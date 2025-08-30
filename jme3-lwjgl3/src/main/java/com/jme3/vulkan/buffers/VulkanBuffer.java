package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;

import static org.lwjgl.vulkan.VK10.*;

public interface VulkanBuffer extends GpuBuffer {

    LogicalDevice<?> getDevice();

    default void recordCopy(MemoryStack stack, CommandBuffer commands, GpuBuffer source,
                           long srcOffset, long dstOffset, long size) {
        VkBufferCopy.Buffer copy = VkBufferCopy.calloc(1, stack)
                .srcOffset(srcOffset)
                .dstOffset(dstOffset)
                .size(size);
        vkCmdCopyBuffer(commands.getBuffer(), source.getId(), getId(), copy);
    }

}
