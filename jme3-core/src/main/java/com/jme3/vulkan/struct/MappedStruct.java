package com.jme3.vulkan.struct;

import com.jme3.vulkan.buffers.GpuBuffer;

public interface MappedStruct extends GpuBuffer {

    void set(String name, Object value);

    <T> T get(String name);

}
