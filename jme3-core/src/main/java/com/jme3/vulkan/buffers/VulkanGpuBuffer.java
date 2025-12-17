package com.jme3.vulkan.buffers;

import com.jme3.vulkan.buffers.stream.BufferStream;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;

import java.util.function.Consumer;

public class VulkanGpuBuffer implements VulkanBuffer {

    private final LogicalDevice<?> device;
    private final BufferStream stream;
    private final NioBuffer cpuBuffer;
    private VulkanBuffer gpuBuffer;
    private MemorySize size;
    private Flag<BufferUsage> usage = BufferUsage.Storage;
    private boolean concurrent = false;

    public VulkanGpuBuffer(LogicalDevice<?> device, BufferStream stream, MemorySize size) {
        this.device = device;
        this.stream = stream;
        this.size = size;
        this.cpuBuffer = new NioBuffer(size);
    }

    @Override
    public LogicalDevice<?> getDevice() {
        return device;
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return usage;
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return gpuBuffer.getMemoryProperties();
    }

    @Override
    public boolean isConcurrent() {
        return concurrent;
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return cpuBuffer.map(offset, size);
    }

    @Override
    public boolean resize(MemorySize size) {
        this.size = size;
        return cpuBuffer.resize(size) | gpuBuffer.resize(size);
    }

    @Override
    public long getId() {
        return gpuBuffer.getId();
    }

    @Override
    public MemorySize size() {
        return size;
    }

    @Override
    public void unmap() {
        cpuBuffer.unmap();
    }

    @Override
    public void update(int offset, int size) {
        cpuBuffer.update(offset, size);
    }

    public static VulkanGpuBuffer build(LogicalDevice<?> device, BufferStream stream, MemorySize size, Consumer<Builder> config) {
        Builder b = new VulkanGpuBuffer(device, stream, size).new Builder();
        config.accept(b);
        return b.build();
    }

    public class Builder {

        public VulkanGpuBuffer build() {
            usage = usage.add(BufferUsage.TransferDst);
            gpuBuffer = BasicVulkanBuffer.build(device, size, b -> {
                b.setUsage(usage);
                b.setMemFlags(MemoryProp.DeviceLocal);
                b.setConcurrent(concurrent);
            });
            return VulkanGpuBuffer.this;
        }

        public void setUsage(Flag<BufferUsage> usage) {
            VulkanGpuBuffer.this.usage = usage;
        }

        public void setConcurrent(boolean concurrent) {
            VulkanGpuBuffer.this.concurrent = concurrent;
        }

    }

}
