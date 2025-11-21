package com.jme3.vulkan.mesh;

import com.jme3.dev.NotFullyImplemented;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;

@NotFullyImplemented
public class LodBuffer implements GpuBuffer, Comparable<LodBuffer> {

    private final GpuBuffer buffer;
    private final float optimalDistance;

    public LodBuffer(GpuBuffer buffer, float optimalDistance) {
        this.buffer = buffer;
        this.optimalDistance = optimalDistance;
    }

    @Override
    public PointerBuffer map(int offset, int size) {
        return buffer.map(offset, size);
    }

    @Override
    public long getId() {
        return buffer.getId();
    }

    @Override
    public boolean resize(MemorySize size) {
        return buffer.resize(size);
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public void unmap() {
        buffer.unmap();
    }

    @Override
    public int compareTo(LodBuffer o) {
        return Float.compare(optimalDistance, o.optimalDistance);
    }

    public float getOptimalDistance() {
        return optimalDistance;
    }

}
