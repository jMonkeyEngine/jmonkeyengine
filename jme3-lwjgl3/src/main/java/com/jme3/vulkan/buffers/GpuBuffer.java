package com.jme3.vulkan.buffers;

import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemorySize;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Struct;
import org.lwjgl.system.StructBuffer;
import org.lwjgl.vulkan.VkBufferCopy;

import java.nio.*;
import java.util.function.Function;

import static org.lwjgl.vulkan.VK10.*;

public interface GpuBuffer {

    /**
     * Maps the memory of this buffer and returns a pointer to the mapped
     * region. If the memory is currently mapped when this is called, an
     * exception is thrown.
     *
     * @param offset offset, in bytes, of the mapping
     * @param size size, in bytes, of the mapping, or {@link org.lwjgl.vulkan.VK10#VK_WHOLE_SIZE
     * VK_WHOLE_SIZE} to map all buffer memory starting at {@code offset}.
     * @return Buffer containing a pointer to the mapped memory
     */
    PointerBuffer map(int offset, int size);

    void unmap();

    void freeMemory();

    MemorySize size();

    long getId();

    default void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size().getBytes()) {
            throw new BufferOverflowException();
        }
    }

    default <T> T map(int offset, int size, Function<PointerBuffer, T> factory) {
        return factory.apply(map(offset, size));
    }

    default ByteBuffer mapBytes(int offset, int size) {
        return map(offset * Byte.BYTES, size * Byte.BYTES).getByteBuffer(0, size);
    }

    default ShortBuffer mapShorts(int offset, int size) {
        return map(offset * Short.BYTES, size * Short.BYTES).getShortBuffer(0, size);
    }

    default IntBuffer mapInts(int offset, int size) {
        return map(offset * Integer.BYTES, size * Integer.BYTES).getIntBuffer(0, size);
    }

    default FloatBuffer mapFloats(int offset, int size) {
        return map(offset * Float.BYTES, size * Float.BYTES).getFloatBuffer(0, size);
    }

    default DoubleBuffer mapDoubles(int offset, int size) {
        return map(offset * Double.BYTES, size * Double.BYTES).getDoubleBuffer(0, size);
    }

    default LongBuffer mapLongs(int offset, int size) {
        return map(offset * Long.BYTES, size * Long.BYTES).getLongBuffer(0, size);
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
