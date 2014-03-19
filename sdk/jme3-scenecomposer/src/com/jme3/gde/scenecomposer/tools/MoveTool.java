/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.asset.AssetManager;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.sceneexplorer.nodes.JmeSpatial;
import com.jme3.gde.scenecomposer.SceneComposerToolController;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 * Move an object.
 * When created, it generates a quad that will lie along a plane
 * that the user selects for moving on. When the mouse is over
 * the axisMarker, it will highlight the plane that it is over: XY,XZ,YZ.
 * When clicked and then dragged, the selected object will move along that 
 * plane.
 * @author Brent Owens
 */
public class MoveTool extends SceneEditTool {

    private Vector3f pickedPlane;
    private boolean wasDragging = false;
    private MoveManager moveManager;

    public MoveTool() {
        axisPickType = AxisMarkerPickType.axisAndPlane;
        setOverrideCameraControl(true);

    }

    @Override
    public void activate(AssetManager manager, Node toolNode, Node onTopToolNode, Spatial selectedSpatial, SceneComposerToolController toolController) {
        super.activate(manager, toolNode, onTopToolNode, selectedSpatial, toolController);
        moveManager = Lookup.getDefault().lookup(MoveManager.class);
        displayPlanes();
    }

    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedPlane = null; // mouse released, reset selection
            if (wasDragging) {
                actionPerformed(moveManager.makeUndo());
                wasDragging = false;
            }
            moveManager.reset();
        }
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
    }

    @Override
    public void mouseMoved(Vector2f screenCoord, JmeNode rootNode, DataObject currentDataObject, JmeSpatial selectedSpatial) {
        
        if (pickedPlane == null) {
            highlightAxisMarker(camera, screenCoord, axisPickType);
        } else {
            pickedPlane = null;
            moveManager.reset();
        }
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedPlane = null; // mouse released, reset selection
            if (wasDragging) {
                actionPerformed(moveManager.makeUndo());
                wasDragging = false;
            }
            moveManager.reset();
            return;
        }

        if (toolController.getSelectedSpatial() == null) {
            return;
        }

        if (pickedPlane == null) {
            pickedPlane = pickAxisMarker(camera, screenCoord, axisPickType);
            if (pickedPlane == null) {
                return;
            }

            if (pickedPlane.equals(new Vector3f(1, 1, 0))) {
                moveManager.initiateMove(toolController.getSelectedSpatial(), MoveManager.XY, true);
            } else if (pickedPlane.equals(new Vector3f(1, 0, 1))) {
                moveManager.initiateMove(toolController.getSelectedSpatial(), MoveManager.XZ, true);
            } else if (pickedPlane.equals(new Vector3f(0, 1, 1))) {
                moveManager.initiateMove(toolController.getSelectedSpatial(), MoveManager.YZ, true);
            }
        }
        if (!moveManager.move(camera, screenCoord)) {
            return;
        }
        updateToolsTransformation();

        wasDragging = true;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
    }
}
