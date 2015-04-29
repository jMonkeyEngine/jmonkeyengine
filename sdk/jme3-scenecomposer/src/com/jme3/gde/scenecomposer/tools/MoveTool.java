/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 * Move an object. When created, it generates a quad that will lie along a plane
 * that the user selects for moving on. When the mouse is over the axisMarker,
 * it will highlight the plane that it is over: XY,XZ,YZ. When clicked and then
 * dragged, the selected object will move along that plane.
 *
 * @author Brent Owens
 */
public class MoveTool extends SceneEditTool {

    private Vector3f pickedMarker;
    private Vector3f constraintAxis; //used for one axis move
    private boolean wasDragging = false;
    private Vector3f startPosition;
    private Vector3f lastPosition;
    private PickManager pickManager;

    public MoveTool() {
        axisPickType = AxisMarkerPickType.axisAndPlane;
        setOverrideCameraControl(true);

    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController);
        pickManager = Lookup.getDefault().lookup(PickManager.class);
        displayPlanes();
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedMarker = null; // mouse released, reset selection
            constraintAxis = Vector3f.UNIT_XYZ; // no constraint
            if (wasDragging) {
                actionPerformed(new MoveUndo(toolController.getSelectedSpatial(), startPosition, lastPosition));
                wasDragging = false;
            }
            pickManager.reset();
        }
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject currentDataObject, JmeSpatial selectedSpatial) {

        if (pickedMarker == null) {
            highlightAxisMarker(camera, screenCoord, axisPickType);
        } else {
            pickedMarker = null;
            pickManager.reset();
        }
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedMarker = null; // mouse released, reset selection
            constraintAxis = Vector3f.UNIT_XYZ; // no constraint
            if (wasDragging) {
                actionPerformed(new MoveUndo(toolController.getSelectedSpatial(), startPosition, lastPosition));
                wasDragging = false;
            }
            pickManager.reset();
            return;
        }

        if (toolController.getSelectedSpatial() == null) {
            return;
        }

        if (pickedMarker == null) {
            pickedMarker = pickAxisMarker(camera, screenCoord, axisPickType);
            if (pickedMarker == null) {
                return;
            }

            if (pickedMarker.equals(QUAD_XY)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XY, getTransformType(), camera, screenCoord);
            } else if (pickedMarker.equals(QUAD_XZ)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XZ, getTransformType(), camera, screenCoord);
            } else if (pickedMarker.equals(QUAD_YZ)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ, getTransformType(), camera, screenCoord);
            } else if (pickedMarker.equals(ARROW_X)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XY, getTransformType(), camera, screenCoord);
                constraintAxis = Vector3f.UNIT_X; // move only X
            } else if (pickedMarker.equals(ARROW_Y)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ, getTransformType(), camera, screenCoord);
                constraintAxis = Vector3f.UNIT_Y; // move only Y
            } else if (pickedMarker.equals(ARROW_Z)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XZ, getTransformType(), camera, screenCoord);
                constraintAxis = Vector3f.UNIT_Z; // move only Z
            }
            startPosition = toolController.getSelectedSpatial().getLocalTranslation().clone();

        }
        if (!pickManager.updatePick(camera, screenCoord)) {
            return;
        }
        Vector3f diff = Vector3f.ZERO;
        if (pickedMarker.equals(QUAD_XY) || pickedMarker.equals(QUAD_XZ) || pickedMarker.equals(QUAD_YZ)) {
            diff = pickManager.getTranslation();

        } else if (pickedMarker.equals(ARROW_X) || pickedMarker.equals(ARROW_Y) || pickedMarker.equals(ARROW_Z)) {
            diff = pickManager.getTranslation(constraintAxis);
        }
        Vector3f position = startPosition.add(diff);
        lastPosition = position;
        toolController.getSelectedSpatial().setLocalTranslation(position);
        updateToolsTransformation();

        wasDragging = true;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
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
