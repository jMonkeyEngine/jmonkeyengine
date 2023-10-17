package com.jme3.util.struct.fields;

import com.jme3.math.Matrix3f;
import com.jme3.util.struct.StructField;

public class Matrix3fArrayField extends StructField<Matrix3f[]> {

    public Matrix3fArrayField(int position, String name, Matrix3f[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public Matrix3fArrayField(int position, String name, int length) {
        super(position, name, new Matrix3f[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new Matrix3f();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Matrix3f[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
