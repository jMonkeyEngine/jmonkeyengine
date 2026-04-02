package com.jme3.util.struct;

import com.jme3.vulkan.buffers.BufferMapping;

public interface FieldDesc<T> {

    int getSize(StructLayout layout, T value);

    int getAlignment(StructLayout layout, T value);

    void write(StructLayout layout, BufferMapping mapping, int position, T value);

    T read(StructLayout layout, BufferMapping mapping, int position, T store);

}
