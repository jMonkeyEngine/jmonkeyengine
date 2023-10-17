package com.jme3.util.struct.fields;

import com.jme3.math.Vector2f;
import com.jme3.util.struct.StructField;

public class Vector2fArrayField extends StructField<Vector2f[]> {

    public Vector2fArrayField(int position, String name, Vector2f[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public Vector2fArrayField(int position, String name, int length) {
        super(position, name, new Vector2f[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new Vector2f();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Vector2f[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
