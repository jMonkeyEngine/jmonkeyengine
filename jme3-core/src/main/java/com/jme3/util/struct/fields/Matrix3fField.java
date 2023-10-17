package com.jme3.util.struct.fields;

import com.jme3.math.Matrix3f;
import com.jme3.util.struct.StructField;

public class Matrix3fField extends StructField<Matrix3f> {

    public Matrix3fField(int position, String name, Matrix3f value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Matrix3f getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
