package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

public class VersionedBuffer implements GpuBuffer {

    private final GpuBuffer[] buffers;
    private int version = 0;
    private boolean mapped = false;

    public VersionedBuffer(GpuBuffer... buffers) {
        assert buffers.length > 0;
        this.buffers = buffers;
        MemorySize standard = buffers[0].size();
        for (int i = 1; i < buffers.length; i++) {
            if (!buffers[i].size().equals(standard)) {
                throw new IllegalArgumentException("All buffers must be of equivalent size.");
            }
        }
    }

    @Override
    public PointerBuffer map(MemoryStack stack, int offset, int size, int flags) {
        mapped = true;
        return buffers[version].map(stack, offset, size, flags);
    }

    @Override
    public void unmap() {
        mapped = false;
        buffers[version].unmap();
    }

    @Override
    public void freeMemory() {
        for (GpuBuffer buf : buffers) {
            buf.freeMemory();
        }
    }

    @Override
    public MemorySize size() {
        return buffers[version].size();
    }

    @Override
    public long getId() {
        return buffers[version].getId();
    }

    public void setVersion(int version) {
        if (mapped) {
            throw new IllegalStateException("Cannot change version while buffer is mapped.");
        }
        if (version < 0 || version >= buffers.length) {
            throw new IndexOutOfBoundsException("Version must be between 0 (inclusive) and the number of internal buffers (exclusive).");
        }
        this.version = version;
    }

    public GpuBuffer[] getInternalBuffers() {
        return buffers;
    }

    public int getVersion() {
        return version;
    }

}
