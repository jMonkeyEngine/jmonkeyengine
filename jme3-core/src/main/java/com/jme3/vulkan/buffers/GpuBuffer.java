package com.jme3.vulkan.buffers;

import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexByteBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.vulkan.GpuResource;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;

import java.nio.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongFunction;

public interface GpuBuffer extends GpuResource, Mappable {

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
    PointerBuffer map(int offset, int size);

    /**
     * Marks the region of this buffer as needing to be pushed to the GPU
     * or another receiving buffer.
     *
     * @param offset offset of the region in bytes
     * @param size size of the region in bytes
     */
    void push(int offset, int size);

    /**
     * Marks this entire buffer as needing to be pushed to the GPU or
     * another receiving buffer.
     *
     * @see #push(int, int)
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
    default void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size().getBytes()) {
            throw new BufferOverflowException();
        }
    }

    /**
     * Maps bytes starting from the byte offset and extending to the end of the buffer.
     *
     * @param offset offset in bytes
     * @return pointer to the mapped region
     * @see #map(int, int)
     */
    default PointerBuffer map(int offset) {
        return map(offset, size().getBytes() - offset);
    }

    /**
     * Maps all bytes in the buffer.
     *
     * @return pointer to the mapped region
     * @see #map(int, int)
     */
    @Override
    default PointerBuffer map() {
        return map(0, size().getBytes());
    }

    /**
     * Maps bytes within the {@code offset} and {@code size} (in bytes) as an object.
     *
     * @param offset offset in bytes
     * @param size size in bytes
     * @param factory creates an object from the mapped pointer
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(int offset, int size, Function<PointerBuffer, T> factory) {
        return factory.apply(map(offset, size));
    }

    /**
     * Maps bytes starting from the offset and extending to the end of the buffer
     * as an object.
     *
     * @param offset offset in bytes
     * @param factory creates an object from the mapped pointer
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(int offset, Function<PointerBuffer, T> factory) {
        return factory.apply(map(offset, size().getBytes() - offset));
    }

    /**
     * Maps all bytes in the buffer as an object.
     *
     * @param factory creates an object from the mapped pointer
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(Function<PointerBuffer, T> factory) {
        return factory.apply(map(0, size().getBytes()));
    }

    /**
     * Maps bytes within the {@code offset} and {@code size} (in bytes) as an object.
     *
     * @param offset offset in bytes
     * @param size size in bytes
     * @param factory creates an object from the mapped pointer
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(int offset, int size, LongFunction<T> factory) {
        return factory.apply(map(offset, size).get(0));
    }

    /**
     * Maps bytes starting from the offset and extending to the end of the buffer
     * as an object.
     *
     * @param offset offset in bytes
     * @param factory creates an object from the mapped pointer
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(int offset, LongFunction<T> factory) {
        return factory.apply(map(offset, size().getBytes() - offset).get(0));
    }

    /**
     * Maps all bytes in the buffer as an object.
     *
     * @param factory creates an object from the mapped pointer
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(LongFunction<T> factory) {
        return factory.apply(map(0, size().getBytes()).get(0));
    }

    /**
     * Maps bytes within the {@code offset} and {@code size} (in elements) as an object.
     *
     * @param offset offset in bytes
     * @param size size in bytes
     * @param factory creates an object from the mapped pointer and the number of
     *                elements in the mapped region
     * @return mapped object
     * @param <T> mapped type
     * @see #map(int, int)
     */
    default <T> T map(int offset, int size, BiFunction<Long, Integer, T> factory) {
        offset *= size().getBytesPerElement();
        return factory.apply(map(offset, size * size().getBytesPerElement()).get(0), size);
    }

    /**
     * Maps bytes starting from the offset and extending to the end of the buffer
     * as an object.
     *
     * @param offset offset in elements
     * @param factory creates an object from the mapped pointer and the number of
     *                elements in the mapped region
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(int offset, BiFunction<Long, Integer, T> factory) {
        offset *= size().getBytesPerElement();
        int size = size().getBytes() - offset;
        return factory.apply(map(offset, size).get(0), size / size().getBytesPerElement());
    }

    /**
     * Maps all bytes in the buffer as an object.
     *
     * @param factory creates an object from the mapped pointer and the number of
     *                elements in the mapped region
     * @return mapped object
     * @param <T> mapping type
     * @see #map(int, int)
     */
    default <T> T map(BiFunction<Long, Integer, T> factory) {
        return factory.apply(map(0, size().getBytes()).get(0), size().getElements());
    }

    /**
     * Maps bytes within {@code offset} and {@code size}.
     *
     * @param offset offset in bytes
     * @param size size in bytes
     * @return mapped region as a byte buffer
     * @see #map(int, int)
     */
    default ByteBuffer mapBytes(int offset, int size) {
        return map(offset * Byte.BYTES, size * Byte.BYTES).getByteBuffer(0, size);
    }

    /**
     * Maps bytes from {@code offset} to the end of the buffer.
     *
     * @param offset offset in bytes
     * @return mapped region as a byte buffer
     * @see #map(int, int)
     */
    default ByteBuffer mapBytes(int offset) {
        return mapBytes(offset, size().getBytes() - offset);
    }

    /**
     * Maps all bytes in the buffer.
     *
     * @return mapped region as a byte buffer
     * @see #map(int, int)
     */
    @Override
    default ByteBuffer mapBytes() {
        return mapBytes(0, size().getBytes());
    }

    /**
     * Maps shorts within {@code offset} and {@code size}.
     *
     * @param offset offset in shorts
     * @param size size in shorts
     * @return mapped region as a short buffer
     * @see #map(int, int)
     */
    default ShortBuffer mapShorts(int offset, int size) {
        return map(offset * Short.BYTES, size * Short.BYTES).getShortBuffer(0, size);
    }

    /**
     * Maps shorts from {@code offset} to the end of the buffer.
     *
     * @param offset offset in shorts
     * @return mapped region as a short buffer
     * @see #map(int, int)
     */
    default ShortBuffer mapShorts(int offset) {
        return mapShorts(offset, size().getShorts() - offset);
    }

    /**
     * Maps all shorts in the buffer.
     *
     * @return mapped region as a short buffer
     * @see #map(int, int)
     */
    @Override
    default ShortBuffer mapShorts() {
        return mapShorts(0, size().getShorts());
    }

    /**
     * Maps ints within {@code offset} and {@code size}.
     *
     * @param offset offset in ints
     * @param size size in ints
     * @return mapped region as an int buffer
     * @see #map(int, int)
     */
    default IntBuffer mapInts(int offset, int size) {
        return map(offset * Integer.BYTES, size * Integer.BYTES).getIntBuffer(0, size);
    }

    /**
     * Maps ints from {@code offset} to the end of the buffer.
     *
     * @param offset offset in ints
     * @return mapped region as an int buffer
     * @see #map(int, int)
     */
    default IntBuffer mapInts(int offset) {
        return mapInts(offset, size().getInts() - offset);
    }

    /**
     * Maps all ints in the buffer.
     *
     * @return mapped region as an int buffer
     * @see #map(int, int)
     */
    @Override
    default IntBuffer mapInts() {
        return mapInts(0, size().getInts());
    }

    /**
     * Maps floats within {@code offset} and {@code size}.
     *
     * @param offset offset in floats
     * @param size size in floats
     * @return mapped region as a float buffer
     * @see #map(int, int)
     */
    default FloatBuffer mapFloats(int offset, int size) {
        return map(offset * Float.BYTES, size * Float.BYTES).getFloatBuffer(0, size);
    }

    /**
     * Maps floats from {@code offset} to the end of the buffer.
     *
     * @param offset offset in floats
     * @return mapped region as a float buffer
     * @see #map(int, int)
     */
    default FloatBuffer mapFloats(int offset) {
        return mapFloats(offset, size().getFloats() - offset);
    }

    /**
     * Maps all floats in the buffer.
     *
     * @return mapped region as a float buffer
     * @see #map(int, int)
     */
    @Override
    default FloatBuffer mapFloats() {
        return mapFloats(0, size().getFloats());
    }

    /**
     * Maps doubles within {@code offset} and {@code size}.
     *
     * @param offset offset in doubles
     * @param size size in doubles
     * @return mapped region as a double buffer
     * @see #map(int, int)
     */
    default DoubleBuffer mapDoubles(int offset, int size) {
        return map(offset * Double.BYTES, size * Double.BYTES).getDoubleBuffer(0, size);
    }

    /**
     * Maps doubles from {@code offset} to the end of the buffer.
     *
     * @param offset offset in doubles
     * @return mapped region as a double buffer
     * @see #map(int, int)
     */
    default DoubleBuffer mapDoubles(int offset) {
        return mapDoubles(offset, size().getDoubles() - offset);
    }

    /**
     * Maps all doubles in the buffer.
     *
     * @return mapped region as a double buffer
     * @see #map(int, int)
     */
    @Override
    default DoubleBuffer mapDoubles() {
        return mapDoubles(0, size().getDoubles());
    }

    /**
     * Maps longs within {@code offset} and {@code size}.
     *
     * @param offset offset in longs
     * @param size size in longs
     * @return mapped region as a long buffer
     * @see #map(int, int)
     */
    default LongBuffer mapLongs(int offset, int size) {
        return map(offset * Long.BYTES, size * Long.BYTES).getLongBuffer(0, size);
    }

    /**
     * Maps longs from {@code offset} to the end of the buffer.
     *
     * @param offset offset in longs
     * @return mapped region as a long buffer
     * @see #map(int, int)
     */
    default LongBuffer mapLongs(int offset) {
        return mapLongs(offset, size().getLongs() - offset);
    }

    /**
     * Maps all longs in the buffer.
     *
     * @return mapped region as a long buffer
     * @see #map(int, int)
     */
    @Override
    default LongBuffer mapLongs() {
        return mapLongs(0, size().getLongs());
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
     * @see #map(int, int)
     */
    default IndexBuffer mapIndices(int offset, int size) {
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
     * @see #map(int, int)
     */
    default IndexBuffer mapIndices(int offset) {
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
     * @see #map(int, int)
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

    /**
     * Copies the contents of {@code buffer} to this buffer. This buffer must be large
     * enough to fit the contents of {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(GpuBuffer buffer) {
        verifyBufferSize(buffer.size().getBytes(), Byte.BYTES);
        MemoryUtil.memCopy(buffer.mapBytes(), mapBytes(0, buffer.size().getBytes()));
        buffer.unmap();
        unmap();
    }

    /**
     * Copies all bytes from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all bytes from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(ByteBuffer buffer) {
        verifyBufferSize(buffer.limit(), Byte.BYTES);
        MemoryUtil.memCopy(buffer, mapBytes(0, buffer.limit()));
        unmap();
    }

    /**
     * Copies all shorts from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all shorts from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(ShortBuffer buffer) {
        verifyBufferSize(buffer.limit(), Short.BYTES);
        MemoryUtil.memCopy(buffer, mapShorts(0, buffer.limit()));
        unmap();
    }

    /**
     * Copies all ints from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all integers from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(IntBuffer buffer) {
        verifyBufferSize(buffer.limit(), Integer.BYTES);
        MemoryUtil.memCopy(buffer, mapInts(0, buffer.limit()));
        unmap();
    }

    /**
     * Copies all floats from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all floats from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(FloatBuffer buffer) {
        verifyBufferSize(buffer.limit(), Float.BYTES);
        MemoryUtil.memCopy(buffer, mapFloats(0, buffer.limit()));
        unmap();
    }

    /**
     * Copies all doubles from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all doubles from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(DoubleBuffer buffer) {
        verifyBufferSize(buffer.limit(), Double.BYTES);
        MemoryUtil.memCopy(buffer, mapDoubles(0, buffer.limit()));
        unmap();
    }

    /**
     * Copies all longs from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all longs from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(LongBuffer buffer) {
        verifyBufferSize(buffer.limit(), Long.BYTES);
        MemoryUtil.memCopy(buffer, mapLongs(0, buffer.limit()));
        unmap();
    }

    /**
     * Copies all content from {@code buffer} to this buffer. This buffer must be large
     * enough to fit all content from {@code buffer}.
     *
     * @param buffer buffer to copy
     */
    default void copy(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            copy((ByteBuffer)buffer);
        } else if (buffer instanceof ShortBuffer) {
            copy((ShortBuffer)buffer);
        } else if (buffer instanceof IntBuffer) {
            copy((IntBuffer)buffer);
        } else if (buffer instanceof FloatBuffer) {
            copy((FloatBuffer)buffer);
        } else if (buffer instanceof DoubleBuffer) {
            copy((DoubleBuffer)buffer);
        } else if (buffer instanceof LongBuffer) {
            copy((LongBuffer)buffer);
        }
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
