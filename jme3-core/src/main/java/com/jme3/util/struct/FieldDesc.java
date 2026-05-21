package com.jme3.util.struct;

import java.nio.ByteBuffer;

public interface FieldDesc<T> {

    int getSize(StructLayout layout, T value);

    int getAlignment(StructLayout layout, T value);

    void write(StructLayout layout, ByteBuffer buffer, T value);

    T read(StructLayout layout, ByteBuffer buffer, T store);

}
