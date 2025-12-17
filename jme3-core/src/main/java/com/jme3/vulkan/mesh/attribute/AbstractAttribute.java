package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.buffers.GpuBuffer;
import com.jme3.vulkan.mesh.VertexBinding;

import java.nio.ByteBuffer;
import java.util.Iterator;

public abstract class AbstractAttribute <T> implements Attribute<T> {

    private final VertexBinding binding;
    private final GpuBuffer vertices;
    private final int size, offset;
    private ByteBuffer buffer;

    public AbstractAttribute(VertexBinding binding, GpuBuffer vertices, int size, int offset) {
        this.binding = binding;
        this.vertices = vertices;
        this.size = size;
        this.offset = offset;
        this.buffer = vertices.mapBytes((int)binding.getOffset());
    }

    @Override
    public void unmap() {
        if (buffer == null) {
            throw new IllegalStateException("Attribute has already been unmapped.");
        }
        buffer = null;
        vertices.unmap();
    }

    @Override
    public void push(int baseElement, int elements) {
        vertices.push((int)binding.getOffset() + baseElement * binding.getStride(), elements * binding.getStride());
    }

    @Override
    public void push() {
        push(0, size);
    }

    protected ByteBuffer getBuffer() {
        return buffer;
    }

    protected ByteBuffer getBuffer(int element) {
        // buffer is already offset by binding.getOffset() relative to vertices
        return buffer.position(element * binding.getStride() + offset);
    }

    public ReadIterator<T> read(T store) {
        return new ReadIterator<>(this, store, size);
    }

    public ReadIterator<T> read() {
        return read(null);
    }

    public ReadWriteIterator<T> readWrite(T store) {
        return new ReadWriteIterator<>(this, store, size);
    }

    public ReadWriteIterator<T> readWrite() {
        return readWrite(null);
    }

    public IndexIterator indices() {
        return new IndexIterator(size);
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
