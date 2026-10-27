package com.jme3.util.struct;

import com.jme3.vulkan.buffer.BufferBindingInfo;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;

import java.util.Iterator;

public class SingletonStructArray <T extends Struct> implements StructArray<T> {

    private final int length;
    private final T struct;
    protected final BufferBindingInfo binding = new BufferBindingInfo();
    protected int alignment, stride;

    public SingletonStructArray(int length, T struct) {
        this.length = length;
        this.struct = struct;
    }

    @Override
    public void setupLayout(StructLayout layout) {
        struct.setupLayout(layout);
        alignment = layout.getArrayElementAlignment(struct.alignment());
        stride = layout.getArrayElementSize(alignment, struct.size());
    }

    @Override
    public void bind(GpuBuffer buffer, int offset) {
        struct.bind(buffer, offset);
        binding.setBuffer(buffer);
        binding.setOffset(offset);
        binding.setLimit(offset + stride * length);
    }

    @Override
    public void unbind() {
        binding.unbind();
        struct.unbind();
    }

    @Override
    public DataBuffer cache() {
        return binding.cache();
    }

    @Override
    public int alignment() {
        return struct.alignment();
    }

    @Override
    public int size() {
        return struct.alignedSize() * length;
    }

    @Override
    public T index(int i) {
        if (i >= length) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + length);
        }
        struct.bind(binding.getBuffer(), binding.getOffset() + stride * i);
        return struct;
    }

    @Override
    public T index(int i, T struct) {
        if (i >= length) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + length);
        }
        struct.bind(binding.getBuffer(), binding.getOffset() + stride * i);
        return struct;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public int stride() {
        return stride;
    }

    @Override
    public T getStruct() {
        return struct;
    }

    @Override
    public Iterator<T> iterator() {
        return new IteratorImpl();
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
