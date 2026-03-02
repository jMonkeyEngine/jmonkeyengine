package com.jme3.vulkan.buffers;

import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexByteBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.vulkan.memory.MemorySize;

import java.nio.*;

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

        /**
         * Returns true if the buffer had to be resized to fit the new size.
         *
         * @return true if resized
         */
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
    void stage(long offset, long size);

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
     * @see #stage(long, long)
     */
    default void stage() {
        stage(0, size().getBytes());
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

}
