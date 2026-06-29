package com.jme3.vulkan.material.experimental;

import com.jme3.backend.Engine;
import com.jme3.math.ColorRGBA;
import com.jme3.vulkan.buffer.BufferUsage;
import com.jme3.vulkan.buffer.DynamicallySizedBuffer;

public class MyCustomPBR extends PBR {

    public static class MyParams extends Params {

        public final Field<Float> emissionPower = new Field<>(0f);
        public final Field<ColorRGBA> emissive = new Field<>(new ColorRGBA());

        public MyParams() {
            super();
            addFields(emissionPower, emissive);
        }

    }

    public MyCustomPBR(Engine engine) {
        super(engine);
        materialData.addArrayIfAbsent(new MyParams(), new DynamicallySizedBuffer(n -> engine.createBuffer(n, BufferUsage.Vertex, UpdateHint.Dynamic)));

    }

}
