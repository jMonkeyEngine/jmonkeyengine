package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;

import static org.lwjgl.vulkan.VK10.*;

public interface VulkanBuffer extends GpuBuffer<Long> {

    LogicalDevice<?> getDevice();

    Flag<BufferUsage> getUsage();

    Flag<MemoryProp> getMemoryProperties();

    boolean isConcurrent();

    default void recordCopy(MemoryStack stack, CommandBuffer cmd, VulkanBuffer source,
                            long srcOffset, long dstOffset, long size) {
        VkBufferCopy.Buffer copy = VkBufferCopy.calloc(1, stack)
                .srcOffset(srcOffset)
                .dstOffset(dstOffset)
                .size(size);
        vkCmdCopyBuffer(cmd.getBuffer(), source.getGpuObject(), getGpuObject(), copy);
    }

}
