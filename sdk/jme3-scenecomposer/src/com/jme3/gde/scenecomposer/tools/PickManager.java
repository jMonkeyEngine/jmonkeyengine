/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author dokthar
 */
@ServiceProvider(service = PickManager.class)
public class PickManager {

    private Vector3f startPickLoc;
    private Vector3f finalPickLoc;
    private Vector3f startSpatialLocation;
    private Quaternion origineRotation;
    private final Node plane;
    private Spatial spatial;

    protected static final Quaternion PLANE_XY = new Quaternion().fromAngleAxis(0, new Vector3f(1, 0, 0));
    protected static final Quaternion PLANE_YZ = new Quaternion().fromAngleAxis(-FastMath.PI / 2, new Vector3f(0, 1, 0));//YAW090
    protected static final Quaternion PLANE_XZ = new Quaternion().fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0)); //PITCH090

    public enum TransformationType {

        local, global, camera
    }

    public PickManager() {
        float size = 1000;
        Geometry g = new Geometry("plane", new Quad(size, size));
        g.setLocalTranslation(-size / 2, -size / 2, 0);
        plane = new Node();
        plane.attachChild(g);
    }

    public void reset() {
        startPickLoc = null;
        finalPickLoc = null;
        startSpatialLocation = null;
        spatial = null;
    }

    public void initiatePick(Spatial selectedSpatial, Quaternion planeRotation, TransformationType type, Camera camera, Vector2f screenCoord) {
        spatial = selectedSpatial;
        startSpatialLocation = selectedSpatial.getWorldTranslation().clone();

        setTransformation(planeRotation, type);
        plane.setLocalTranslation(startSpatialLocation);

        startPickLoc = SceneEditTool.pickWorldLocation(camera, screenCoord, plane, null);
    }

    public void setTransformation(Quaternion planeRotation, TransformationType type) {
        Quaternion rot = new Quaternion();
        if (type == TransformationType.local) {
            rot.set(spatial.getWorldRotation());
            rot.multLocal(planeRotation);
            origineRotation = spatial.getWorldRotation().clone();
        } else if (type == TransformationType.global) {
            rot.set(planeRotation);
            origineRotation = new Quaternion(Quaternion.IDENTITY);
        } else if (type == TransformationType.camera) {
            rot.set(planeRotation);
            origineRotation = planeRotation.clone();
        }
        plane.setLocalRotation(rot);
    }

    public boolean updatePick(Camera camera, Vector2f screenCoord) {
        finalPickLoc = SceneEditTool.pickWorldLocation(camera, screenCoord, plane, null);
        return finalPickLoc != null;
    }

    /**
     *
     * @return the start location in WorldSpace
     */
    public Vector3f getStartLocation() {
        return startSpatialLocation;
    }

    /**
     *
     * @return the vector from the tool origin to the start location, in
     * WorldSpace
     */
    public Vector3f getStartOffset() {
        return startPickLoc.subtract(startSpatialLocation);
    }

    /**
     *
     * @return the vector from the tool origin to the final location, in
     * WorldSpace
     */
    public Vector3f getFinalOffset() {
        return finalPickLoc.subtract(startSpatialLocation);
    }

    /**
     *
     * @return the angle between the start location and the final location
     */
    public float getAngle() {
        Vector3f v1, v2;
        v1 = startPickLoc.subtract(startSpatialLocation);
        v2 = finalPickLoc.subtract(startSpatialLocation);
        return v1.angleBetween(v2);
    }

    /**
     *
     * @return the Quaternion rotation in the WorldSpace
     */
    public Quaternion getRotation() {
        Vector3f v1, v2;
        v1 = startPickLoc.subtract(startSpatialLocation).normalize();
        v2 = finalPickLoc.subtract(startSpatialLocation).normalize();
        Vector3f axis = v1.cross(v2);
        float angle = v1.angleBetween(v2);
        return new Quaternion().fromAngleAxis(angle, axis);
    }

    /**
     * 
     * @return the Quaternion rotation in the ToolSpace
     */
    public Quaternion getLocalRotation() {
        Vector3f v1, v2;
        Quaternion rot = origineRotation.inverse();
        v1 = rot.mult(startPickLoc.subtract(startSpatialLocation).normalize());
        v2 = rot.mult(finalPickLoc.subtract(startSpatialLocation).normalize());
        Vector3f axis = v1.cross(v2);
        float angle = v1.angleBetween(v2);
        return new Quaternion().fromAngleAxis(angle, axis);
    }

    /**
     *
     * @return the translation in WorldSpace
     */
    public Vector3f getTranslation() {
        return finalPickLoc.subtract(startPickLoc);
    }

    /**
     *
     * @param axisConstrainte
     * @return
     */
    public Vector3f getTranslation(Vector3f axisConstrainte) {
        Vector3f localConstrainte = (origineRotation.mult(axisConstrainte)).normalize(); // according to the "plane" rotation
        Vector3f constrainedTranslation = localConstrainte.mult(getTranslation().dot(localConstrainte));
        return constrainedTranslation;
    }

    /**
     *
     * @param axisConstrainte
     * @return
     */
    public Vector3f getLocalTranslation(Vector3f axisConstrainte) {
        //return plane.getWorldRotation().inverse().mult(getTranslation(axisConstrainte));
        return getTranslation(origineRotation.inverse().mult(axisConstrainte));
    }

}
