package com.jme3.vulkan.buffers;

import com.jme3.vulkan.buffers.generate.BufferGenerator;
import com.jme3.vulkan.frames.VersionedResource;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.meshnew.AccessRate;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

public class AdaptiveBuffer implements GpuBuffer {

    private final BufferGenerator<?> generator;
    private final Flag<BufferUsage> usage;
    private GpuBuffer buffer;
    private AccessRate access;

    public AdaptiveBuffer(MemorySize size, Flag<BufferUsage> usage, BufferGenerator<?> generator) {
        this(size, usage, AccessRate.Static, generator);
    }

    public AdaptiveBuffer(MemorySize size, Flag<BufferUsage> usage, AccessRate access, BufferGenerator<?> generator) {
        this.generator = generator;
        this.usage = usage;
        this.access = access;
        this.buffer = generator.createBuffer(size, usage, access);
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return buffer.map(offset, size);
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public long getId() {
        return buffer.getId();
    }

    @Override
    public void resize(int elements) {
        buffer.resize(elements);
    }

    public void setAccessFrequency(AccessRate access) {
        if (this.access.ordinal() > access.ordinal()) {
            this.access = access;
            GpuBuffer newBuffer = generator.createBuffer(buffer.size(), usage, access);
            newBuffer.copy(buffer);
            buffer = newBuffer;
        }
    }

    public GpuBuffer getBuffer() {
        return buffer;
    }

    public AccessRate getAccessFrequency() {
        return access;
    }

}
