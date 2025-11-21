package com.jme3.vulkan.buffers.generate;

import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.DataAccess;
import com.jme3.vulkan.util.Flag;

public interface BufferGenerator <T extends GpuBuffer> {

    T createBuffer(MemorySize size, Flag<BufferUsage> usage, DataAccess access);

}
