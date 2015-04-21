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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;

/**
 *
 * @author sploreg
 */
public class ScaleTool extends SceneEditTool {

    private Vector3f pickedMarker;
    private Vector3f constraintAxis; //used for one axis scale
    private Vector2f lastScreenCoord;
    private Vector3f startScale;
    private Vector3f lastScale;
    private boolean wasDragging = false;

    public ScaleTool() {
        axisPickType = AxisMarkerPickType.axisAndPlane;
        setOverrideCameraControl(true);
    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController);
        displayPlanes();
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedMarker = null; // mouse released, reset selection
            lastScreenCoord = null;
            constraintAxis = Vector3f.UNIT_XYZ; // no axis constraint
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startScale, lastScale));
                wasDragging = false;
            }
        }
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {

    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject currentDataObject, JmeSpatial selectedSpatial) {
        if (pickedMarker == null) {
            highlightAxisMarker(camera, screenCoord, axisPickType, true);
        }
        /*else {
         pickedPlane = null;
         lastScreenCoord = null;
         }*/
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedMarker = null; // mouse released, reset selection
            lastScreenCoord = null;
            constraintAxis = Vector3f.UNIT_XYZ; // no axis constraint
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startScale, lastScale));
                wasDragging = false;
            }
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
            if (pickedMarker.equals(ARROW_X)) {
                constraintAxis = Vector3f.UNIT_X; // scale only X
            } else if (pickedMarker.equals(ARROW_Y)) {
                constraintAxis = Vector3f.UNIT_Y; // scale only Y
            } else if (pickedMarker.equals(ARROW_Z)) {
                constraintAxis = Vector3f.UNIT_Z; // scale only Z
            }
            startScale = toolController.getSelectedSpatial().getLocalScale().clone();
        }

        if (lastScreenCoord == null) {
            lastScreenCoord = screenCoord;
        } else {
            float diff = screenCoord.y - lastScreenCoord.y;
            diff *= 0.1f;
            lastScreenCoord = screenCoord;
            Vector3f scale = toolController.getSelectedSpatial().getLocalScale().add(new Vector3f(diff, diff, diff).multLocal(constraintAxis));
            lastScale = scale;
            toolController.getSelectedSpatial().setLocalScale(scale);
            updateToolsTransformation();
        }

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
