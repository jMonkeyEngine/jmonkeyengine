package com.jme3.vulkan.buffers;

import com.jme3.vulkan.buffers.stream.DirtyRegions;

public abstract class AbstractGpuBuffer implements GpuBuffer {

    protected final DirtyRegions updateRegions = new DirtyRegions();

    @Override
    public void update(int offset, int size) {
        updateRegions.add(offset, size);
    }

    @Override
    public DirtyRegions getUpdateRegions() {
        return updateRegions;
    }

}
