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
import java.util.function.Function;
import java.util.function.LongFunction;

public interface GpuBuffer {

    /**
     * Maps the memory of this buffer and returns a pointer to the mapped
     * region. If the memory is currently mapped when this is called, an
     * exception is thrown.
     *
     * @param offset offset, in bytes, of the mapping
     * @param size size, in bytes, of the mapping
     * @return Buffer containing a pointer to the mapped memory
     */
    PointerBuffer map(int offset, int size);

    /**
     * Unmaps the memory of this buffer. If this buffer is not currently
     * {@link #map(int, int) mapped} when this method is called, an
     * exception is thrown.
     */
    void unmap();

    /**
     * Gets the functional size of this buffer.
     *
     * @return size
     */
    MemorySize size();

    /**
     * Gets the native buffer ID.
     *
     * @return buffer ID
     */
    long getId();

    /**
     * Resizes this buffer to accomodate the given number of elements.
     *
     * @param elements number of elements this buffer must be able to store (not negative)
     */
    void resize(int elements);

    default void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size().getBytes()) {
            throw new BufferOverflowException();
        }
    }

    default PointerBuffer map(int offset) {
        return map(offset, size().getBytes() - offset);
    }

    default PointerBuffer map() {
        return map(0, size().getBytes());
    }

    default <T> T map(int offset, int size, Function<PointerBuffer, T> factory) {
        return factory.apply(map(offset, size));
    }

    default <T> T map(int offset, Function<PointerBuffer, T> factory) {
        return factory.apply(map(offset, size().getBytes() - offset));
    }

    default <T> T map(Function<PointerBuffer, T> factory) {
        return factory.apply(map(0, size().getBytes()));
    }

    default <T> T map(int offset, int size, LongFunction<T> factory) {
        return factory.apply(map(offset, size).get(0));
    }

    default <T> T map(int offset, LongFunction<T> factory) {
        return factory.apply(map(offset, size().getBytes() - offset).get(0));
    }

    default <T> T map(LongFunction<T> factory) {
        return factory.apply(map(0, size().getBytes()).get(0));
    }

    default ByteBuffer mapBytes(int offset, int size) {
        return map(offset * Byte.BYTES, size * Byte.BYTES).getByteBuffer(0, size);
    }

    default ByteBuffer mapBytes(int offset) {
        return mapBytes(offset, size().getBytes() - offset);
    }

    default ByteBuffer mapBytes() {
        return mapBytes(0, size().getBytes());
    }

    default ShortBuffer mapShorts(int offset, int size) {
        return map(offset * Short.BYTES, size * Short.BYTES).getShortBuffer(0, size);
    }

    default ShortBuffer mapShorts(int offset) {
        return mapShorts(offset, size().getShorts() - offset);
    }

    default ShortBuffer mapShorts() {
        return mapShorts(0, size().getShorts());
    }

    default IntBuffer mapInts(int offset, int size) {
        return map(offset * Integer.BYTES, size * Integer.BYTES).getIntBuffer(0, size);
    }

    default IntBuffer mapInts(int offset) {
        return mapInts(offset, size().getInts() - offset);
    }

    default IntBuffer mapInts() {
        return mapInts(0, size().getInts());
    }

    default FloatBuffer mapFloats(int offset, int size) {
        return map(offset * Float.BYTES, size * Float.BYTES).getFloatBuffer(0, size);
    }

    default FloatBuffer mapFloats(int offset) {
        return mapFloats(offset, size().getFloats() - offset);
    }

    default FloatBuffer mapFloats() {
        return mapFloats(0, size().getFloats());
    }

    default DoubleBuffer mapDoubles(int offset, int size) {
        return map(offset * Double.BYTES, size * Double.BYTES).getDoubleBuffer(0, size);
    }

    default DoubleBuffer mapDoubles(int offset) {
        return mapDoubles(offset, size().getDoubles() - offset);
    }

    default DoubleBuffer mapDoubles() {
        return mapDoubles(0, size().getDoubles());
    }

    default LongBuffer mapLongs(int offset, int size) {
        return map(offset * Long.BYTES, size * Long.BYTES).getLongBuffer(0, size);
    }

    default LongBuffer mapLongs(int offset) {
        return mapLongs(offset, size().getLongs() - offset);
    }

    default LongBuffer mapLongs() {
        return mapLongs(0, size().getLongs());
    }

    default IndexBuffer mapIndices(int offset, int size) {
        switch (size().getBytesPerElement()) {
            case Byte.BYTES: return new IndexByteBuffer(mapBytes(offset, size));
            case Short.BYTES: return new IndexShortBuffer(mapShorts(offset, size));
            case Integer.BYTES: return new IndexIntBuffer(mapInts(offset, size));
            default: throw new UnsupportedOperationException("Unable to map to index buffer with "
                    + size().getBytesPerElement() + " bytes per element.");
        }
    }

    default IndexBuffer mapIndices(int offset) {
        switch (size().getBytesPerElement()) {
            case Byte.BYTES: return new IndexByteBuffer(mapBytes(offset));
            case Short.BYTES: return new IndexShortBuffer(mapShorts(offset));
            case Integer.BYTES: return new IndexIntBuffer(mapInts(offset));
            default: throw new UnsupportedOperationException("Unable to map to index buffer with "
                    + size().getBytesPerElement() + " bytes per element.");
        }
    }

    default IndexBuffer mapIndices() {
        switch (size().getBytesPerElement()) {
            case Byte.BYTES: return new IndexByteBuffer(mapBytes());
            case Short.BYTES: return new IndexShortBuffer(mapShorts());
            case Integer.BYTES: return new IndexIntBuffer(mapInts());
            default: throw new UnsupportedOperationException("Unable to map to index buffer with "
                    + size().getBytesPerElement() + " bytes per element.");
        }
    }

    default void copy(GpuBuffer buffer) {
        verifyBufferSize(buffer.size().getBytes(), Byte.BYTES);
        MemoryUtil.memCopy(buffer.mapBytes(), mapBytes(0, buffer.size().getBytes()));
        buffer.unmap();
        unmap();
    }

    default void copy(ByteBuffer buffer) {
        verifyBufferSize(buffer.limit(), Byte.BYTES);
        MemoryUtil.memCopy(buffer, mapBytes(0, buffer.limit()));
        unmap();
    }

    default void copy(ShortBuffer buffer) {
        verifyBufferSize(buffer.limit(), Short.BYTES);
        MemoryUtil.memCopy(buffer, mapShorts(0, buffer.limit()));
        unmap();
    }

    default void copy(IntBuffer buffer) {
        verifyBufferSize(buffer.limit(), Integer.BYTES);
        MemoryUtil.memCopy(buffer, mapInts(0, buffer.limit()));
        unmap();
    }

    default void copy(FloatBuffer buffer) {
        verifyBufferSize(buffer.limit(), Float.BYTES);
        MemoryUtil.memCopy(buffer, mapFloats(0, buffer.limit()));
        unmap();
    }

    default void copy(DoubleBuffer buffer) {
        verifyBufferSize(buffer.limit(), Double.BYTES);
        MemoryUtil.memCopy(buffer, mapDoubles(0, buffer.limit()));
        unmap();
    }

    default void copy(LongBuffer buffer) {
        verifyBufferSize(buffer.limit(), Long.BYTES);
        MemoryUtil.memCopy(buffer, mapLongs(0, buffer.limit()));
        unmap();
    }

    default void copy(Struct<?> struct) {
        verifyBufferSize(struct.sizeof(), Byte.BYTES);
        MemoryUtil.memCopy(MemoryUtil.memByteBuffer(struct.address(), struct.sizeof()), mapBytes(0, struct.sizeof()));
        unmap();
    }

    default void copy(StructBuffer<?, ?> buffer) {
        verifyBufferSize(buffer.limit(), buffer.sizeof());
        int size = buffer.limit() * buffer.sizeof();
        MemoryUtil.memCopy(MemoryUtil.memByteBuffer(buffer.address(), size), mapBytes(0, size));
        unmap();
    }

}
