package com.jme3.light;

import com.jme3.scene.Spatial;

/**
 * An ambient light adds a constant color to the scene.
 * <p>
 * Ambient lights are unaffected by the surface normal, and are constant
 * regardless of the model's location. The material's ambient color is
 * multiplied by the ambient light color to get the final ambient color of
 * an object.
 * 
 * @author Kirill Vainer
 */
public class AmbientLight extends Light {

    @Override
    public void computeLastDistance(Spatial owner) {
    }

    @Override
    public Type getType() {
        return Type.Ambient;
    }

}
