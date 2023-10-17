package com.jme3.util.struct.fields;

import com.jme3.math.Vector3f;
import com.jme3.util.struct.StructField;

public class Vector3fField extends StructField<Vector3f> {

    public Vector3fField(int position, String name, Vector3f value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Vector3f getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
