package com.jme3.util.struct;

import com.jme3.vulkan.buffers.BufferMapping;
import com.jme3.vulkan.buffers.MappableBuffer;
import com.jme3.vulkan.memory.MemorySize;

import java.util.Iterator;
import java.util.function.Function;

public class StructBuffer <T extends Struct> implements MappableBuffer, Iterable<T> {

    private final StructLayout layout;
    private final T[] structs;
    private final MappableBuffer buffer;

    public StructBuffer(StructLayout layout, T[] structs, Function<MemorySize, MappableBuffer> buffer) {
        assert structs.length > 0 : "Struct buffer must contain at least one struct.";
        this.layout = layout;
        this.structs = structs;
        this.buffer = buffer.apply(MemorySize.bytes(structs[0].getSize(layout)));
    }

    @Override
    public BufferMapping map(long offset, long size) {
        return buffer.map(offset, size);
    }

    @Override
    public void stage(long offset, long size) {
        buffer.stage(offset, size);
    }

    @Override
    public ResizeResult resize(MemorySize size) {
        return buffer.resize(size);
    }

    @Override
    public MemorySize size() {
        return buffer.size();
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl<>(structs);
    }

    private static class IteratorImpl <T extends Struct> implements Iterator<T> {

        private final T[] array;
        private int index = 0;

        public IteratorImpl(T[] array) {
            this.array = array;
        }

        @Override
        public boolean hasNext() {
            return index < array.length;
        }

        @Override
        public T next() {
            return array[index++];
        }

    }

}
