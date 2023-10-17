package com.jme3.util.struct.fields;

import com.jme3.util.struct.StructField;

public class BooleanField extends StructField<Boolean> {

    public BooleanField(int position, String name, Boolean value) {
        super(position, name, value);
    }

    /**
     * Set value for this field and mark for update
     * 
     * @param value
     */
    public void setValue(Boolean value) {
        isUpdateNeeded = true;
        this.value = value;
    }
}
