package com.jme3.vulkan.alloc;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.function.Function;

public class StructArray <T extends Struct> implements MemoryPointer, Iterable<T> {

    private final T struct;
    private final SlicePointer index;
    private final int length;
    private Memory source;

    public StructArray(T struct, int length) {
        this.struct = struct;
        this.length = length;
        this.index = new SlicePointer(0, struct.getAlignedSize());
        this.index.bind(this);
        this.struct.bind(index);
    }

    @Override
    public void bind(Memory memory) {
        this.source = memory;
    }

    @Override
    public ByteBuffer map(MappingArena arena) {
        assert source != null : "No memory bound.";
        return source.map(arena);
    }

    @Override
    public ByteBuffer map() {
        assert source != null : "No memory bound.";
        return source.map();
    }

    @Override
    public void stage(long offset, long size) {
        assert source != null : "No memory bound.";
        source.stage(offset, size);
    }

    @Override
    public void stage() {
        assert source != null : "No memory bound.";
        source.stage();
    }

    @Override
    public Memory getBoundMemory() {
        return source;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl();
    }

    public T index(int index) {
        if (index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        this.index.setOffset(index * struct.getAlignedSize());
        return struct;
    }

    public <E extends Struct> E index(int index, E struct) {
        if (index >= length) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for length " + length);
        }
        SlicePointer ptr = new SlicePointer(index * this.struct.getAlignedSize(), this.struct.getAlignedSize());
        ptr.bind(this);
        struct.bind(ptr);
        return struct;
    }

    public <E extends StructField> FieldArray<E> field(Function<T, E> field) {
        return new FieldArray<>(this, field.apply(struct));
    }

    public T getStruct() {
        return struct;
    }

    public int length() {
        return length;
    }

    public int getAlignedSize() {
        return struct.getAlignedSize() * length;
    }

    private class IteratorImpl implements Iterator<T> {

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
