package com.jme3.vulkan.buffers.mapping;

import java.nio.*;

public class VirtualBufferMapping implements BufferMapping {

    private final BufferMapping source;

    public VirtualBufferMapping(BufferMapping source) {
        this.source = source;
    }

    @Override
    public void close() {}

    @Override
    public void stage(long offset, long size) {
        source.stage(offset, size);
    }

    @Override
    public ByteBuffer getBytes() {
        return source.getBytes();
    }

}
