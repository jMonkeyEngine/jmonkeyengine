/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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
import com.jme3.cinematic.events.MotionEvent;
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
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A motion path is used to create a path between waypoints for animating objects.
 *
 * @author Nehon
 */
public class MotionPath implements JmeCloneable, Savable {

    private Node debugNode;
    private AssetManager assetManager;
    private List<MotionPathListener> listeners;
    private Spline spline = new Spline();
    int prevWayPoint = 0;

    /**
     * Creates a new motion path.
     */
    public MotionPath() {
    }

    /**
     * Interpolates the path based on the time elapsed since the animation began and the motion control.
     * This method updates the local translation of the spatial associated with the {@code MotionEvent} control.
     *
     * @param time    The time since the animation started (in seconds).
     * @param control The control managing the moving spatial.
     * @param tpf     Time per frame (in seconds).
     * @return The distance traveled (in world units).
     */
    public float interpolatePath(float time, MotionEvent control, float tpf) {

        float traveledDistance = 0;
        TempVars vars = TempVars.get();
        Vector3f newPosition = vars.vect1;
        Vector3f tempDirection = vars.vect2;
        Vector2f waypointInfo = vars.vect2d;

        // Computing traveled distance according to new time
        traveledDistance = time * (getLength() / control.getInitialDuration());

        // Getting waypoint index and current value from new traveled distance
        waypointInfo = getWayPointIndexForDistance(traveledDistance, waypointInfo);

        // Setting values
        control.setCurrentWayPoint((int) waypointInfo.x);
        control.setCurrentValue(waypointInfo.y);

        // Interpolating new position
        getSpline().interpolate(control.getCurrentValue(), control.getCurrentWayPoint(), newPosition);
        if (control.needsDirection()) {
            tempDirection.set(newPosition);
            tempDirection.subtractLocal(control.getSpatial().getLocalTranslation()).normalizeLocal();
            control.setDirection(tempDirection);
        }
        checkWayPoint(control, tpf);

        control.getSpatial().setLocalTranslation(newPosition);
        vars.release();
        return traveledDistance;
    }

    /**
     * Checks if a waypoint has been reached and triggers listeners if so.
     *
     * @param control The motion event control.
     * @param tpf     Time per frame (in seconds).
     */
    public void checkWayPoint(MotionEvent control, float tpf) {
        // Epsilon varies with the tpf to avoid missing a waypoint on low frame rate.
        float epsilon = tpf * 4f;
        if (control.getCurrentWayPoint() != prevWayPoint) {
            if (control.getCurrentValue() >= 0f && control.getCurrentValue() < epsilon) {
                triggerWayPointReach(control.getCurrentWayPoint(), control);
                prevWayPoint = control.getCurrentWayPoint();
            }
        }
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(spline, "spline", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        spline = (Spline) ic.readSavable("spline", null);
    }

    /**
     * Callback from {@link com.jme3.util.clone.Cloner} to convert this
     * shallow-cloned MotionPath into a deep-cloned one, using the specified
     * cloner and original to resolve copied fields.
     *
     * @param cloner   the cloner that's cloning this MotionPath (not null)
     * @param original the object from which this MotionPath was shallow-cloned
     *                 (not null, unaffected)
     */
    @Override
    public void cloneFields(Cloner cloner, Object original) {
        this.debugNode = cloner.clone(debugNode);
        this.spline = cloner.clone(spline);
        /*
         * The clone will share both the asset manager and the list of listeners
         * of the original MotionPath.
         */
    }

    /**
     * Creates a shallow clone for the JME cloner.
     *
     * @return a new object
     */
    @Override
    public MotionPath jmeClone() {
        try {
            MotionPath clone = (MotionPath) clone();
            return clone;
        } catch (CloneNotSupportedException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Computes the index of the waypoint and the interpolation value according to a distance.
     * Returns a {@code Vector2f} containing the index in the x field and the interpolation value in the y field.
     *
     * @param distance The distance traveled on this path.
     * @param store    Storage for the result (not null, modified).
     * @return The waypoint index and the interpolation value in a {@code Vector2f}.
     */
    public Vector2f getWayPointIndexForDistance(float distance, Vector2f store) {
        float sum = 0;
        if (spline.getTotalLength() == 0) {
            store.set(0, 0);
            return store;
        }
        distance = distance % spline.getTotalLength();
        int i = 0;
        for (Float len : spline.getSegmentsLength()) {
            if (sum + len >= distance) {
                return new Vector2f(i, (distance - sum) / len);
            }
            sum += len;
            i++;
        }
        store.set((float) spline.getControlPoints().size() - 1, 1.0f);
        return store;
    }

    /**
     * Adds a waypoint to the path.
     *
     * @param wayPoint A position in world space.
     */
    public void addWayPoint(Vector3f wayPoint) {
        spline.addControlPoint(wayPoint);
    }

    /**
     * Returns the waypoint at the given index.
     *
     * @param i The index of the waypoint.
     * @return The waypoint position.
     */
    public Vector3f getWayPoint(int i) {
        return spline.getControlPoints().get(i);
    }

    /**
     * Removes the specified waypoint from the path.
     *
     * @param wayPoint The waypoint to remove.
     */
    public void removeWayPoint(Vector3f wayPoint) {
        spline.removeControlPoint(wayPoint);
    }

    /**
     * Removes the waypoint at the given index from the path.
     *
     * @param i The index of the waypoint to remove.
     */
    public void removeWayPoint(int i) {
        removeWayPoint(spline.getControlPoints().get(i));
    }

    /**
     * Clears all waypoints from the path.
     */
    public void clearWayPoints() {
        spline.clearControlPoints();
    }

    /**
     * Returns the number of waypoints on this path.
     *
     * @return The count of waypoints (&ge;0)
     */
    public int getNbWayPoints() {
        return spline.getControlPoints().size();
    }

    /**
     * Returns the total length of the path in world units.
     *
     * @return The length of the path.
     */
    public float getLength() {
        return spline.getTotalLength();
    }

    /**
     * Returns an iterator for the waypoints collection.
     *
     * @return An iterator for the waypoints.
     */
    public Iterator<Vector3f> iterator() {
        return spline.getControlPoints().iterator();
    }

    /**
     * Disables the display of the path and waypoints.
     */
    public void disableDebugShape() {
        if (debugNode != null) {
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            assetManager = null;
        }
    }

    /**
     * Enables the display of the path and waypoints.
     *
     * @param manager  The asset manager.
     * @param rootNode The node where the debug shapes must be attached.
     */
    public void enableDebugShape(AssetManager manager, Node rootNode) {
        assetManager = manager;
        attachDebugNode(rootNode);
    }

    /**
     * Attaches the debug node to the root node if it doesn't already exist.
     *
     * @param root The root node to attach the debug shapes to.
     */
    private void attachDebugNode(Node root) {
        if (debugNode == null) {
            debugNode = new Node("DebugWayPoints");
            Material mat = assetManager.loadMaterial("Common/Materials/RedColor.j3m");
            for (Vector3f cp : spline.getControlPoints()) {
                Geometry geo = new Geometry("box", new Box(0.3f, 0.3f, 0.3f));
                geo.setLocalTranslation(cp);
                geo.setMaterial(mat);
                debugNode.attachChild(geo);
            }

            int nbSubSegments = (spline.getType() == SplineType.CatmullRom) ? 10 : 0;
            debugNode.attachChild(createPathDebugGeometry(nbSubSegments));

            root.attachChild(debugNode);
        }
    }

    /**
     * Creates the geometry for displaying the path based on the spline type.
     *
     * @param nbSubSegments The detail level for the curve (e.g., segments for CatmullRom).
     * @return The {@code Geometry} representing the path.
     */
    private Geometry createPathDebugGeometry(int nbSubSegments) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.getAdditionalRenderState().setWireframe(true);
        mat.setColor("Color", ColorRGBA.Blue);
        Geometry geo = new Geometry("line", new Curve(spline, nbSubSegments));
        geo.setMaterial(mat);
        return geo;
    }

    /**
     * Adds a motion path listener to the path.
     *
     * @param listener The {@code MotionPathListener} to attach.
     */
    public void addListener(MotionPathListener listener) {
        if (listeners == null) {
            listeners = new ArrayList<MotionPathListener>();
        }
        listeners.add(listener);
    }

    /**
     * Removes the given listener.
     *
     * @param listener The listener to remove.
     */
    public void removeListener(MotionPathListener listener) {
        if (listeners != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Triggers the waypoint reach event for all registered listeners.
     *
     * @param wayPointIndex The index of the waypoint that was reached.
     * @param control       The motion event control.
     */
    public void triggerWayPointReach(int wayPointIndex, MotionEvent control) {
        if (listeners != null) {
            for (MotionPathListener listener : listeners) {
                listener.onWayPointReach(control, wayPointIndex);
            }
        }
    }

    /**
     * Helper method to update the debug node when spline properties change.
     */
    private void updateDebugNode() {
        if (debugNode != null) {
            Node parent = debugNode.getParent();
            debugNode.removeFromParent();
            debugNode.detachAllChildren();
            debugNode = null;
            attachDebugNode(parent);
        }
    }

    /**
     * Returns the type of spline used for path interpolation.
     *
     * @return The path interpolation spline type.
     */
    public SplineType getPathSplineType() {
        return spline.getType();
    }

    /**
     * Sets the type of spline used for path interpolation.
     *
     * @param pathSplineType The desired spline type.
     */
    public void setPathSplineType(SplineType pathSplineType) {
        spline.setType(pathSplineType);
        updateDebugNode();
    }

    /**
     * Returns the curve tension.
     *
     * @return The curve tension.
     */
    public float getCurveTension() {
        return spline.getCurveTension();
    }

    /**
     * Sets the tension of the curve (only applicable for Catmull-Rom splines).
     * A value of 0.0 will result in a linear curve, while 1.0 will produce a round curve.
     *
     * @param curveTension The desired tension value.
     */
    public void setCurveTension(float curveTension) {
        spline.setCurveTension(curveTension);
        updateDebugNode();
    }

    /**
     * Sets whether the path forms a closed cycle.
     *
     * @param cycle {@code true} for a cycle, {@code false} for a non-cycle.
     */
    public void setCycle(boolean cycle) {
        spline.setCycle(cycle);
        updateDebugNode();
    }

    /**
     * Returns {@code true} if the path is a cycle, {@code false} otherwise.
     *
     * @return {@code true} if the path is a cycle.
     */
    public boolean isCycle() {
        return spline.isCycle();
    }

    /**
     * Returns the underlying {@code Spline} object used by this motion path.
     *
     * @return The {@code Spline} instance.
     */
    public Spline getSpline() {
        return spline;
    }
}
