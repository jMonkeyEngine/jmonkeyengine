package com.jme3.util.struct.fields;

import com.jme3.math.Vector2f;
import com.jme3.util.struct.StructField;

public class Vector2fField extends StructField<Vector2f> {

    public Vector2fField(int position, String name, Vector2f value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Vector2f getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
