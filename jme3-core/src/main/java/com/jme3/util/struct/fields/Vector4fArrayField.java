package com.jme3.util.struct.fields;

import com.jme3.math.Vector4f;
import com.jme3.util.struct.StructField;

public class Vector4fArrayField extends StructField<Vector4f[]> {
    
    public Vector4fArrayField(int position, String name, Vector4f[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public Vector4fArrayField(int position, String name, int length) {
        super(position, name, new Vector4f[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new Vector4f();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Vector4f[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
