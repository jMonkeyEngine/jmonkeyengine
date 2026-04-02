package com.jme3.vulkan.buffers.stream;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.buffers.newbuf.DeviceLocalBuffer;
import com.jme3.vulkan.buffers.saving.SavableBufferWrapper;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;
import com.jme3.vulkan.util.IntEnum;

import java.util.function.Consumer;

public class StreamingBuffer extends NioBuffer implements VulkanBuffer {

    private final DeviceLocalBuffer gpuBuffer;
    private final BufferTracker tracker = new BufferTracker();

    public StreamingBuffer(long bytes, Consumer<DeviceLocalBuffer.Builder> config) {
        this(bytes, true, config);
    }

    public StreamingBuffer(long bytes, boolean clearMem, Consumer<DeviceLocalBuffer.Builder> config) {
        super(bytes, clearMem);
        gpuBuffer = DeviceLocalBuffer.build(bytes, b -> {
            config.accept(b);
            b.setUsage(b.getUsage().add(BufferUsage.TransferDst));
        });
    }

    @Override
    public void upload(CommandBuffer cmd, BufferStream stream) {
        gpuBuffer.upload(cmd, stream);
        stream.stream(cmd, this, gpuBuffer, tracker);
    }

    @Override
    public void resize(long bytes) {
        gpuBuffer.resize(bytes);
        super.resize(bytes);
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
        tracker.add(offset, size);
    }

}
