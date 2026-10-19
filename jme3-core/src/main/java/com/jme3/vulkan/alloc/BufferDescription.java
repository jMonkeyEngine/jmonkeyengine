package com.jme3.vulkan.alloc;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;

public interface BufferDescription {

    /**
     * Binds this description to {@code buffer} so that subsequent operations
     * interact with {@code buffer}. Any previous binding will be broken.
     *
     * @param buffer buffer to bind to (not null)
     * @param offset offset in bytes
     */
    void bind(GpuBuffer buffer, int offset);

    /**
     * Breaks any current buffer binding and does not replace it.
     */
    void unbind();

    /**
     * Retrieves the cache of the buffer this description is bound to with
     * {@link DataBuffer#position() position} set to this description's byte offset
     * from the start of {@code buffer} and {@link DataBuffer#limit() limit} set
     * to {@link DataBuffer#position()} plus this description's byte size.
     *
     * @return cache buffer
     */
    DataBuffer cache();

    /**
     * Gets the number of bytes described by this buffer description.
     *
     * @return size in bytes
     */
    int size();

}
