package com.jme3.util.struct;

import java.util.function.Function;

public interface StructArray <T extends Struct<StructField>> extends StructField, Iterable<T> {

    T index(int i);

    T index(int i, T struct);

    int length();

    int stride();

    T getStruct();

    default <E extends StructField> FieldArray<T, E> getField(Function<T, E> field) {
        return new FieldArray<>(this, field);
    }V

}
