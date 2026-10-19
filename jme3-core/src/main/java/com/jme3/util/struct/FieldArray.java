package com.jme3.util.struct;

import java.util.function.Function;

public class FieldArray <T extends Struct<StructField>, E extends StructField> {

    private final StructArray<T> array;
    private final Function<T, E> field;

    public FieldArray(StructArray<T> array, Function<T, E> field) {
        this.array = array;
        this.field = field;
    }

    public StructArray<T> getStructArray() {
        return array;
    }

    public T getStruct() {
        return array.getStruct();
    }

    public E index(int i) {
        return field.apply(array.index(i));
    }

    public E index(int i, T struct) {
        return field.apply(array.index(i, struct));
    }

    public int length() {
        return array.length();
    }

    /**
     * Gets the byte offset of this array's field within one element of the underlying
     * {@link #getStructArray()}, via {@link Struct#getFieldOffset}.
     *
     * @return byte offset of this array's field within its struct
     */
    public int offset() {
        T struct = getStruct();
        return struct.getFieldOffset(field.apply(struct));
    }

}
