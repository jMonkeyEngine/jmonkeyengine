package com.jme3.vulkan.buffers.generate;

import com.jme3.vulkan.buffers.NioBuffer;
import com.jme3.vulkan.buffers.BufferUsage;
import com.jme3.vulkan.memory.MemorySize;
import com.jme3.vulkan.mesh.DataAccess;
import com.jme3.vulkan.util.Flag;

public class NioBufferGenerator implements BufferGenerator<NioBuffer> {

    @Override
    public NioBuffer createBuffer(MemorySize size, Flag<BufferUsage> usage, DataAccess access) {
        return new NioBuffer(size);
    }

}
