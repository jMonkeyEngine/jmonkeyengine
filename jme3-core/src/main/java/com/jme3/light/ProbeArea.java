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
     * @param box the BoundingBox to test for intersection
     * @param vars storage for temporary data
     * @return true if the area and the box intersect, otherwise false
     * @see Light#intersectsBox(BoundingBox, TempVars)
     */
    public boolean intersectsBox(BoundingBox box, TempVars vars);

    /**
     * @param sphere the BoundingSphere to test for intersection
     * @param vars storage for temporary data
     * @return true if the area and the sphere intersect, otherwise false
     * @see Light#intersectsSphere(BoundingSphere, TempVars)
     */
    public boolean intersectsSphere(BoundingSphere sphere, TempVars vars);

    /**
     * @param camera the Camera whose frustum will be tested for intersection
     * @param vars storage for temporary data
     * @return true if the area and the frustum intersect, otherwise false
     * @see Light#intersectsFrustum(Camera, TempVars)
     */
    public abstract boolean intersectsFrustum(Camera camera, TempVars vars);
}
