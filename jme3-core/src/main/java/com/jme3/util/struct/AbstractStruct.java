package com.jme3.util.struct;

import com.jme3.vulkan.buffers.MappableBuffer;

public abstract class AbstractStruct implements Struct {

    protected MappableBuffer buffer;

    @Override
    public MappableBuffer getBuffer() {
        return buffer;
    }

}
