package com.jme3.util.struct.fields;

import com.jme3.util.struct.StructField;

public class IntField extends StructField<Integer> {

    public IntField(int position, String name, Integer value) {
        super(position, name, value);
    }

    /**
     * Set value for this field and mark for update
     * 
     * @param value
     */
    public void setValue(Integer value) {
        isUpdateNeeded = true;
        this.value = value;
    }
}
