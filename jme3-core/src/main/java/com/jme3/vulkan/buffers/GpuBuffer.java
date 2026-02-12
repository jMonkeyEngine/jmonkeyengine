package com.jme3.vulkan.buffers;

public interface GpuBuffer <T> extends MappableBuffer {

    T getGpuObject();

}
