package com.jme3.vulkan.buffer.alloc;

import com.jme3.vulkan.buffer.GpuBuffer;

public interface BufferPool {

    GpuBuffer alloc(int bytes);

}
