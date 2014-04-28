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
 
/**
 *
 * @author kbender
 */
public class RotateTool extends SceneEditTool {
 
    private Vector3f pickedPlane;
    private Vector2f lastScreenCoord;
    private Quaternion startRotate;
    private Quaternion lastRotate;
    private boolean wasDragging = false;
 
    public RotateTool() {
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
            pickedPlane = null; // mouse released, reset selection
            lastScreenCoord = null;
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startRotate, lastRotate));
                wasDragging = false;
            }
        }
    }
 
    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
 
    }
 
    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject currentDataObject, JmeSpatial selectedSpatial) {
        if (pickedPlane == null) {
            highlightAxisMarker(camera, screenCoord, axisPickType);
        }
        else {
            pickedPlane = null;
        }
    }
 
    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
 
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedPlane = null; // mouse released, reset selection
            lastScreenCoord = null;
 
            if (wasDragging) {
                actionPerformed(new ScaleUndo(toolController.getSelectedSpatial(), startRotate, lastRotate));
                wasDragging = false;
            }
            return;
        }
 
        if (toolController.getSelectedSpatial() == null)
        {
            return;
        }
 
        if (pickedPlane == null)
        {
            pickedPlane = pickAxisMarker(camera, screenCoord, axisPickType);
            if (pickedPlane == null)
            {
                return;
            }
            startRotate = toolController.getSelectedSpatial().getLocalRotation().clone();
        }
 
        if (lastScreenCoord == null) {
            lastScreenCoord = screenCoord;
        } else {
            Quaternion rotate = new Quaternion();
            float diff;
            if(pickedPlane.equals(QUAD_XY))
            {
                diff = -(screenCoord.x-lastScreenCoord.x);
                diff *= 0.03f;
                rotate = rotate.fromAngleAxis(diff, Vector3f.UNIT_Z);
            }
            else if(pickedPlane.equals(QUAD_YZ))
            {
                diff = -(screenCoord.y-lastScreenCoord.y);
                diff *= 0.03f;
                rotate = rotate.fromAngleAxis(diff, Vector3f.UNIT_X);
            }
            else if(pickedPlane.equals(QUAD_XZ))
            {
                diff = screenCoord.x-lastScreenCoord.x;
                diff *= 0.03f;
                rotate = rotate.fromAngleAxis(diff, Vector3f.UNIT_Y);
            }
 
            lastScreenCoord = screenCoord;
            Quaternion rotation = toolController.getSelectedSpatial().getLocalRotation().mult(rotate);
            lastRotate = rotation;
            toolController.getSelectedSpatial().setLocalRotation(rotation);
            updateToolsTransformation();
        }
 
        wasDragging = true;
    }
 
    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
 
    }
 
    private class ScaleUndo extends AbstractUndoableSceneEdit {
 
        private Spatial spatial;
        private Quaternion before,after;
 
        ScaleUndo(Spatial spatial, Quaternion before, Quaternion after) {
            this.spatial = spatial;
            this.before = before;
            this.after = after;
        }
 
        @Override
        public void sceneUndo() {
            spatial.setLocalRotation(before);
            toolController.selectedSpatialTransformed();
        }
 
        @Override
        public void sceneRedo() {
            spatial.setLocalRotation(after);
            toolController.selectedSpatialTransformed();
        }
    }
}
