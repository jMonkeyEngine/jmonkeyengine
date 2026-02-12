package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.mesh.AttributeMappingInfo;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;

public abstract class AbstractAttribute <T, E> implements Attribute<T> {

    protected final ValueMapper<E> mapper;
    private final AttributeMappingInfo info;
    private ByteBuffer buffer;

    public AbstractAttribute(ValueMapper<E> mapper, AttributeMappingInfo info) {
        this.mapper = mapper;
        this.info = info;
        this.buffer = info.getVertices().mapBytes((int)info.getBinding().getOffset());
    }

    @Override
    public void unmap() {
        if (buffer == null) {
            throw new IllegalStateException("Attribute has already been unmapped.");
        }
        buffer = null;
        info.getVertices().unmap();
    }

    @Override
    public void push(int baseElement, int elements) {
        info.getVertices().push((int)info.getBinding().getOffset() + baseElement * info.getBinding().getStride(), elements * info.getBinding().getStride());
    }

    @Override
    public void push() {
        push(0, info.getSize());
    }

    @Override
    public ReadIterator<T> read(T store) {
        return new ReadIterator<>(this, store, info.getSize());
    }

    @Override
    public ReadWriteIterator<T> readWrite(T store) {
        return new ReadWriteIterator<>(this, store, info.getSize());
    }

    @Override
    public Iterable<T> write(T store) {
        return null;
    }

    @Override
    public IndexIterator indices() {
        return new IndexIterator(info.getSize());
    }

    protected ByteBuffer getBuffer() {
        return buffer;
    }

    protected ByteBuffer getBuffer(int element) {
        // buffer is already offset by binding.getOffset() relative to vertices
        buffer.position(element * info.getBinding().getStride() + info.getOffset());
        return buffer;
    }

    public static class ReadIterator <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final int size;
        private int index = 0;

        public ReadIterator(Attribute<T> attr, T store, int size) {
            this.attr = attr;
            this.store = store;
            this.size = size;
        }

        @Override
        public Iterator<T> iterator() {
            this.index = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public T next() {
            return attr.get(index++, store);
        }

    }

    public static class ReadWriteIterator <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final int size;
        private int index = 0;
        private T toWrite;

        public ReadWriteIterator(Attribute<T> attr, T store, int size) {
            this.attr = attr;
            this.store = store;
            this.size = size;
        }

        @Override
        public Iterator<T> iterator() {
            this.index = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            if (toWrite != null) {
                attr.set(index - 1, toWrite);
                toWrite = null;
            }
            return index < size;
        }

        @Override
        public T next() {
            return toWrite = attr.get(index++, store);
        }

    }

    public static class WriteIterator <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final int size;
        private int index = 0;
        private T toWrite;

        public WriteIterator(Attribute<T> attr, T store, int size) {
            this.attr = attr;
            this.store = Objects.requireNonNull(store, "Storage object cannot be null for write-only iteration.");
            this.size = size;
        }

        @Override
        public Iterator<T> iterator() {
            this.index = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            if (toWrite != null) {
                attr.set(index - 1, toWrite);
                toWrite = null;
            }
            return index < size;
        }

        @Override
        public T next() {
            return toWrite = store;
        }

    }

    public static class IndexIterator implements Iterator<Integer>, Iterable<Integer> {

        private final int size;
        private int index = 0;

        public IndexIterator(int size) {
            this.size = size;
        }

        @Override
        public Iterator<Integer> iterator() {
            this.index = 0;
            return this;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public Integer next() {
            return index++;
        }

    }

}
