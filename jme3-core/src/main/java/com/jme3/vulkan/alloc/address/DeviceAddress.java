package com.jme3.vulkan.alloc.address;

import com.jme3.vulkan.buffer.Handle;

@Deprecated
public interface DeviceAddress <T> extends Address {

    Handle<T> handle();

    int offset();

}
