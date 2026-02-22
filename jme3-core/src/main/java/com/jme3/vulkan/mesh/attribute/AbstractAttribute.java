package com.jme3.vulkan.mesh.attribute;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.mesh.AttributeMappingInfo;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.BiConsumer;

public abstract class AbstractAttribute <T, E> implements Attribute<T> {

    protected final ValueMapper<E> mapper;
    private final AttributeMappingInfo info;
    private BufferMapping mapping;

    public AbstractAttribute(ValueMapper<E> mapper, AttributeMappingInfo info) {
        this.mapper = mapper;
        this.info = info;
        this.mapping = info.getVertices().map(info.getBinding().getOffset());
    }

    @Override
    public void close() {
        if (mapping == null) {
            throw new IllegalStateException("Attribute has already been unmapped.");
        }
        mapping.close();
        mapping = null;
    }

    @Override
    public void push(long baseElement, long elements) {
        mapping.push(baseElement * info.getBinding().getStride(), elements * info.getBinding().getStride());
    }

    @Override
    public void push() {
        push(0, info.getSize());
    }

    @Override
    public Iterable<T> read(T store) {
        return new ReadIterator<>(this, createStorageObject(store), info.getSize());
    }

    @Override
    public Iterable<T> readWrite(T store) {
        return new ReadWriteIterator<>(this, createStorageObject(store), info.getSize());
    }

    @Override
    public Iterable<T> write(T store) {
        return new WriteIterator<>(this, createStorageObject(store), info.getSize());
    }

    @Override
    public Iterable<T> transfer(Attribute<T> dst, T store) {
        store = createStorageObject(store);
        return new TransferIterator<>(read(store).iterator(), dst.write(store).iterator(), this::copyValueTo);
    }

    @Override
    public Iterable<Integer> indices() {
        return new IndexIterator(info.getSize());
    }

    @Override
    public long getNumElements() {
        return info.getSize();
    }

    protected abstract void copyValueTo(T src, T dst);

    protected ByteBuffer getBuffer() {
        return mapping.getBytes();
    }

    protected ByteBuffer getBuffer(long element) {
        // buffer is already offset by binding.getOffset() relative to vertices
        mapping.getBytes().position((int)(element * info.getBinding().getStride() + info.getOffset()));
        return mapping.getBytes();
    }

    protected static class ReadIterator <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final long size;
        private int index = 0;

        public ReadIterator(Attribute<T> attr, T store, long size) {
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

    protected static class ReadWriteIterator <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final long size;
        private int index = 0;
        private T toWrite;

        public ReadWriteIterator(Attribute<T> attr, T store, long size) {
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

    protected static class WriteIterator <T> implements Iterator<T>, Iterable<T> {

        private final Attribute<T> attr;
        private final T store;
        private final long size;
        private int index = 0;
        private T toWrite;

        public WriteIterator(Attribute<T> attr, T store, long size) {
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

    protected static class TransferIterator <T> implements Iterable<T>, Iterator<T> {

        private final Iterator<T> read, write;
        private final BiConsumer<T, T> assign;
        private T src;

        public TransferIterator(Iterator<T> read, Iterator<T> write, BiConsumer<T, T> assign) {
            this.read = read;
            this.write = write;
            this.assign = assign;
        }

        @Override
        public Iterator<T> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            if (src != null) {
                assign.accept(src, write.next());
                src = null;
            }
            return read.hasNext() & write.hasNext();
        }

        @Override
        public T next() {
            return src = read.next();
        }

    }

    protected static class IndexIterator implements Iterator<Integer>, Iterable<Integer> {

        private final long size;
        private int index = 0;

        public IndexIterator(long size) {
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
