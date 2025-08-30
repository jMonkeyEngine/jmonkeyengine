package com.jme3.vulkan.mesh;

import com.jme3.vulkan.buffers.GpuBuffer;

public interface Mesh {

    GpuBuffer getBindingBuffer(int binding);

}
