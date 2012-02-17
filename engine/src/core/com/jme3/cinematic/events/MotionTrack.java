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
package com.jme3.cinematic.events;

import com.jme3.animation.LoopMode;
import com.jme3.app.Application;
import com.jme3.cinematic.Cinematic;
import com.jme3.cinematic.MotionPath;
import com.jme3.cinematic.PlayState;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * A MotionTrack is a control over the spatial that manage the position and direction of the spatial while following a motion Path
 *
 * You must first create a MotionPath and then create a MotionTrack to associate a spatial and the path.
 *
 * @author Nehon
 */
public class MotionTrack extends AbstractCinematicEvent implements Control {

    protected Spatial spatial;
    protected int currentWayPoint;
    protected float currentValue;
    protected Vector3f direction = new Vector3f();
    protected Vector3f lookAt;
    protected Vector3f upVector;
    protected Quaternion rotation;
    protected Direction directionType = Direction.None;
    protected MotionPath path;
    private boolean isControl = true;
    /**
     * the distance traveled by the spatial on the path
     */
    protected float traveledDistance = 0;

    /**
     * Enum for the different type of target direction behavior
     */
    public enum Direction {

        /**
         * the target stay in the starting direction
         */
        None,
        /**
         * The target rotates with the direction of the path
         */
        Path,
        /**
         * The target rotates with the direction of the path but with the additon of a rtotation
         * you need to use the setRotation mathod when using this Direction
         */
        PathAndRotation,
        /**
         * The target rotates with the given rotation
         */
        Rotation,
        /**
         * The target looks at a point
         * You need to use the setLookAt method when using this direction
         */
        LookAt
    }

    /**
     * Create MotionTrack,
     * when using this constructor don't forget to assign spatial and path
     */
    public MotionTrack() {
        super();
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionTrack(Spatial spatial, MotionPath path) {
        super();
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionTrack(Spatial spatial, MotionPath path, float initialDuration) {
        super(initialDuration);
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionTrack(Spatial spatial, MotionPath path, LoopMode loopMode) {
        super();
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
        this.loopMode = loopMode;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path
     * @param spatial
     * @param path
     */
    public MotionTrack(Spatial spatial, MotionPath path, float initialDuration, LoopMode loopMode) {
        super(initialDuration);
        this.spatial = spatial;
        spatial.addControl(this);
        this.path = path;
        this.loopMode = loopMode;
    }

    public void update(float tpf) {
        if (isControl) {

            if (playState == PlayState.Playing) {
                time = time + (tpf * speed);

                if (time >= initialDuration && loopMode == loopMode.DontLoop) {
                    stop();
                } else {
                    onUpdate(tpf);
                }
            }
        }
    }

    @Override
    public void initEvent(Application app, Cinematic cinematic) {
        super.initEvent(app, cinematic);
        isControl = false;
    }

    @Override
    public void setTime(float time) {
        super.setTime(time);
        onUpdate(0);
    }

    public void onUpdate(float tpf) {
        traveledDistance = path.interpolatePath(time, this);
        computeTargetDirection();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(lookAt, "lookAt", Vector3f.ZERO);
        oc.write(upVector, "upVector", Vector3f.UNIT_Y);
        oc.write(rotation, "rotation", Quaternion.IDENTITY);
        oc.write(directionType, "directionType", Direction.None);
        oc.write(path, "path", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        lookAt = (Vector3f) in.readSavable("lookAt", Vector3f.ZERO);
        upVector = (Vector3f) in.readSavable("upVector", Vector3f.UNIT_Y);
        rotation = (Quaternion) in.readSavable("rotation", Quaternion.IDENTITY);
        directionType = in.readEnum("directionType", Direction.class, Direction.None);
        path = (MotionPath) in.readSavable("path", null);
    }

    /**
     * this method is meant to be called by the motion path only
     * @return
     */
    public boolean needsDirection() {
        return directionType == Direction.Path || directionType == Direction.PathAndRotation;
    }

    private void computeTargetDirection() {
        switch (directionType) {
            case Path:
                Quaternion q = new Quaternion();
                q.lookAt(direction, Vector3f.UNIT_Y);
                spatial.setLocalRotation(q);
                break;
            case LookAt:
                if (lookAt != null) {
                    spatial.lookAt(lookAt, upVector);
                }
                break;
            case PathAndRotation:
                if (rotation != null) {
                    Quaternion q2 = new Quaternion();
                    q2.lookAt(direction, Vector3f.UNIT_Y);
                    q2.multLocal(rotation);
                    spatial.setLocalRotation(q2);
                }
                break;
            case Rotation:
                if (rotation != null) {
                    spatial.setLocalRotation(rotation);
                }
                break;
            case None:
                break;
            default:
                break;
        }
    }

    /**
     * Clone this control for the given spatial
     * @param spatial
     * @return
     */
    public Control cloneForSpatial(Spatial spatial) {
        MotionTrack control = new MotionTrack(spatial, path);
        control.playState = playState;
        control.currentWayPoint = currentWayPoint;
        control.currentValue = currentValue;
        control.direction = direction.clone();
        control.lookAt = lookAt.clone();
        control.upVector = upVector.clone();
        control.rotation = rotation.clone();
        control.initialDuration = initialDuration;
        control.speed = speed;
        control.loopMode = loopMode;
        control.directionType = directionType;

        return control;
    }

    @Override
    public void onPlay() {
        traveledDistance = 0;
    }

    @Override
    public void onStop() {
        currentWayPoint = 0;
    }

    @Override
    public void onPause() {
    }

    /**
     * this method is meant to be called by the motion path only
     * @return
     */
    public float getCurrentValue() {
        return currentValue;
    }

    /**
     * this method is meant to be called by the motion path only
     *
     */
    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
    }

    /**
     * this method is meant to be called by the motion path only
     * @return
     */
    public int getCurrentWayPoint() {
        return currentWayPoint;
    }

    /**
     * this method is meant to be called by the motion path only
     *
     */
    public void setCurrentWayPoint(int currentWayPoint) {
        if (this.currentWayPoint != currentWayPoint) {
            this.currentWayPoint = currentWayPoint;
            path.triggerWayPointReach(currentWayPoint, this);
        }
    }

    /**
     * returns the direction the spatial is moving
     * @return
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Sets the direction of the spatial
     * This method is used by the motion path.
     * @param direction
     */
    public void setDirection(Vector3f direction) {
        this.direction.set(direction);
    }

    /**
     * returns the direction type of the target
     * @return the direction type
     */
    public Direction getDirectionType() {
        return directionType;
    }

    /**
     * Sets the direction type of the target
     * On each update the direction given to the target can have different behavior
     * See the Direction Enum for explanations
     * @param directionType the direction type
     */
    public void setDirectionType(Direction directionType) {
        this.directionType = directionType;
    }

    /**
     * Set the lookAt for the target
     * This can be used only if direction Type is Direction.LookAt
     * @param lookAt the position to look at
     * @param upVector the up vector
     */
    public void setLookAt(Vector3f lookAt, Vector3f upVector) {
        this.lookAt = lookAt;
        this.upVector = upVector;
    }

    /**
     * returns the rotation of the target
     * @return the rotation quaternion
     */
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * sets the rotation of the target
     * This can be used only if direction Type is Direction.PathAndRotation or Direction.Rotation
     * With PathAndRotation the target will face the direction of the path multiplied by the given Quaternion.
     * With Rotation the rotation of the target will be set with the given Quaternion.
     * @param rotation the rotation quaternion
     */
    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    /**
     * retun the motion path this control follows
     * @return
     */
    public MotionPath getPath() {
        return path;
    }

    /**
     * Sets the motion path to follow
     * @param path
     */
    public void setPath(MotionPath path) {
        this.path = path;
    }

    public void setEnabled(boolean enabled) {
        if (enabled) {
            play();
        } else {
            pause();
        }
    }

    public boolean isEnabled() {
        return playState != PlayState.Stopped;
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
    }

    public Spatial getSpatial() {
        return spatial;
    }

    /**
     * return the distance traveled by the spatial on the path
     * @return 
     */
    public float getTraveledDistance() {
        return traveledDistance;
    }
}
