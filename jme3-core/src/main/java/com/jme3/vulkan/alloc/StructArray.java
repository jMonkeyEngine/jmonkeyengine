package com.jme3.vulkan.alloc;

import com.jme3.util.natives.Destructor;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.util.struct.StructuredArray;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.EngineBuffer;
import com.jme3.vulkan.commands.CommandBuffer;
import com.jme3.vulkan.memory.MemoryProp;
import com.jme3.vulkan.util.Flag;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;

public class StructArray <T extends Struct> implements StructuredArray<T>, BufferDescription, Iterable<T> {

    private int length;
    private final T sharedStruct;
    private final int stride;
    private EngineBuffer buffer;
    private int bufferOffset;

    public StructArray(int length, T struct) {
        this.length = length;
        this.sharedStruct = struct;
        stride = sharedStruct.getAlignedSize();
    }

    @Override
    public DataBuffer cache() {
        return buffer.cache().offset(bufferOffset, length * stride);
    }

    @Override
    public void bind(EngineBuffer buffer, int baseOffset) {
        this.buffer = buffer;
        this.bufferOffset = baseOffset;
        sharedStruct.bind(null, 0);
    }

    @Override
    public int size() {
        return length * stride;
    }

    @Override
    public Iterator<T> iterator() {
        return new SharedIteratorImpl();
    }

    @Override
    public T index(int index) {
        sharedStruct.bind(buffer, bufferOffset + index * stride);
        return sharedStruct;
    }

    @Override
    public int getLength() {
        return length;
    }

    /**
     * Sets the length of this array in elements.
     *
     * @param length length in elements
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Binds {@code struct} to {@code index} in this array.
     *
     * @param index index to bind to
     * @param struct struct to bind
     * @return {@code struct}
     * @param <E> struct type
     */
    @Override
    public <E extends Struct> E index(int index, E struct) {
        struct.bind(buffer, bufferOffset + index * stride);
        return struct;
    }

    /**
     * Creates a {@link Field} for this array based on the {@link StructField}
     * returned by {@code field}. Changes made through the returned field array are
     * reflected by this array and vise versa. The field is accessed internally
     * through the shared struct via {@link #index(int)}.
     *
     * @param field function that fetches the field to be represented by the returned array
     * @return field array representing the field
     * @param <F> field type
     */
    public <F extends StructField> Field<F> field(Function<T, F> field) {
        return new Field<>(this, field.apply(sharedStruct));
    }

    /**
     * Creates an array that is a slice of this array. The emitted array
     * shares the same struct as this array.
     *
     * @param offset offset of the slice
     * @param length length of the slice
     * @return array being a slice of this array
     */
    public StructArray<T> slice(int offset, int length) {
        StructArray<T> array = new StructArray<>(length, sharedStruct);
        array.bind(buffer, bufferOffset + offset * stride);
        return array;
    }

    /**
     * Gets a struct that represents the layout of all structs officially associated
     * with this array (not structs bound through {@link #index(int, Struct)}).
     *
     * @return representational struct
     */
    public T getStruct() {
        return sharedStruct;
    }

    /**
     * Gets the byte offset at the specified index.
     *
     * @param index index
     * @return byte offset
     */
    public int getBytePosition(int index) {
        return index * stride;
    }

    /**
     * Gets the size of each array element (stride) in bytes.
     *
     * @return byte stride
     */
    public int getByteStride() {
        return stride;
    }

    public static class Field <F extends StructField> implements BufferDescription, Iterable<F> {

        private final StructArray<?> array;
        private final F field;

        protected Field(StructArray array, F field) {
            this.array = array;
            this.field = field;
        }

        @Override
        public void bind(EngineBuffer buffer, int baseOffset) {
            throw new UnsupportedOperationException("Array field cannot be bound separate to its array.");
        }

        @Override
        public DataBuffer cache() {
            return array.cache();
        }

        @Override
        public int size() {
            return array.size();
        }

        @Override
        public Iterator<F> iterator() {
            return new FieldIteratorImpl();
        }

        public F index(int index) {
            array.index(index);
            return field;
        }

        @SuppressWarnings("unchecked")
        public void set(int index, Object value) {
            array.index(index);
            field.set(value);
        }

        public int getOffset() {
            return field.getStructLocalOffset();
        }

        public int getStride() {
            return array.stride;
        }

        private class FieldIteratorImpl implements Iterator<F> {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < array.getLength();
            }

            @Override
            public F next() {
                array.index(index++);
                return field;
            }

        }

    }

    private class SharedIteratorImpl implements Iterator<T> {

        private int index = 0;
        private final int length = getLength();

        @Override
        public boolean hasNext() {
            return index < length;
        }

        @Override
        public T next() {
            return index(index++);
        }

    }

}
