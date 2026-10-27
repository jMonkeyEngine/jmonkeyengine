package com.jme3.util.struct;

public interface StructArray <T extends Struct> extends StructField, Iterable<T> {

    T index(int i);

    T index(int i, T struct);

    int length();

    int stride();

    T getStruct();

}
