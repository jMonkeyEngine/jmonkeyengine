package com.jme3.vulkan.material.structs;

import com.jme3.math.ColorRGBA;
import com.jme3.util.struct.Struct;

public class UnshadedParams extends Struct {

    public final Field<ColorRGBA> color = new Field<>(ColorRGBA.White.clone());
    public final Field<ColorRGBA> glowColor = new Field<>(ColorRGBA.BlackNoAlpha.clone());
    public final Field<Boolean> vertexColor = new Field<>(false);
    public final Field<Float> pointSize = new Field<>(1.0f);
    public final Field<Boolean> seperateTexCoord = new Field<>(false);
    public final Field<Boolean> useInstancing = new Field<>(false);
    public final Field<Float> alphaDiscardThreshold = new Field<>(0.0f);
    public final Field<Float> desaturation = new Field<>(0.0f);

    public UnshadedParams() {
        addFields(color, glowColor, vertexColor, pointSize, seperateTexCoord,
                  useInstancing, alphaDiscardThreshold, desaturation);
    }

}
