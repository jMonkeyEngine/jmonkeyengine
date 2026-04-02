package com.jme3.vulkan.material.structs;

import com.jme3.math.ColorRGBA;
import com.jme3.util.struct.Struct;
import com.jme3.util.struct.StructField;

public class UnshadedParams extends Struct {

    public final StructField<ColorRGBA> color = new Field<>(new ColorRGBA());
    public final StructField<ColorRGBA> glowColor = new Field<>(new ColorRGBA());
    public final StructField<Boolean> vertexColor = new Field<>(false);
    public final StructField<Float> pointSize = new Field<>(1.0f);
    public final StructField<Boolean> seperateTexCoord = new Field<>(false);
    public final StructField<Boolean> useInstancing = new Field<>(false);
    public final StructField<Float> alphaDiscardThreshold = new Field<>(0.0f);
    public final StructField<Float> desaturation = new Field<>(0.0f);

    public UnshadedParams() {
        addFields(color, glowColor, vertexColor, pointSize, seperateTexCoord,
                  useInstancing, alphaDiscardThreshold, desaturation);
    }

}
