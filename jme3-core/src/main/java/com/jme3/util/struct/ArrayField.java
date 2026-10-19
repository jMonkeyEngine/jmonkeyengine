package com.jme3.util.struct;

import com.jme3.vulkan.buffer.BufferBindingInfo;
import com.jme3.vulkan.buffer.DataBuffer;
import com.jme3.vulkan.buffer.GpuBuffer;

public class ArrayField <T> implements StructField {

    private final int length;
    private T alias;

    private final BufferBindingInfo binding = new BufferBindingInfo();
    private FieldDescription<T> desc;
    private int alignment, stride;

    public ArrayField(int length, T alias) {
        this.length = length;
        this.alias = alias;
    }

    @Override
    public void setupLayout(StructLayout layout) {
        desc = layout.getFieldDescription(alias.getClass());
        alignment = layout.getArrayElementAlignment(desc.getAlignment());
        stride = layout.getArrayElementSize(alignment, desc.getSize());
    }

    @Override
    public void bind(GpuBuffer buffer, int offset) {
        binding.setBuffer(buffer);
        binding.setOffset(offset);
        binding.setLimit(offset + stride * length);
    }

    @Override
    public int alignment() {
        return alignment;
    }

    @Override
    public void unbind() {
        binding.setBuffer(null);
    }

    @Override
    public DataBuffer cache() {
        return binding.cache();
    }

    @Override
    public int size() {
        return binding.getSize();
    }

    private DataBuffer elementCache(int i) {
        return binding.getBuffer().cache().slice(binding.getOffset() + stride * i, binding.getOffset() + stride * (i + 1));
    }

    public void set(int i, T value) {
        if (i > length) {
            throw new IndexOutOfBoundsException("Index " + i + " out of bounds for length " + length);
        }
        desc.write(elementCache(i), value);
    }

    public T index(int i, T store) {
        return desc.read(elementCache(i), store);
    }

    public T index(int i) {
        return alias = index(i, alias);
    }

}
