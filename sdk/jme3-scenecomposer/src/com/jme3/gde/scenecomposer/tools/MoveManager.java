/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
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
 * @author Nehon
 */
@ServiceProvider(service = MoveManager.class)
public class MoveManager {

    private Vector3f startLoc;
    private Vector3f startWorldLoc;
    private Vector3f lastLoc;
    private Vector3f offset;
    private Node alternativePickTarget = null;
    private Node plane;
    private Spatial spatial;
    protected static final Quaternion XY = new Quaternion().fromAngleAxis(0, new Vector3f(1, 0, 0));
    protected static final Quaternion YZ = new Quaternion().fromAngleAxis(-FastMath.PI / 2, new Vector3f(0, 1, 0));
    protected static final Quaternion XZ = new Quaternion().fromAngleAxis(FastMath.PI / 2, new Vector3f(1, 0, 0));
    //temp vars 
    private Quaternion rot = new Quaternion();
    private Vector3f newPos = new Vector3f();

    public MoveManager() {
        float size = 1000;
        Geometry g = new Geometry("plane", new Quad(size, size));
        g.setLocalTranslation(-size / 2, -size / 2, 0);
        plane = new Node();
        plane.attachChild(g);
    }

    public Vector3f getOffset() {
        return offset;
    }

    public void reset() {
        offset = null;
        startLoc = null;
        startWorldLoc = null;
        lastLoc = null;
        spatial = null;
        alternativePickTarget = null;
    }

    public void initiateMove(Spatial selectedSpatial, Quaternion planeRotation, boolean local) {
        spatial = selectedSpatial;
        startLoc = selectedSpatial.getLocalTranslation().clone();
        startWorldLoc = selectedSpatial.getWorldTranslation().clone();
        if (local) {
            rot.set(selectedSpatial.getWorldRotation());
            plane.setLocalRotation(rot.multLocal(planeRotation));
        } else {
            rot.set(planeRotation);
        }

        plane.setLocalRotation(rot);
        plane.setLocalTranslation(startWorldLoc);

    }

    public void updatePlaneRotation(Quaternion planeRotation) {
        plane.setLocalRotation(rot);
    }

    public boolean move(Camera camera, Vector2f screenCoord) {
        return move(camera, screenCoord, Vector3f.UNIT_XYZ, false);
    }

    public boolean move(Camera camera, Vector2f screenCoord, Vector3f constraintAxis, boolean gridSnap) {
        Node toPick = alternativePickTarget == null ? plane : alternativePickTarget;

        Vector3f planeHit = SceneEditTool.pickWorldLocation(camera, screenCoord, toPick, alternativePickTarget == null ? null : spatial);
        if (planeHit == null) {
            return false;
        }

        Spatial parent = spatial.getParent();
        //we are moving the root node, there is a slight chance that something went wrong.
        if (parent == null) {
            return false;
        }

        //offset in world space
        if (offset == null) {
            offset = planeHit.subtract(spatial.getWorldTranslation()); // get the offset when we start so it doesn't jump
        }

        newPos.set(planeHit).subtractLocal(offset);

        //constraining the translation with the contraintAxis.
        Vector3f tmp = startWorldLoc.mult(Vector3f.UNIT_XYZ.subtract(constraintAxis));
        newPos.multLocal(constraintAxis).addLocal(tmp);
        worldToLocalMove(gridSnap);
        return true;
    }

    private void worldToLocalMove(boolean gridSnap) {
        //snap to grid (grid is assumed 1 WU per cell)
        if (gridSnap) {
            newPos.set(Math.round(newPos.x), Math.round(newPos.y), Math.round(newPos.z));
        }

        //computing the inverse world transform to get the new localtranslation        
        newPos.subtractLocal(spatial.getParent().getWorldTranslation());
        newPos = spatial.getParent().getWorldRotation().inverse().normalizeLocal().multLocal(newPos);
        newPos.divideLocal(spatial.getParent().getWorldScale());
       
        lastLoc = newPos;
        spatial.setLocalTranslation(newPos);        

        RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
        if (control != null) {
            control.setPhysicsLocation(spatial.getWorldTranslation());
        }
        CharacterControl character = spatial.getControl(CharacterControl.class);
        if (character != null) {
            character.setPhysicsLocation(spatial.getWorldTranslation());
        }
    }

    public boolean moveAcross(Vector3f constraintAxis, float value, boolean gridSnap) {
        newPos.set(startWorldLoc).addLocal(constraintAxis.mult(value));
        Spatial parent = spatial.getParent();
        //we are moving the root node, there is a slight chance that something went wrong.
        if (parent == null) {
            return false;
        }
        worldToLocalMove(gridSnap);
        return true;
    }

    public MoveUndo makeUndo() {
        return new MoveUndo(spatial, startLoc, lastLoc);
    }

    public void setAlternativePickTarget(Node alternativePickTarget) {
        this.alternativePickTarget = alternativePickTarget;
    }

    protected class MoveUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Vector3f before = new Vector3f(), after = new Vector3f();

        MoveUndo(Spatial spatial, Vector3f before, Vector3f after) {
            this.spatial = spatial;
            this.before.set(before);
            if (after != null) {
                this.after.set(after);
            }
        }

        @Override
        public void sceneUndo() {
            spatial.setLocalTranslation(before);
            RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
            if (control != null) {
                control.setPhysicsLocation(spatial.getWorldTranslation());
            }
            CharacterControl character = spatial.getControl(CharacterControl.class);
            if (character != null) {
                character.setPhysicsLocation(spatial.getWorldTranslation());
            }
            //     toolController.selectedSpatialTransformed();
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalTranslation(after);
            RigidBodyControl control = spatial.getControl(RigidBodyControl.class);
            if (control != null) {
                control.setPhysicsLocation(spatial.getWorldTranslation());
            }
            CharacterControl character = spatial.getControl(CharacterControl.class);
            if (character != null) {
                character.setPhysicsLocation(spatial.getWorldTranslation());
            }
            //toolController.selectedSpatialTransformed();
        }

        public void setAfter(Vector3f after) {
            this.after.set(after);
        }
    }
}
