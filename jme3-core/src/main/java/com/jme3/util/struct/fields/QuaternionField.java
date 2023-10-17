package com.jme3.util.struct.fields;

import com.jme3.math.Quaternion;
import com.jme3.util.struct.StructField;

public class QuaternionField extends StructField<Quaternion> {

    public QuaternionField(int position, String name, Quaternion value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public Quaternion getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
