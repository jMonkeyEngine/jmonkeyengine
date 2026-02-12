package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

public class LodBuffer implements MappableBuffer {

    private final MappableBuffer buffer;
    private final float optimalDistance;

    public LodBuffer(MappableBuffer buffer) {
        this(buffer, 0f);
    }

    public LodBuffer(MappableBuffer buffer, float optimalDistance) {
        this.buffer = buffer;
        this.optimalDistance = optimalDistance;
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return buffer.map(offset, size);
    }

    @Override
    public void push(int offset, int size) {
        buffer.push(offset, size);
    }

    @Override
    public long getId() {
        return buffer.getId();
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    public float getOptimalDistance() {
        return optimalDistance;
    }

}
