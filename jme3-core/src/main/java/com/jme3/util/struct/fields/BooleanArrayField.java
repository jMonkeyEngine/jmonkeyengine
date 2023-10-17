package com.jme3.util.struct.fields;

import com.jme3.util.struct.StructField;

public class BooleanArrayField extends StructField<Boolean[]> {

    public BooleanArrayField(int position, String name, Boolean[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public BooleanArrayField(int position, String name, int length) {
        super(position, name, new Boolean[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = false;
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Boolean[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
