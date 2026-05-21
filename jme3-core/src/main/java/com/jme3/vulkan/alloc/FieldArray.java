package com.jme3.vulkan.alloc;

import com.jme3.util.struct.StructField;

import java.nio.ByteBuffer;
import java.util.Iterator;

public class FieldArray <T extends StructField> implements Memory, Iterable<T> {

    private final StructArray source;
    private final T field;

    public FieldArray(StructArray source, T field) {
        this.source = source;
        this.field = field;
    }

    @Override
    public ByteBuffer map(MappingArena arena) {
        return source.map(arena);
    }

    @Override
    public ByteBuffer map() {
        return source.map();
    }

    @Override
    public void stage(long offset, long size) {
        source.stage(offset, size);
    }

    @Override
    public void stage() {
        source.stage();
    }

    @Override
    public void destroy() {
        source.destroy();
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl();
    }

    public T index(int index) {
        source.index(index);
        return field;
    }

    public void set(int index, Object value) {
        source.index(index);
        field.set(value);
    }

    private class IteratorImpl implements Iterator<T> {

        private final Iterator structArray = source.iterator();

        @Override
        public boolean hasNext() {
            return structArray.hasNext();
        }

        @Override
        public T next() {
            structArray.next();
            return field;
        }

    }

}
