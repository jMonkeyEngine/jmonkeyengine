package com.jme3.vulkan.buffers.stream;

import com.jme3.vulkan.buffers.MappableBuffer;

public interface Updateable extends MappableBuffer {

    DirtyRegions getUpdateRegions();

}
