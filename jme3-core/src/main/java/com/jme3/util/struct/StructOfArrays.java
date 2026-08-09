package com.jme3.util.struct;

public class StructOfArrays <T extends ArrayField> extends Struct<T> {

    private final int length;

    public StructOfArrays(int length) {
        this.length = length;
    }

    public int length() {
        return length;
    }

}
