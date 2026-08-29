package com.jme3.vulkan.alloc;

import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface BufferDescription {

    /**
     * Binds this description to {@code buffer} so that it reads and writes
     * from {@code buffer} starting at {@code baseOffset} plus this description's
     * internal byte offset (if any).
     *
     * <p>If {@code buffer} is null, this description is unbound and not rebound
     * to any buffer.</p>
     *
     * @param buffer buffer to bind to, or null
     * @param baseOffset base offset in bytes
     */
    void bind(@Nullable EngineBuffer buffer, int baseOffset);

    /**
     * Unbinds this description from the buffer it is bound to.
     */
    default void unbind() {
        bind(null, 0);
    }

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
     * Gets the described buffer.
     *
     * @return
     */
    EngineBuffer getBuffer();

    /**
     * Gets the in bytes offset to the start of the described region from the start
     * of the buffer.
     *
     * @return offset in bytes
     */
    int offset();

    /**
     * Gets the number of bytes described.
     *
     * @return bytes described
     */
    int size();

}
