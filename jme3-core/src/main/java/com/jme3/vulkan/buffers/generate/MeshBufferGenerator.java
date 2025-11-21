package com.jme3.vulkan.buffers.generate;

import com.jme3.vulkan.buffers.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.frames.UpdateFrameManager;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.DataAccess;
import com.jme3.vulkan.update.CommandBatch;
import com.jme3.vulkan.util.Flag;

public class MeshBufferGenerator implements BufferGenerator<GpuBuffer> {

    private final LogicalDevice<?> device;
    private final UpdateFrameManager<?> frames;
    private final CommandBatch dynamicBatch, sharedBatch;

    public MeshBufferGenerator(LogicalDevice<?> device, UpdateFrameManager<?> frames) {
        this(device, frames, null, null);
    }

    public MeshBufferGenerator(LogicalDevice<?> device, UpdateFrameManager<?> frames,
                               CommandBatch dynamicBatch, CommandBatch sharedBatch) {
        this.device = device;
        this.frames = frames;
        this.dynamicBatch = dynamicBatch;
        this.sharedBatch = sharedBatch;
    }

    @Override
    public GpuBuffer createBuffer(MemorySize size, Flag<BufferUsage> usage, DataAccess access) {
        switch (access) {
            case Stream: return createStreamingBuffer(size, usage);
            case Dynamic: return createDynamicBuffer(size, usage);
            case Static: return createStaticBuffer(size, usage);
            default: throw new UnsupportedOperationException("Unable to create \"" + access + "\" buffer.");
        }
    }

    public GpuBuffer createStreamingBuffer(MemorySize size, Flag<BufferUsage> usage) {
        PerFrameBuffer<OldPersistentBuffer> buffer = new PerFrameBuffer<>(frames, size,
                s -> new OldPersistentBuffer(device, s));
        for (OldPersistentBuffer buf : buffer) {
            try (OldPersistentBuffer.Builder b = buf.build()) {
                b.setUsage(usage);
            }
        }
        return buffer;
    }

    public GpuBuffer createDynamicBuffer(MemorySize size, Flag<BufferUsage> usage) {
        if (dynamicBatch == null) {
            throw new UnsupportedOperationException("Cannot create dynamic buffer: dynamic batch is null.");
        }
        PerFrameBuffer<StageableBuffer> buffer = new PerFrameBuffer<>(frames, size,
                s -> new StageableBuffer(device, s));
        for (StageableBuffer buf : buffer) {
            try (BasicVulkanBuffer.Builder b = buf.build()) {
                b.setUsage(usage);
                b.setMemFlags(MemoryProp.DeviceLocal);
            }
        }
        return dynamicBatch.addAll(buffer);
    }

    public GpuBuffer createStaticBuffer(MemorySize size, Flag<BufferUsage> usage) {
        if (sharedBatch == null) {
            throw new UnsupportedOperationException("Cannot create static buffer: shared batch is null.");
        }
        BackedStaticBuffer buffer = new BackedStaticBuffer(device, size);
        try (BasicVulkanBuffer.Builder b = buffer.build()) {
            b.setUsage(usage);
            b.setMemFlags(MemoryProp.DeviceLocal);
        }
        return sharedBatch.add(buffer);
    }

}
