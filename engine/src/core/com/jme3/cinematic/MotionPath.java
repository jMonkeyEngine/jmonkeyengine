/*
 * Copyright (c) 2009-2010 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.cinematic;

import com.jme3.asset.AssetManager;
import com.jme3.cinematic.events.MotionTrack;
import com.jme3.export.*;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Spline;
import com.jme3.math.Spline.SplineType;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Curve;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Motion path is used to create a path between way points.
 * @author Nehon
 */
public class MotionPath implements Savable {

    private Node debugNode;
    private AssetManager assetManager;
    private List<MotionPathListener> listeners;
    private Spline spline = new Spline();
    private float eps = 0.0001f;

    /**
     * Create a motion Path
     */
    public MotionPath() {
    }

    /**
     * interpolate the path giving the time since the beginnin and the motionControl     
     * this methods sets the new localTranslation to the spatial of the motionTrack control.
     * @param time the time since the animation started
     * @param control the ocntrol over the moving spatial
     */
    public float interpolatePath(float time, MotionTrack control) {

        float traveledDistance = 0;
        TempVars vars = TempVars.get();
        Vector3f temp = vars.vect1;
        Vector3f tmpVector = vars.vect2;
        //computing traveled distance according to new time
        traveledDistance = time * (getLength() / control.getInitialDuration());

        //getting waypoint index and current value from new traveled distance
        Vector2f v = getWayPointIndexForDistance(traveledDistance);

        //setting values
        control.setCurrentWayPoint((int) v.x);
        control.setCurrentValue(v.y);
        
        //interpolating new position
        getSpline().interpolate(control.getCurrentValue(), control.getCurrentWayPoint(), temp);
        if (control.needsDirection()) {
            tmpVector.set(temp);
            control.setDirection(tmpVector.subtractLocal(control.getSpatial().getLocalTranslation()).normalizeLocal());
        }

        control.getSpatial().setLocalTranslation(temp);
        vars.release();
        return traveledDistance;
    }

    private void attachDebugNode(Node root) {
        if (debugNode == null) {
            debugNode = new Node();
            Material m = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
            for (Iterator<Vector3f> it = spline.getControlPoints().iterator(); it.hasNext();) {
                Vector3f cp = it.next();
                Geometry geo = new Geometry("box", new Box(cp, 0.3f, 0.3f, 0.3f));
                geo.setMaterial(m);
                debugNode.attachChild(geo);

            }
            switch (spline.getType()) {
                case CatmullRom:
                    debugNode.attachChild(CreateCatmullRomPath());
                    break;
                case Linear:
                    debugNode.attachChild(CreateLinearPath());
                    break;
                default:
                    debugNode.attachChild(CreateLinearPath());
                    break;
            }

            root.attachChild(debugNode);
        }
    }

    private Geometry CreateLinearPath() {

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        Geometry lineGeometry = new Geometry("line", new Curve(spline, 0));
        lineGeometry.setMaterial(mat);
        return lineGeometry;
    }

    private Geometry CreateCatmullRomPath() {

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        Geometry lineGeometry = new Geometry("line", new Curve(spline, 10));
        lineGeometry.setMaterial(mat);
        return lineGeometry;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(spline, "spline", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule in = im.getCapsule(this);
        spline = (Spline) in.readSavable("spline", null);

    }

    /**
     * compute the index of the waypoint and the interpolation value according to a distance
     * returns a vector 2 containing the index in the x field and the interpolation value in the y field
     * @param distance the distance traveled on this path
     * @return the waypoint index and the interpolation value in a vector2
     */
    public Vector2f getWayPointIndexForDistance(float distance) {
        float sum = 0;
        distance = distance % spline.getTotalLength();
        int i = 0;
        for (Float len : spline.getSegmentsLength()) {
            if (sum + len >= distance) {
                return new Vector2f((float) i, (distance - sum) / len);
            }
            sum += len;
            i++;
        }
        return new Vector2f((float) spline.getControlPoints().size() - 1, 1.0f);
    }

    /**
     * Addsa waypoint to the path
     * @param wayPoint a position in world space
     */
    public void addWayPoint(Vector3f wayPoint) {
        spline.addControlPoint(wayPoint);
    }

    /**
     * retruns the length of the path in world units
     * @return the length
     */
    public float getLength() {
        return spline.getTotalLength();
    }

    /**
     * returns the waypoint at the given index
     * @param i the index
     * @return returns the waypoint position
     */
    public Vector3f getWayPoint(int i) {
        return spline.getControlPoints().get(i);
    }

    /**
     * remove the waypoint from the path
     * @param wayPoint the waypoint to remove
     */
    public void removeWayPoint(Vector3f wayPoint) {
        spline.removeControlPoint(wayPoint);
    }

    /**
     * remove the waypoint at the given index from the path
     * @param i the index of the waypoint to remove
     */
    public void removeWayPoint(int i) {
        removeWayPoint(spline.getControlPoints().get(i));
    }

    /**
     * returns an iterator on the waypoints collection
     * @return
     */
    public Iterator<Vector3f> iterator() {
        return spline.getControlPoints().iterator();
    }

    /**
     * return the type of spline used for the path interpolation for this path
     * @return the path interpolation spline type
     */
    public SplineType getPathSplineType() {
        return spline.getType();
    }

    /**
     * sets the type of spline used for the path interpolation for this path
     * @param pathSplineType
     */
    public void setPathSplineType(SplineType pathSplineType) {
        spline.setType(pathSplineType);
        if (debugNode != null) {
            Node parent = debugNode.getParent();
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            attachDebugNode(parent);
        }
    }

    /**
     * disable the display of the path and the waypoints
     */
    public void disableDebugShape() {

        debugNode.detachAllChildren();
        debugNode = null;
        assetManager = null;
    }

    /**
     * enable the display of the path and the waypoints
     * @param manager the assetManager
     * @param rootNode the node where the debug shapes must be attached
     */
    public void enableDebugShape(AssetManager manager, Node rootNode) {
        assetManager = manager;
        // computeTotalLentgh();
        attachDebugNode(rootNode);
    }

    /**
     * Adds a motion pathListener to the path
     * @param listener the MotionPathListener to attach
     */
    public void addListener(MotionPathListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<MotionPathListener>();
        }
        listeners.add(listener);
    }

    /**
     * remove the given listener
     * @param listener the listener to remove
     */
    public void removeListener(MotionPathListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * return the number of waypoints of this path
     * @return
     */
    public int getNbWayPoints() {
        return spline.getControlPoints().size();
    }

    public void triggerWayPointReach(int wayPointIndex, MotionTrack control) {
        if (listeners != null) {
            for (Iterator<MotionPathListener> it = listeners.iterator(); it.hasNext();) {
                MotionPathListener listener = it.next();
                listener.onWayPointReach(control, wayPointIndex);
            }
        }
    }

    /**
     * Returns the curve tension
     * @return
     */
    public float getCurveTension() {
        return spline.getCurveTension();
    }

    /**
     * sets the tension of the curve (only for catmull rom) 0.0 will give a linear curve, 1.0 a round curve
     * @param curveTension
     */
    public void setCurveTension(float curveTension) {
        spline.setCurveTension(curveTension);
        if (debugNode != null) {
            Node parent = debugNode.getParent();
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            attachDebugNode(parent);
        }
    }

    public void clearWayPoints() {
        spline.clearControlPoints();
    }

    /**
     * Sets the path to be a cycle
     * @param cycle
     */
    public void setCycle(boolean cycle) {

        spline.setCycle(cycle);
        if (debugNode != null) {
            Node parent = debugNode.getParent();
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            attachDebugNode(parent);
        }

    }

    /**
     * returns true if the path is a cycle
     * @return
     */
    public boolean isCycle() {
        return spline.isCycle();
    }

    public Spline getSpline() {
        return spline;
    }
}
