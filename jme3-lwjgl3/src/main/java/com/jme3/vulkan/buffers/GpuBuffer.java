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

    PointerBuffer map(MemoryStack stack, int offset, int size, int flags);

    void unmap();

    void freeMemory();

    MemorySize size();

    long getId();

    default void verifyBufferSize(int elements, long bytesPerElement) {
        if (elements * bytesPerElement > size().getBytes()) {
            throw new BufferOverflowException();
        }
    }

    default <T> T map(MemoryStack stack, int offset, int size, int flags, Function<PointerBuffer, T> factory) {
        return factory.apply(map(stack, offset, size, flags));
    }

    default ByteBuffer mapBytes(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Byte.BYTES, flags).getByteBuffer(0, size);
    }

    default ShortBuffer mapShorts(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Short.BYTES, flags).getShortBuffer(0, size);
    }

    default IntBuffer mapInts(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Integer.BYTES, flags).getIntBuffer(0, size);
    }

    default FloatBuffer mapFloats(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Float.BYTES, flags).getFloatBuffer(0, size);
    }

    default DoubleBuffer mapDoubles(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Double.BYTES, flags).getDoubleBuffer(0, size);
    }

    default LongBuffer mapLongs(MemoryStack stack, int offset, int size, int flags) {
        return map(stack, offset, size * Long.BYTES, flags).getLongBuffer(0, size);
    }

    default void copy(MemoryStack stack, ByteBuffer buffer) {
        verifyBufferSize(buffer.limit(), Byte.BYTES);
        MemoryUtil.memCopy(buffer, mapBytes(stack, 0, buffer.limit(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, ShortBuffer buffer) {
        verifyBufferSize(buffer.limit(), Short.BYTES);
        MemoryUtil.memCopy(buffer, mapShorts(stack, 0, buffer.limit(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, IntBuffer buffer) {
        verifyBufferSize(buffer.limit(), Integer.BYTES);
        MemoryUtil.memCopy(buffer, mapInts(stack, 0, buffer.limit(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, FloatBuffer buffer) {
        verifyBufferSize(buffer.limit(), Float.BYTES);
        MemoryUtil.memCopy(buffer, mapFloats(stack, 0, buffer.limit(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, DoubleBuffer buffer) {
        verifyBufferSize(buffer.limit(), Double.BYTES);
        MemoryUtil.memCopy(buffer, mapDoubles(stack, 0, buffer.limit(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, LongBuffer buffer) {
        verifyBufferSize(buffer.limit(), Long.BYTES);
        MemoryUtil.memCopy(buffer, mapLongs(stack, 0, buffer.limit(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, Struct<?> struct) {
        verifyBufferSize(struct.sizeof(), Byte.BYTES);
        MemoryUtil.memCopy(MemoryUtil.memByteBuffer(struct.address(), struct.sizeof()),
                mapBytes(stack, 0, struct.sizeof(), 0));
        unmap();
    }

    default void copy(MemoryStack stack, StructBuffer<?, ?> buffer) {
        verifyBufferSize(buffer.limit(), buffer.sizeof());
        int size = buffer.limit() * buffer.sizeof();
        MemoryUtil.memCopy(MemoryUtil.memByteBuffer(buffer.address(), size), mapBytes(stack, 0, size, 0));
        unmap();
    }

    default void recordCopy(MemoryStack stack, CommandBuffer commands, GpuBuffer source,
                           long srcOffset, long dstOffset, long size) {
        VkBufferCopy.Buffer copy = VkBufferCopy.calloc(1, stack)
                .srcOffset(srcOffset).dstOffset(dstOffset).size(size);
        vkCmdCopyBuffer(commands.getBuffer(), source.getId(), getId(), copy);
    }

}
