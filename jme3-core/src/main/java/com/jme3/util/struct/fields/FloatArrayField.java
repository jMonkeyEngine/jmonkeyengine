package com.jme3.util.struct.fields;

import com.jme3.util.struct.StructField;

public class FloatArrayField extends StructField<Float[]> {

    public FloatArrayField(int position, String name, Float[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public FloatArrayField(int position, String name, int length) {
        super(position, name, new Float[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = 0f;
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Float[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
