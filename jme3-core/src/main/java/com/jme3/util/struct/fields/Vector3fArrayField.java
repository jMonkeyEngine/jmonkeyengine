package com.jme3.util.struct.fields;

import com.jme3.math.Vector3f;
import com.jme3.util.struct.StructField;

public class Vector3fArrayField extends StructField<Vector3f[]> {

    public Vector3fArrayField(int position, String name, Vector3f[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public Vector3fArrayField(int position, String name, int length) {
        super(position, name, new Vector3f[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new Vector3f();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Vector3f[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
