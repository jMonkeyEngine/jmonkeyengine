package com.jme3.vulkan.buffers.mapping;

import java.nio.*;

public class DirectBufferMapping implements BufferMapping {

    private final ByteBuffer bytes;

    public DirectBufferMapping(ByteBuffer bytes) {
        this.bytes = bytes;
    }

    @Override
    public void close() {}

    @Override
    public void stage(long offset, long size) {}

    @Override
    public ByteBuffer getBytes() {
        return bytes;
    }

}
