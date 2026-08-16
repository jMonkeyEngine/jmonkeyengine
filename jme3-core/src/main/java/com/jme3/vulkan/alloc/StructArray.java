package com.jme3.vulkan.alloc;

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

public class StructArray <T extends Struct> implements StructuredArray<T>, RelativeBuffer, Iterable<T> {

    private int length;
    private final Index<T> sharedStruct;
    private final int stride;
    private int currentIndex = 0;
    private EngineBuffer source;

    public StructArray(int length, T struct) {
        this.length = length;
        sharedStruct = new Index<>(struct);
        sharedStruct.getPointer().bind(this);
        stride = sharedStruct.getStruct().getAlignedSize();
    }

    public StructArray(int length, T struct, EngineBuffer source) {
        this(length, struct);
        bind(source);
    }

    public static <T extends Struct> StructArray<T> directBuffer(int length, T struct) {
        return new StructArray<>(length, struct, new DataBuffer(ByteBuffer.wrap(new byte[length * struct.getAlignedSize()])));
    }

    @Override
    public void update(CommandBuffer cmd) {
        source.update(cmd);
    }

    @Override
    public boolean isDeviceAccessible() {
        return source.isDeviceAccessible();
    }

    @Override
    public DataBuffer cache() {
        return source.cache();
    }

    @Override
    public void bind(EngineBuffer parent) {
        this.source = parent;
    }

    @Override
    public void invalidateCache() {
        source.invalidateCache();
    }

    @Override
    public int capacity() {
        return length * stride;
    }

    @Override
    public int getBufferLocalOffset() {
        return source.getBufferLocalOffset();
    }

    @Override
    public long getHandle() {
        return source.getHandle();
    }

    @Override
    public long getDeviceAddress() {
        return source.getDeviceAddress();
    }

    @Override
    public Flag<Role> getRoles() {
        return source.getRoles();
    }

    @Override
    public Flag<MemoryProp> getMemoryProperties() {
        return source.getMemoryProperties();
    }

    @Override
    public Iterator<T> iterator() {
        return new SharedIteratorImpl();
    }

    protected Index<T> getSharedStruct() {
        return sharedStruct;
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
    @Override
    public T index(int index) {
        Index<T> i = getSharedStruct();
        i.getPointer().setOffset(index * stride);
        this.currentIndex = index;
        return i.getStruct();
    }

    @Override
    public int getIndex() {
        return currentIndex;
    }

    @Override
    public int getLength() {
        return length;
    }

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
    public <E extends Struct> E index(int index, E struct) {
        SlicePointer ptr = new SlicePointer(index * stride);
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

    public static class Field <F extends StructField> implements EngineBuffer, Iterable<F> {

        private final StructArray<?> array;
        private final IntFunction<F> field;

        protected Field(StructArray array, IntFunction<F> field) {
            this.array = array;
            this.field = field;
        }

        @Override
        public void update(CommandBuffer cmd) {
            array.update(cmd);
        }

        @Override
        public boolean isDeviceAccessible() {
            return array.isDeviceAccessible();
        }

        @Override
        public DataBuffer cache() {
            return array.cache();
        }

        @Override
        public void invalidateCache() {
            array.invalidateCache();
        }

        @Override
        public int capacity() {
            return array.capacity();
        }

        @Override
        public int getBufferLocalOffset() {
            return array.getBufferLocalOffset();
        }

        @Override
        public long getHandle() {
            return array.getHandle();
        }

        @Override
        public long getDeviceAddress() {
            return array.getDeviceAddress();
        }

        @Override
        public Flag<Role> getRoles() {
            return array.getRoles();
        }

        @Override
        public Flag<MemoryProp> getMemoryProperties() {
            return array.getMemoryProperties();
        }

        @Override
        public Iterator<F> iterator() {
            return new FieldIteratorImpl();
        }

        public F index(int index) {
            return field.apply(index);
        }

        @SuppressWarnings("unchecked")
        public void set(int index, Object value) {
            field.apply(index).set(value);
        }

        private class FieldIteratorImpl implements Iterator<F> {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < array.getLength();
            }

            @Override
            public F next() {
                return field.apply(index++);
            }

        }

    }

    protected static class Index <T extends Struct> {

        private final T struct;
        private final SlicePointer ptr;

        public Index(T struct) {
            this.struct = struct;
            this.ptr = new SlicePointer(0);
            this.struct.bind(ptr);
        }

        public Index(T struct, int offset) {
            this(struct);
            ptr.setOffset(offset);
        }

        public T getStruct() {
            return struct;
        }

        public SlicePointer getPointer() {
            return ptr;
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
