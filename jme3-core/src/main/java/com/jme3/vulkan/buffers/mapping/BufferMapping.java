package com.jme3.vulkan.buffers.mapping;

import java.nio.*;

public interface BufferMapping extends AutoCloseable {

    @Override
    void close();

    void stage(long offset, long size);

    ByteBuffer getBytes();

    default void stage() {
        stage(0, getBytes().capacity());
    }

}
