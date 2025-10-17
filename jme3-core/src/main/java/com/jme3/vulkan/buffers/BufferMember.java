package com.jme3.vulkan.buffers;

import java.nio.ByteBuffer;

public interface BufferMember {

    /**
     * Fills {@link #getSizeInBytes()} bytes of {@code buffer},
     * starting from the buffer's current position and incrementing
     * the position by {@link #getSizeInBytes()} bytes.
     *
     * @param buffer buffer to fill
     */
    void fillBuffer(ByteBuffer buffer);

    /**
     * Gets the size of this member in bytes.
     *
     * @return size in bytes
     */
    int getSizeInBytes();

}
