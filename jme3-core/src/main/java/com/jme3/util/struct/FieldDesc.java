package com.jme3.util.struct;

public interface FieldDesc<T> {

    int getSize(StructLayout layout, T value);

    int getAlignment(StructLayout layout, T value);

    void write(StructLayout layout, long address, T value);

    T read(StructLayout layout, long address, T store);

}
