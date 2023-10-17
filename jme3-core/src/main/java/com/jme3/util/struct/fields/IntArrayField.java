package com.jme3.util.struct.fields;

import com.jme3.util.struct.StructField;

public class IntArrayField extends StructField<Integer[]> {

    public IntArrayField(int position, String name, Integer[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public IntArrayField(int position, String name, Integer length) {
        super(position, name, new Integer[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = 0;
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Integer[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
