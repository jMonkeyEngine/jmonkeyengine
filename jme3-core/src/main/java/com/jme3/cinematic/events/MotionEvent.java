/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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

import com.jme3.animation.AnimationUtils;
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
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * A MotionEvent is a control over the spatial that manages the position and direction of the spatial while following a motion Path.
 *
 * You must first create a MotionPath and then create a MotionEvent to associate a spatial and the path.
 *
 * @author Nehon
 */
public class MotionEvent extends AbstractCinematicEvent implements Control, JmeCloneable {

    protected Spatial spatial;
    protected int currentWayPoint;
    protected float currentValue;
    protected Vector3f direction = new Vector3f();
    protected Vector3f lookAt = null;
    protected Vector3f upVector = Vector3f.UNIT_Y;
    protected Quaternion rotation = null;
    protected Direction directionType = Direction.None;
    protected MotionPath path;
    private boolean isControl = true;
    private int travelDirection = 1;
    /**
     * the distance traveled by the spatial on the path
     */
    protected float traveledDistance = 0;

    /**
     * Enum for the different type of target direction behavior.
     */
    public enum Direction {

        /**
         * The target stays in the starting direction.
         */
        None,
        /**
         * The target rotates with the direction of the path.
         */
        Path,
        /**
         * The target rotates with the direction of the path but with the addition of a rotation.
         * You need to use the setRotation method when using this Direction.
         */
        PathAndRotation,
        /**
         * The target rotates with the given rotation.
         */
        Rotation,
        /**
         * The target looks at a point.
         * You need to use the setLookAt method when using this direction.
         */
        LookAt
    }

    /**
     * Create MotionEvent,
     * when using this constructor don't forget to assign spatial and path.
     */
    public MotionEvent() {
        super();
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path.
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path) {
        super();
        spatial.addControl(this);
        this.path = path;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path.
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path, float initialDuration) {
        super(initialDuration);
        spatial.addControl(this);
        this.path = path;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path.
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path, LoopMode loopMode) {
        super();
        spatial.addControl(this);
        this.path = path;
        this.loopMode = loopMode;
    }

    /**
     * Creates a MotionPath for the given spatial on the given motion path.
     * @param spatial
     * @param path
     */
    public MotionEvent(Spatial spatial, MotionPath path, float initialDuration, LoopMode loopMode) {
        super(initialDuration);
        spatial.addControl(this);
        this.path = path;
        this.loopMode = loopMode;
    }

    public void update(float tpf) {
        if (isControl) {
            internalUpdate(tpf);
        }
    }

    @Override
    public void internalUpdate(float tpf) {
        if (playState == PlayState.Playing) {
            time = time + (tpf * speed);
            if (loopMode == LoopMode.Loop && time < 0) {
                time = initialDuration;
            }            
            if ((time >= initialDuration || time < 0) && loopMode == LoopMode.DontLoop) {
                if (time >= initialDuration) {
                    path.triggerWayPointReach(path.getNbWayPoints() - 1, this);
                }
                stop();
            } else {
                time = AnimationUtils.clampWrapTime(time, initialDuration, loopMode);
                if(time<0){
                    speed = - speed;
                    time = - time;
                }
                onUpdate(tpf);
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
        traveledDistance = path.interpolatePath(time, this, tpf);
        computeTargetDirection();
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(lookAt, "lookAt", null);
        oc.write(upVector, "upVector", Vector3f.UNIT_Y);
        oc.write(rotation, "rotation", null);
        oc.write(directionType, "directionType", Direction.None);
        oc.write(path, "path", null);
        oc.write(spatial, "spatial", null);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule in = im.getCapsule(this);
        lookAt = (Vector3f) in.readSavable("lookAt", null);
        upVector = (Vector3f) in.readSavable("upVector", Vector3f.UNIT_Y);
        rotation = (Quaternion) in.readSavable("rotation", null);
        directionType = in.readEnum("directionType", Direction.class, Direction.None);
        path = (MotionPath) in.readSavable("path", null);
        spatial = (Spatial) in.readSavable("spatial", null);
    }

    /**
     * This method is meant to be called by the motion path only.
     * @return
     */
    public boolean needsDirection() {
        return directionType == Direction.Path || directionType == Direction.PathAndRotation;
    }

    private void computeTargetDirection() {
        switch (directionType) {
            case Path:
                Quaternion q = new Quaternion();
                q.lookAt(direction, upVector);
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
                    q2.lookAt(direction, upVector);
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
     * Clone this control for the given spatial.
     * @param spatial
     * @return
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        MotionEvent control = new MotionEvent();
        control.setPath(path);
        control.playState = playState;
        control.currentWayPoint = currentWayPoint;
        control.currentValue = currentValue;
        control.direction = direction.clone();
        control.lookAt = lookAt;
        control.upVector = upVector.clone();
        control.rotation = rotation;
        control.initialDuration = initialDuration;
        control.speed = speed;
        control.loopMode = loopMode;
        control.directionType = directionType;

        return control;
    }

    @Override   
    public Object jmeClone() {
        MotionEvent control = new MotionEvent();
        control.path = path;
        control.playState = playState;
        control.currentWayPoint = currentWayPoint;
        control.currentValue = currentValue;
        control.direction = direction.clone();
        control.lookAt = lookAt;
        control.upVector = upVector.clone();
        control.rotation = rotation;
        control.initialDuration = initialDuration;
        control.speed = speed;
        control.loopMode = loopMode;
        control.directionType = directionType;
        control.spatial = spatial;

        return control;
    }     

    @Override   
    public void cloneFields( Cloner cloner, Object original ) { 
        this.spatial = cloner.clone(spatial);
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
     * This method is meant to be called by the motion path only.
     * @return
     */
    public float getCurrentValue() {
        return currentValue;
    }

    /**
     * This method is meant to be called by the motion path only.
     *
     */
    public void setCurrentValue(float currentValue) {
        this.currentValue = currentValue;
    }

    /**
     * This method is meant to be called by the motion path only.
     * @return
     */
    public int getCurrentWayPoint() {
        return currentWayPoint;
    }

    /**
     * This method is meant to be called by the motion path only.
     *
     */
    public void setCurrentWayPoint(int currentWayPoint) {
        this.currentWayPoint = currentWayPoint;
    }

    /**
     * Returns the direction the spatial is moving.
     * @return
     */
    public Vector3f getDirection() {
        return direction;
    }

    /**
     * Sets the direction of the spatial, using the Y axis as the up vector.
     * Use MotionEvent#setDirection((Vector3f direction,Vector3f upVector) if 
     * you want a custum up vector.
     * This method is used by the motion path.
     * @param direction
     */
    public void setDirection(Vector3f direction) {
        setDirection(direction, Vector3f.UNIT_Y); 
   }
    
    /**
     * Sets the direction of the spatial with the given up vector.
     * This method is used by the motion path.
     * @param direction
     * @param upVector the up vector to consider for this direction.
     */
    public void setDirection(Vector3f direction,Vector3f upVector) {
        this.direction.set(direction);
        this.upVector.set(upVector);
    }

    /**
     * Returns the direction type of the target.
     * @return the direction type.
     */
    public Direction getDirectionType() {
        return directionType;
    }

    /**
     * Sets the direction type of the target.
     * On each update the direction given to the target can have different behavior.
     * See the Direction Enum for explanations.
     * @param directionType the direction type.
     */
    public void setDirectionType(Direction directionType) {
        this.directionType = directionType;
    }

    /**
     * Set the lookAt for the target.
     * This can be used only if direction Type is Direction.LookAt.
     * @param lookAt the position to look at.
     * @param upVector the up vector.
     */
    public void setLookAt(Vector3f lookAt, Vector3f upVector) {
        this.lookAt = lookAt;
        this.upVector = upVector;
    }

    /**
     * Returns the rotation of the target.
     * @return the rotation quaternion.
     */
    public Quaternion getRotation() {
        return rotation;
    }

    /**
     * Sets the rotation of the target.
     * This can be used only if direction Type is Direction.PathAndRotation or Direction.Rotation.
     * With PathAndRotation the target will face the direction of the path multiplied by the given Quaternion.
     * With Rotation the rotation of the target will be set with the given Quaternion.
     * @param rotation the rotation quaternion.
     */
    public void setRotation(Quaternion rotation) {
        this.rotation = rotation;
    }

    /**
     * Return the motion path this control follows.
     * @return
     */
    public MotionPath getPath() {
        return path;
    }

    /**
     * Sets the motion path to follow.
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
     * Return the distance traveled by the spatial on the path.
     * @return 
     */
    public float getTraveledDistance() {
        return traveledDistance;
    }
}
