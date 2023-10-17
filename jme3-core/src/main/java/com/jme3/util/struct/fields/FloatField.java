package com.jme3.util.struct.fields;

import com.jme3.util.struct.StructField;

public class FloatField extends StructField<Float> {

    public FloatField(int position, String name, Float value) {
        super(position, name, value);
    }

    /**
     * Set value for this field and mark for update
     * 
     * @param value
     */
    public void setValue(Float value) {
        isUpdateNeeded = true;
        this.value = value;
    }
}
