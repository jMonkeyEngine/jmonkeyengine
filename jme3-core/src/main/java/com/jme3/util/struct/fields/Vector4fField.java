package com.jme3.util.struct.fields;

import com.jme3.math.Vector4f;
import com.jme3.util.struct.StructField;

public class Vector4fField extends StructField<Vector4f> {

    public Vector4fField(int position, String name, Vector4f value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Vector4f getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
