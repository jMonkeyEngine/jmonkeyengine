/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author sploreg
 */
public class ScaleTool extends SceneEditTool {

    private Vector3f pickedMarker;
    private Vector3f constraintAxis; //used for one axis scale
    private Vector3f startScale;
    private Vector3f lastScale;
    private boolean wasDragging = false;
    private PickManager pickManager;

    public ScaleTool() {
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
            constraintAxis = Vector3f.UNIT_XYZ; // no axis constraint
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startScale, lastScale));
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
            highlightAxisMarker(camera, screenCoord, axisPickType, true);
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
            constraintAxis = Vector3f.UNIT_XYZ; // no axis constraint
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startScale, lastScale));
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

            if (pickedMarker.equals(QUAD_XY) || pickedMarker.equals(QUAD_XZ) || pickedMarker.equals(QUAD_YZ)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), camera.getRotation(), 
                        SceneComposerToolController.TransformationType.camera, camera, screenCoord);
            } else if (pickedMarker.equals(ARROW_X)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XY, getTransformType(), camera, screenCoord);
                constraintAxis = Vector3f.UNIT_X; // scale only X
            } else if (pickedMarker.equals(ARROW_Y)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_YZ, getTransformType(), camera, screenCoord);
                constraintAxis = Vector3f.UNIT_Y; // scale only Y
            } else if (pickedMarker.equals(ARROW_Z)) {
                pickManager.initiatePick(toolController.getSelectedSpatial(), PickManager.PLANE_XZ, getTransformType(), camera, screenCoord);
                constraintAxis = Vector3f.UNIT_Z; // scale only Z
            }
            startScale = toolController.getSelectedSpatial().getLocalScale().clone();
        }

        if (!pickManager.updatePick(camera, screenCoord)) {
            return;
        }
        if (pickedMarker.equals(QUAD_XY) || pickedMarker.equals(QUAD_XZ) || pickedMarker.equals(QUAD_YZ)) {
            constraintAxis = pickManager.getStartOffset().normalize();
            float diff = pickManager.getTranslation(constraintAxis).dot(constraintAxis);
            diff *= 0.5f;
            Vector3f scale = startScale.add(new Vector3f(diff, diff, diff));
            lastScale = scale;
            toolController.getSelectedSpatial().setLocalScale(scale);
        } else if (pickedMarker.equals(ARROW_X) || pickedMarker.equals(ARROW_Y) || pickedMarker.equals(ARROW_Z)) {
            // Get the translation in the spatial Space
            Quaternion worldToSpatial = toolController.getSelectedSpatial().getWorldRotation().inverse();
            Vector3f diff = pickManager.getTranslation(worldToSpatial.mult(constraintAxis));
            diff.multLocal(0.5f);
            Vector3f scale = startScale.add(diff);
            lastScale = scale;
            toolController.getSelectedSpatial().setLocalScale(scale);
        }
        updateToolsTransformation();

        wasDragging = true;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {

    }

    private class ScaleUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Vector3f before, after;

        ScaleUndo(Spatial spatial, Vector3f before, Vector3f after) {
            this.spatial = spatial;
            this.before = before;
            this.after = after;
        }

        @Override
        public void sceneUndo() {
            spatial.setLocalScale(before);
            toolController.selectedSpatialTransformed();
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalScale(after);
            toolController.selectedSpatialTransformed();
        }

    }
}
