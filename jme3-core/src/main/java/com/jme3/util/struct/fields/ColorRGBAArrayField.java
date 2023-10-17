package com.jme3.util.struct.fields;

import com.jme3.math.ColorRGBA;
import com.jme3.util.struct.StructField;

public class ColorRGBAArrayField extends StructField<ColorRGBA[]> {

    public ColorRGBAArrayField(int position, String name, ColorRGBA[] value) {
        super(position, name, value);
        initializeToZero();
    }

    public ColorRGBAArrayField(int position, String name, int length) {
        super(position, name, new ColorRGBA[length]);
        initializeToZero();
    }

    private void initializeToZero() {
        for (int i = 0; i < value.length; i++) {
            if (value[i] == null) value[i] = new ColorRGBA();
        }
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public ColorRGBA[] getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
