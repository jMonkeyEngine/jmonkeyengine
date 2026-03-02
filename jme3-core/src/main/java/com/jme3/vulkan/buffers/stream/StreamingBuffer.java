package com.jme3.vulkan.buffers.stream;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.newbuf.DeviceLocalBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.util.function.Consumer;

public class StreamingBuffer extends NioBuffer implements VulkanBuffer {

    private final DeviceLocalBuffer gpuBuffer;
    private final DirtyRegions regions = new DirtyRegions();

    public StreamingBuffer(MemorySize size, Consumer<DeviceLocalBuffer.Builder> config) {
        this(size, true, config);
    }

    public StreamingBuffer(MemorySize size, boolean clearMem, Consumer<DeviceLocalBuffer.Builder> config) {
        super(size, clearMem);
        gpuBuffer = DeviceLocalBuffer.build(size, b -> {
            config.accept(b);
            b.setUsage(b.getUsage().add(BufferUsage.TransferDst));
        });
    }

    @Override
    public void upload(CommandBuffer cmd, BufferStream stream) {
        gpuBuffer.upload(cmd, stream);
        regions.optimize();
        stream.stream(cmd, this, gpuBuffer, regions);
    }

    @Override
    public ResizeResult resize(MemorySize size) {
        if (gpuBuffer.resize(size) == ResizeResult.DataLost) {
            regions.add(0, size.getBytes());
        }
        return super.resize(size);
    }

    @Override
    public long getBufferId(LogicalDevice<?> device) {
        return gpuBuffer.getBufferId(device);
    }

    @Override
    public Flag<BufferUsage> getUsage() {
        return gpuBuffer.getUsage();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return gpuBuffer.getMemoryProperties();
    }

    @Override
    public IntEnum<SharingMode> getSharingMode() {
        return gpuBuffer.getSharingMode();
    }

    @Override
    public void stage(long offset, long size) {
        super.stage(offset, size);
        regions.add(offset, size);
    }

}
