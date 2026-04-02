package com.jme3.vulkan.buffers.alloc;

import com.jme3.export.Savable;
import com.jme3.vulkan.buffers.MappableBuffer;

public interface BufferAllocRequest <T extends MappableBuffer> extends Savable {

    T create(long bytes);

}
