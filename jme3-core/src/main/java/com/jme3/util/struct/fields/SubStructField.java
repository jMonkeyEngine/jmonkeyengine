package com.jme3.util.struct.fields;

import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;


public class SubStructField<T extends Struct> extends StructField<T> {

    public SubStructField(int position, String name, T value) {
        super(position, name, value);
    }

}
