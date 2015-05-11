/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.gde.scenecomposer.SceneComposerToolController;
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
    private SceneComposerToolController.TransformationType transformationType;

    protected static final Quaternion PLANE_XY = new Quaternion().fromAngleAxis(0, new Vector3f(1, 0, 0));
    protected static final Quaternion PLANE_YZ = new Quaternion().fromAngleAxis(-FastMath.PI / 2, new Vector3f(0, 1, 0));//YAW090
    protected static final Quaternion PLANE_XZ = new Quaternion().fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0)); //PITCH090


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

    public void initiatePick(Spatial selectedSpatial, Quaternion planeRotation, SceneComposerToolController.TransformationType type, Camera camera, Vector2f screenCoord) {
        spatial = selectedSpatial;
        startSpatialLocation = selectedSpatial.getWorldTranslation().clone();

        setTransformation(planeRotation, type, camera);
        plane.setLocalTranslation(startSpatialLocation);

        startPickLoc = SceneEditTool.pickWorldLocation(camera, screenCoord, plane, null);
    }

    public void setTransformation(Quaternion planeRotation, SceneComposerToolController.TransformationType type, Camera camera) {
        Quaternion rot = new Quaternion();
        transformationType = type;
        if (transformationType == SceneComposerToolController.TransformationType.local) {
            rot.set(spatial.getWorldRotation());
            rot.multLocal(planeRotation);
            origineRotation = spatial.getWorldRotation().clone();
        } else if (transformationType == SceneComposerToolController.TransformationType.global) {
            rot.set(planeRotation);
            origineRotation = new Quaternion(Quaternion.IDENTITY);
        } else if (transformationType == SceneComposerToolController.TransformationType.camera) {
            rot.set(camera.getRotation());  
            origineRotation = camera.getRotation().clone();
        }
        plane.setLocalRotation(rot);
    }
    
    /**
     * 
     * @param camera
     * @param screenCoord
     * @return true if the the new picked location is set, else return false.
     */
    public boolean updatePick(Camera camera, Vector2f screenCoord) {
        if(transformationType == SceneComposerToolController.TransformationType.camera){
            origineRotation = camera.getRotation();
            plane.setLocalRotation(camera.getRotation());
        }
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
        return getRotation(Quaternion.IDENTITY);
    }

    /**
     *
     * @return the Quaternion rotation in the ToolSpace
     */
    public Quaternion getLocalRotation() {
        return getRotation(origineRotation.inverse());
    }

    /**
     * Get the Rotation into a specific custom space.
     * @param transforme the rotation to the custom space (World to Custom space)
     * @return the Rotation in the custom space
     */
    public Quaternion getRotation(Quaternion transforme) {
        Vector3f v1, v2;
        v1 = transforme.mult(startPickLoc.subtract(startSpatialLocation).normalize());
        v2 = transforme.mult(finalPickLoc.subtract(startSpatialLocation).normalize());
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
        return getTranslation(origineRotation.inverse().mult(axisConstrainte));
    }
    
}
