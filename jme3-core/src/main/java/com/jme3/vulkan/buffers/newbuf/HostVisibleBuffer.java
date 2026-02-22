package com.jme3.vulkan.buffers.newbuf;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.SourceBufferMapping;
import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class HostVisibleBuffer extends AbstractVulkanBuffer {

    protected HostVisibleBuffer(MemorySize size) {
        super(size);
    }

    @Override
    public void stage(long offset, long size) {}

    @Override
    public void upload(CommandBuffer cmd, BufferStream stream) {}

    @Override
    protected BufferMapping mapNative(BufferHandle handle, long offset, long size) {
        return new SourceBufferMapping(this, handle.getMemory().map(this.size.getOffset() + offset, size), size, () -> handle.getMemory().unmap());
    }

    @Override
    protected void moveToNewBuffer(BufferHandle oldHandle, BufferHandle newHandle) {
        long size = Math.min(oldHandle.getMemory().getSize(), newHandle.getMemory().getSize());
        try (BufferMapping srcMap = mapNative(oldHandle, 0, size); BufferMapping dstMap = mapNative(newHandle, 0, size)) {
            MemoryUtil.memCopy(srcMap.getBytes(), dstMap.getBytes());
        }
    }

    @Override
    protected void initialize(LogicalDevice<?> device) {
        memProps = memProps.add(MemoryProp.HostVisibleAndCoherent);
        super.initialize(device);
    }

    public static HostVisibleBuffer build(MemorySize size, Consumer<Builder> config) {
        HostVisibleBuffer b = new HostVisibleBuffer(size);
        config.accept(b.new Builder());
        return b;
    }

}
