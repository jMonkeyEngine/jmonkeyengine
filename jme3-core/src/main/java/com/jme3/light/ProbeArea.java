package com.jme3.light;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.export.Savable;
import com.jme3.math.Matrix4f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.util.TempVars;

public interface ProbeArea extends Savable, Cloneable{

    public void setCenter(Vector3f center);

    public float getRadius();

    public void setRadius(float radius);

    public Matrix4f getUniformMatrix();

    /**
     * @see Light#intersectsBox(BoundingBox, TempVars)
     */
    public boolean intersectsBox(BoundingBox box, TempVars vars);

    /**
     * @see Light#intersectsSphere(BoundingSphere, TempVars)
     */
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars);

    /**
     * @see Light#intersectsFrustum(Camera, TempVars)
     */
    public abstract boolean intersectsFrustum(Camera camera, TempVars vars);
}
