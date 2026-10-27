package com.jme3.util.struct;

import java.util.function.IntFunction;

public class FieldArray <E extends StructField> {

    private final StructArray<Struct<StructField>> array;
    private final IntFunction<E> field;

    public FieldArray(StructArray<Struct<StructField>> array, IntFunction<E> field) {
        this.array = array;
        this.field = field;
    }

    public StructArray<Struct<StructField>> getStructArray() {
        return array;
    }

    public E index(int i) {
        return field.apply(i);
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
        Struct<StructField> struct = array.getStruct();
        return struct.getFieldOffset(field.apply(0));
    }

}
