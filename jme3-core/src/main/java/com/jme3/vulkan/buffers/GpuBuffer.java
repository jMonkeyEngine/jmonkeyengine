package com.jme3.vulkan.buffers;

@Deprecated
public interface GpuBuffer <T> extends MappableBuffer {

    T getGpuObject();

}
