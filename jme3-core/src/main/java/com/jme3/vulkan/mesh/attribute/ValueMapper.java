package com.jme3.vulkan.mesh.attribute;

import com.jme3.util.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Maps values to a ByteBuffer and vise versa.
 */
public interface ValueMapper <T> {

    /**
     * Relative put method.
     *
     * @param buffer buffer to put
     * @param value value to put
     */
    void put(ByteBuffer buffer, T value);

    /**
     * Relative get method.
     *
     * @param buffer buffer to get from
     * @return retrieved value
     */
    T get(ByteBuffer buffer);

    ValueMapper<Float> Float64 = new ValueMapperImpl<>(
            (b, v) -> b.putDouble(v.doubleValue()), b -> (float)b.getDouble());
    ValueMapper<Float> Float32 = new ValueMapperImpl<>(ByteBuffer::putFloat, ByteBuffer::getFloat);
    ValueMapper<Float> Float16 = new ValueMapperImpl<>(
            (b, v) -> b.putShort(v.shortValue()), b -> (float)b.getShort());
    ValueMapper<Float> Float8 = new ValueMapperImpl<>(
            (b, v) -> b.put(v.byteValue()), b -> (float)b.get());

    ValueMapper<Integer> Int64 = new ValueMapperImpl<>(
            (b, v) -> b.putLong(v.longValue()), b -> (int)b.getLong());
    ValueMapper<Integer> Int32 = new ValueMapperImpl<>(ByteBuffer::putInt, ByteBuffer::getInt);
    ValueMapper<Integer> Int16 = new ValueMapperImpl<>(
            (b, v) -> b.putShort(v.shortValue()), b -> (int)b.getShort());
    ValueMapper<Integer> Int8 = new ValueMapperImpl<>(
            (b, v) -> b.put(v.byteValue()), b -> (int)b.get());

    static ValueMapper<Float> Float(int bytesPerElement) {
        switch (bytesPerElement) {
            case 1: return Float8;
            case 16: return Float16;
            case 32: return Float32;
            case 64: return Float64;
            default: throw new IllegalArgumentException();
        }
    }

    static ValueMapper<Integer> Int(int bytesPerElement) {
        switch (bytesPerElement) {
            case 1: return Int8;
            case 16: return Int16;
            case 32: return Int32;
            case 64: return Int64;
            default: throw new IllegalArgumentException();
        }
    }

    class ValueMapperImpl <T> implements ValueMapper<T> {

        private final BiConsumer<ByteBuffer, T> putter;
        private final Function<ByteBuffer, T> getter;

        public ValueMapperImpl(BiConsumer<ByteBuffer, T> putter, Function<ByteBuffer, T> getter) {
            this.putter = putter;
            this.getter = getter;
        }

        @Override
        public void put(ByteBuffer buffer, T value) {
            putter.accept(buffer, value);
        }

        @Override
        public T get(ByteBuffer buffer) {
            return getter.apply(buffer);
        }

    }

}
