package com.jme3.util.struct;

import com.jme3.vulkan.buffer.DataBuffer;

public interface FieldDescription <T> {

    int getSize();

    int getAlignment();

    void write(DataBuffer buffer, T value);

    T read(DataBuffer buffer, T store);

}
