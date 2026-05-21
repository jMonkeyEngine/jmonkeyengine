package com.jme3.vulkan.material.experimental;

import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.util.struct.Struct;

import java.util.List;

public class Globals implements ShadingGlobals {

    public static class GlobalsStruct extends Struct {

        public final Field<Matrix4f> viewProjection = new Field<>(new Matrix4f());
        public final Field<Vector3f> cameraPosition = new Field<>(new Vector3f());
        public final Field<Float> time = new Field<>(0f);
        public final Field<Vector3f> cameraDirection = new Field<>(new Vector3f());

    }

    public static class Lights extends Struct {

        public final List<>

    }

}
