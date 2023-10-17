package com.jme3.util.struct.fields;

import com.jme3.math.Quaternion;
import com.jme3.util.struct.StructField;

public class QuaternionArrayField extends StructField<Quaternion[]> {

    public QuaternionArrayField(int position, String name, Quaternion[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public QuaternionArrayField(int position, String name, int length) {
        super(position, name, new Quaternion[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new Quaternion();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Quaternion[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
