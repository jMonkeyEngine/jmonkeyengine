package com.jme3.util.struct.fields;

import com.jme3.math.Matrix4f;
import com.jme3.util.struct.StructField;

public class Matrix4fField extends StructField<Matrix4f> {

    public Matrix4fField(int position, String name, Matrix4f value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Matrix4f getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
