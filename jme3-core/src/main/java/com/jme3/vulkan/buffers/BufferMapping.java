package com.jme3.vulkan.buffers;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructMapping;
import com.jme3.util.struct.StructSequence;
import org.lwjgl.PointerBuffer;

import java.nio.*;
import java.util.function.BiFunction;
import java.util.function.LongFunction;

public interface BufferMapping extends AutoCloseable {

    @Override
    void close();

    boolean isMapped();

    void stage(long offset, long size);

    long getAddress();

    long getSize();

    ByteBuffer getBytes();

    ShortBuffer getShorts();

    IntBuffer getInts();

    FloatBuffer getFloats();

    DoubleBuffer getDoubles();

    LongBuffer getLongs();

    PointerBuffer getPointers();

    default void stage() {
        stage(0, getSize());
    }

    default <T> T get(LongFunction<T> factory) {
        return factory.apply(getAddress());
    }

    default <T> T get(BiFunction<Long, Long, T> factory) {
        return factory.apply(getAddress(), getSize());
    }

}
