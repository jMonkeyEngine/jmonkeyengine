package com.jme3.vulkan.buffers;

import com.jme3.export.Savable;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructMapping;
import com.jme3.vulkan.buffers.mapping.BufferMapping;
import com.jme3.vulkan.memory.MemorySize;

/**
 * @deprecated use {@link com.jme3.vulkan.buffernew.GpuBuffer} instead
 */
@Deprecated
public interface MappableBuffer extends Savable {

    /**
     * Maps the memory of this buffer and returns a pointer to the mapped
     * region. If the memory is currently mapped when this is called, an
     * exception is thrown.
     *
     * @param offset offset, in bytes, of the mapping
     * @param size size, in bytes, of the mapping
     * @return Buffer containing a pointer to the mapped memory
     * @throws IllegalStateException if this buffer is already mapped
     */
    BufferMapping map(long offset, long size);

    /**
     * Marks the region of this buffer as needing to be pushed to the GPU
     * or another receiving buffer.
     *
     * @param offset offset of the region in bytes
     * @param size size of the region in bytes
     */
    void stage(long offset, long size);

    /**
     * Flushes {@link #stage(long, long) staged} regions to the GPU.
     */
    void flush();

    /**
     * Resizes this buffer.
     *
     * @param bytes@return result of the resize operation
     */
    void resize(long bytes);

    /**
     * Returns the memory size of this buffer.
     *
     * @return memory size
     */
    MemorySize size();

    /**
     * Marks this entire buffer as needing to be pushed to the GPU or
     * another receiving buffer.
     *
     * @see #stage(long, long)
     */
    default void stage() {
        stage(0, size().getBytes());
    }

    /**
     * Maps bytes starting from the byte offset and extending to the end of the buffer.
     *
     * @param offset offset in bytes
     * @return pointer to the mapped region
     * @see #map(long, long)
     */
    default BufferMapping map(long offset) {
        return map(offset, size().getBytes() - offset);
    }

    /**
     * Maps all bytes in the buffer.
     *
     * @return pointer to the mapped region
     * @see #map(long, long)
     */
    default BufferMapping map() {
        return map(0, size().getBytes());
    }

    default <T extends Struct> StructMapping<T> mapStruct(T struct) {
        return new StructMapping<>(struct, this, 0, 1);
    }

    default <T extends Struct> StructMapping<T> mapStruct(T struct, int offset) {
        return new StructMapping<>(struct, this, offset, 1);
    }

    default <T extends Struct> StructMapping<T> mapStructs(T struct, int elements) {
        return new StructMapping<>(struct, this, 0, elements);
    }

    default <T extends Struct> StructMapping<T> mapStructs(T struct, int offset, int elements) {
        return new StructMapping<>(struct, this, offset, elements);
    }

    default <T extends Struct> StructMapping<T> mapAllStructs(T struct) {
        return new StructMapping<>(struct, this);
    }

    default <T extends Struct> StructMapping<T> mapAllStructs(T struct, int offset) {
        return new StructMapping<>(struct, this, offset);
    }

    default void resize(int bytes) {
        resize((long)bytes);
    }

    default void resizeUp(long bytes) {
        if (size().getBytes() < bytes) {
            resize(bytes);
        }
    }

    default void resizeUp(int bytes) {
        resizeUp((long)bytes);
    }

}
