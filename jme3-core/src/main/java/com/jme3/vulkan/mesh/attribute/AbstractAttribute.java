package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.buffers.Mappable;

import java.nio.ByteBuffer;
import java.util.Iterator;

public abstract class AbstractAttribute <T> implements Attribute<T> {

    private final Mappable vertices;
    private final int size, stride, offset;
    private ByteBuffer buffer;

    public AbstractAttribute(Mappable vertices, int size, int stride, int offset) {
        this.vertices = vertices;
        this.size = size;
        this.stride = stride;
        this.offset = offset;
        this.buffer = vertices.mapBytes();
    }

    @Override
    public void unmap() {
        if (buffer == null) {
            throw new IllegalStateException("Attribute has already been unmapped.");
        }
        buffer = null;
        vertices.unmap();
    }

    protected ByteBuffer getBuffer() {
        return buffer;
    }

    protected ByteBuffer getBuffer(int element) {
        return buffer.position(element * stride + offset);
    }

    @Override
    public Iterator<T> iterator() {
        if (vertices == null) {
            throw new IllegalStateException("Cannot iterate over unmapped attribute.");
        }
        return new IteratorImpl<>(this, null, size);
    }

    public IteratorImpl<T> iterator(T store) {
        if (vertices == null) {
            throw new IllegalStateException("Cannot iterate over unmapped attribute.");
        }
        return new IteratorImpl<>(this, store, size);
    }

    public IndexIterator indices() {
        return new IndexIterator(size);
    }

    public static class IteratorImpl <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final int size;
        private int index = 0;

        public IteratorImpl(Attribute<T> attr, T store, int size) {
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
