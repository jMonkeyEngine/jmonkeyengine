package com.jme3.vulkan.buffers;

import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexByteBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongFunction;

public interface MappableBuffer extends Mappable {

    enum ResizeResult {

        /**
         * The buffer's size was changed without any side effects.
         */
        Success(true),

        /**
         * The buffer's size was changed but the buffer's data had to be copied
         * to a new memory region. For {@link GpuBuffer GpuBuffers} this often
         * means that a new native buffer of the correct size was created.
         */
        Realloc(true),

        /**
         * The buffer's size was changed by allocating a new memory region, but
         * the buffer's data was lost in the process. This is often unavoidable for
         * certain types of buffers ({@link com.jme3.vulkan.buffers.newbuf.DeviceLocalBuffer
         * DeviceLocalBuffer} for example) where copying the current data to the
         * new region is not feasible.
         */
        DataLost(true),

        /**
         * The buffer was not resized.
         */
        Failure(false);

        private final boolean resized;

        ResizeResult(boolean resized) {
            this.resized = resized;
        }

        public boolean isResized() {
            return resized;
        }

    }

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
    void push(long offset, long size);

    /**
     * Resizes this buffer.
     *
     * @param size size to resize to
     * @return result of the resize operation
     */
    ResizeResult resize(MemorySize size);

    /**
     * Marks this entire buffer as needing to be pushed to the GPU or
     * another receiving buffer.
     *
     * @see #push(long, long)
     */
    default void push() {
        push(0, size().getBytes());
    }

    /**
     * Verifies that there are enough bytes in the buffer to represent {@code elements}
     * with {@code bytesPerElement}.
     *
     * @param elements number of elements
     * @param bytesPerElement number of bytes needed per element
     * @throws BufferOverflowException if this buffer is not large enough
     */
    default void verifyBufferSize(long elements, int bytesPerElement) {
        if (elements * bytesPerElement > size().getBytes()) {
            throw new BufferOverflowException();
        }
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
    @Override
    default BufferMapping map() {
        return map(0, size().getBytes());
    }

    /**
     * Maps indices as an IndexBuffer from {@code offset} through {@code size}.
     *
     * <p>This buffer must have {@link Byte#BYTES}, {@link Short#BYTES}, or {@link Integer#BYTES} number
     * of bytes per element to map to either {@link IndexByteBuffer}, {@link IndexShortBuffer}, or
     * {@link IndexIntBuffer}, respectively. For better results, use {@link #mapInts()} or another
     * similar method that maps directly to a primitive buffer.</p>
     *
     * @param offset offset in elements
     * @param size size in elements
     * @return mapped region as an index buffer
     * @throws UnsupportedOperationException if the number of bytes per element of this buffer
     * does not correspond to a byte, short, or integer
     * @see #map(long, long)
     */
    default IndexBuffer mapIndices(long offset, long size) {
        switch (size().getBytesPerElement()) {
            case Byte.BYTES: return new IndexByteBuffer(mapBytes(offset, size));
            case Short.BYTES: return new IndexShortBuffer(mapShorts(offset, size));
            case Integer.BYTES: return new IndexIntBuffer(mapInts(offset, size));
            default: throw new UnsupportedOperationException("Unable to map to index buffer with "
                    + size().getBytesPerElement() + " bytes per element.");
        }
    }

    /**
     * Maps indices as an IndexBuffer from {@code offset} to the end of the buffer.
     *
     * <p>This buffer must have {@link Byte#BYTES}, {@link Short#BYTES}, or {@link Integer#BYTES} number
     * of bytes per element to map to either {@link IndexByteBuffer}, {@link IndexShortBuffer}, or
     * {@link IndexIntBuffer}, respectively. For better results, use {@link #mapInts()} or another
     * similar method that maps directly to a primitive buffer.</p>
     *
     * @param offset offset in elements
     * @return mapped region as an index buffer
     * @throws UnsupportedOperationException if the number of bytes per element of this buffer
     * does not correspond to a byte, short, or integer
     * @see #map(long, long)
     */
    default IndexBuffer mapIndices(long offset) {
        switch (size().getBytesPerElement()) {
            case Byte.BYTES: return new IndexByteBuffer(mapBytes(offset));
            case Short.BYTES: return new IndexShortBuffer(mapShorts(offset));
            case Integer.BYTES: return new IndexIntBuffer(mapInts(offset));
            default: throw new UnsupportedOperationException("Unable to map to index buffer with "
                    + size().getBytesPerElement() + " bytes per element.");
        }
    }

    /**
     * Maps all indices of this buffer as an IndexBuffer.
     *
     * <p>This buffer must have {@link Byte#BYTES}, {@link Short#BYTES}, or {@link Integer#BYTES} number
     * of bytes per element to map to either {@link IndexByteBuffer}, {@link IndexShortBuffer}, or
     * {@link IndexIntBuffer}, respectively. For better results, use {@link #mapInts()} or another
     * similar method that maps directly to a primitive buffer.</p>
     *
     * @return mapped region as an index buffer
     * @throws UnsupportedOperationException if the number of bytes per element of this buffer
     * does not correspond to a byte, short, or integer
     * @see #map(long, long)
     */
    default IndexBuffer mapIndices() {
        switch (size().getBytesPerElement()) {
            case Byte.BYTES: return new IndexByteBuffer(mapBytes());
            case Short.BYTES: return new IndexShortBuffer(mapShorts());
            case Integer.BYTES: return new IndexIntBuffer(mapInts());
            default: throw new UnsupportedOperationException("Unable to map to index buffer with "
                    + size().getBytesPerElement() + " bytes per element.");
        }
    }

}
