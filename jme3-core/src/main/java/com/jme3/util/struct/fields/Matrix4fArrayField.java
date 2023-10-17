package com.jme3.util.struct.fields;

import com.jme3.math.Matrix4f;
import com.jme3.util.struct.StructField;

public class Matrix4fArrayField extends StructField<Matrix4f[]> {

    public Matrix4fArrayField(int position, String name, Matrix4f[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public Matrix4fArrayField(int position, String name, int length) {
        super(position, name, new Matrix4f[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new Matrix4f();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Matrix4f[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
