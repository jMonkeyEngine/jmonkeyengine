package com.jme3.light;

import com.jme3.scene.Spatial;

public class AmbientLight extends Light {

    @Override
    public void computeLastDistance(Spatial owner) {
    }

    @Override
    public Type getType() {
        return Type.Ambient;
    }

}
