package com.jme3.vulkan.buffers;

import com.jme3.vulkan.memory.MemorySize;

public interface Mappable {

    BufferMapping map();

    MemorySize size();

}
