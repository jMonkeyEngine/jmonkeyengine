package com.jme3.util.struct.fields;

import com.jme3.math.ColorRGBA;
import com.jme3.util.struct.StructField;

public class ColorRGBAField extends StructField<ColorRGBA> {

    public ColorRGBAField(int position, String name, ColorRGBA value) {
        super(position, name, value);
    }

    /**
     * Get value and mark field for update
     * 
     * @return
     */
    public ColorRGBA getValueForUpdate() {
        isUpdateNeeded = true;
        return value;
    }
}
