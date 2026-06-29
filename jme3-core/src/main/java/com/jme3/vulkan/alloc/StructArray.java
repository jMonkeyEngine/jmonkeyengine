package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;
import com.jme3.vulkan.buffer.BufferMapping;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.IntFunction;

public class StructArray <T extends Struct> implements RelativeAddress, Iterable<T> {

    private final IndexedStruct<T> sharedStruct;
    private final int stride;
    private int length;
    private MemoryAddress source;

    public StructArray(T struct, int length) {
        this.length = length;
        sharedStruct = new IndexedStruct<>(struct);
        sharedStruct.getPointer().bind(this);
        stride = sharedStruct.getStruct().getAlignedSize();
    }

    public StructArray(T struct, int length, MemoryAddress source) {
        this(struct, length);
        bind(source);
    }

    @Override
    public BufferMapping map() {
        assert source != null : "No memory bound.";
        return source.map().region(0, length * stride);
    }

    @Override
    public void bind(MemoryAddress parent) {
        this.source = parent;
    }

    @Override
    public MemoryAddress getParentAddress() {
        return source;
    }

    @Override
    public Iterator<T> iterator() {
        return new SharedIteratorImpl();
    }

    protected IndexedStruct<T> getSharedStruct() {
        return sharedStruct;
    }

    /**
     * Resizes this array to the specified length.
     *
     * @param length length in structs to resize to
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Gets the shared, dynamically indexed struct and binds it to {@code index}. Previous
     * calls to this method are invalidated. This is the correct method to use if needing a
     * temporary handle into the struct array. If a more concrete handle is necessary, use
     * {@link #index(int, Struct)}, but this method should be preferred as it is the most
     * performant.
     *
     * @param index index to bind at
     * @return shared indexed struct
     */
    public T index(int index) {
        if (index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        IndexedStruct<T> i = getSharedStruct();
        i.getPointer().setOffset(index * stride);
        return i.getStruct();
    }

    /**
     * Binds {@code struct} to {@code index} in this array.
     *
     * @param index index to bind to
     * @param struct struct to bind
     * @return {@code struct}
     * @param <E> struct type
     */
    public <E extends Struct> E index(int index, E struct) {
        if (index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        OffsetPointer ptr = new OffsetPointer(index * stride);
        ptr.bind(this);
        struct.bind(ptr);
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
        return new Field<>(this, i -> field.apply(index(i)));
    }

    /**
     * Gets a struct that represents the layout of all structs officially associated
     * with this array (not structs bound through {@link #index(int, Struct)}).
     *
     * @return representational struct
     */
    public T getStruct() {
        return sharedStruct.getStruct();
    }

    /**
     * Gets the length of this array.
     *
     * @return array length in structs
     */
    public int length() {
        return length;
    }

    /**
     * Gets the size of this array in bytes.
     *
     * @return array size in bytes
     */
    public int getByteSize() {
        return stride * length;
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

    public static class Field <F extends StructField> implements MemoryAddress, Iterable<F> {

        private final StructArray array;
        private final IntFunction<F> field;

        protected Field(StructArray array, IntFunction<F> field) {
            this.array = array;
            this.field = field;
        }

        @Override
        public BufferMapping map() {
            return array.map();
        }

        @Override
        public Iterator<F> iterator() {
            return new FieldIteratorImpl();
        }

        public F index(int index) {
            return field.apply(index);
        }

        public void set(int index, Object value) {
            field.apply(index).set(value);
        }

        private class FieldIteratorImpl implements Iterator<F> {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < array.length();
            }

            @Override
            public F next() {
                return field.apply(index++);
            }

        }

    }

    protected static class IndexedStruct <T extends Struct> {

        private final T struct;
        private final OffsetPointer ptr;

        public IndexedStruct(T struct) {
            this.struct = struct;
            this.ptr = new OffsetPointer(0);
            this.struct.bind(ptr);
        }

        public T getStruct() {
            return struct;
        }

        public OffsetPointer getPointer() {
            return ptr;
        }

    }

    private class SharedIteratorImpl implements Iterator<T> {

        private int index = 0;

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
