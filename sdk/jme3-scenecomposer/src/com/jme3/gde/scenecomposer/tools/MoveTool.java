/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.gde.scenecomposer.tools;

import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.gde.core.undoredo.AbstractUndoableSceneEdit;
import com.jme3.gde.scenecomposer.SceneEditTool;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import org.openide.loaders.DataObject;

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
    private Vector3f startLoc;
    private Vector3f lastLoc;
    private boolean wasDragging = false;
    private Vector3f offset;
    private Node plane;
    Material pinkMat;
    private final Quaternion XY = new Quaternion().fromAngleAxis(0,   new Vector3f(1,0,0));
    private final Quaternion YZ = new Quaternion().fromAngleAxis(-FastMath.PI/2,   new Vector3f(0,1,0));
    private final Quaternion XZ = new Quaternion().fromAngleAxis(FastMath.PI/2,   new Vector3f(1,0,0));
    
    
    public MoveTool() {
        axisPickType = AxisMarkerPickType.planeOnly;
        setOverrideCameraControl(true);
        
        float size = 1000;
        Geometry g = new Geometry("plane", new Quad(size, size));
        g.setLocalTranslation(-size/2, -size/2, 0);
        plane = new Node();
        plane.attachChild(g);
    }

    
    
    @Override
    public void actionPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedPlane = null; // mouse released, reset selection
            offset = null;
            if (wasDragging) {
                actionPerformed(new MoveUndo(selectedSpatial, startLoc, lastLoc));
                wasDragging = false;
            }
        }
    }

    @Override
    public void actionSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject dataObject) {
        
    }

    @Override
    public void mouseMoved(Vector2f screenCoord) {
        if (pickedPlane == null) {
            highlightAxisMarker(camera, screenCoord, axisPickType);
        }
        else {
            pickedPlane = null;
            offset = null;
        }
    }

    @Override
    public void draggedPrimary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        if (!pressed) {
            setDefaultAxisMarkerColors();
            pickedPlane = null; // mouse released, reset selection
            offset = null;
            if (wasDragging) {
                actionPerformed(new MoveUndo(selectedSpatial, startLoc, lastLoc));
                wasDragging = false;
            }
            return;
        }
        
        if (selectedSpatial == null)
            return;
        if (pickedPlane == null) {
            pickedPlane = pickAxisMarker(camera, screenCoord, axisPickType);
            if (pickedPlane == null)
                return;
            startLoc = selectedSpatial.getLocalTranslation().clone();
            
            if (pickedPlane.equals(new Vector3f(1,1,0)))
                plane.setLocalRotation(XY);
            else if (pickedPlane.equals(new Vector3f(1,0,1)))
                plane.setLocalRotation(XZ);
            else if (pickedPlane.equals(new Vector3f(0,1,1)))
                plane.setLocalRotation(YZ);
            plane.setLocalTranslation(startLoc);
        }
        
        Vector3f planeHit = pickWorldLocation(camera, screenCoord, plane);
        if (planeHit == null)
            return;

        if (offset == null)
            offset = planeHit.subtract(startLoc); // get the offset when we start so it doesn't jump

        Vector3f newPos = planeHit.subtract(offset);
        lastLoc = newPos;
        selectedSpatial.setLocalTranslation(newPos);
        updateToolsTransformation(selectedSpatial);
        
        wasDragging = true;
    }

    @Override
    public void draggedSecondary(Vector2f screenCoord, boolean pressed, JmeNode rootNode, DataObject currentDataObject) {
        
    }

    private class MoveUndo extends AbstractUndoableSceneEdit {

        private Spatial spatial;
        private Vector3f before,after;
        
        MoveUndo(Spatial spatial, Vector3f before, Vector3f after) {
            this.spatial = spatial;
            this.before = before;
            this.after = after;
        }
        
        @Override
        public void sceneUndo() {
            spatial.setLocalTranslation(before);
            toolController.selectedSpatialTransformed();
        }

        @Override
        public void sceneRedo() {
            spatial.setLocalTranslation(after);
            toolController.selectedSpatialTransformed();
        }
        
    }
}
