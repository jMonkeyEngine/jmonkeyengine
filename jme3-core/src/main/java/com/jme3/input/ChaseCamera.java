/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.input;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.input.controls.*;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.jme3.util.clone.Cloner;
import com.jme3.util.clone.JmeCloneable;
import java.io.IOException;

/**
 * A camera that follows a spatial and can turn around it by dragging the mouse
 * @author nehon
 */
public class ChaseCamera implements ActionListener, AnalogListener, Control, JmeCloneable {

    protected Spatial target = null;
    protected float minVerticalRotation = 0.00f;
    protected float maxVerticalRotation = FastMath.PI / 2;
    protected float minDistance = 1.0f;
    protected float maxDistance = 40.0f;
    protected float distance = 20;    
    protected float rotationSpeed = 1.0f;
    protected float rotation = 0;
    protected float trailingRotationInertia = 0.05f;
    protected float zoomSensitivity = 2f;
    protected float rotationSensitivity = 5f;
    protected float chasingSensitivity = 5f;
    protected float trailingSensitivity = 0.5f;
    protected float vRotation = FastMath.PI / 6;
    protected boolean smoothMotion = false;
    protected boolean trailingEnabled = true;
    protected float rotationLerpFactor = 0;
    protected float trailingLerpFactor = 0;
    protected boolean rotating = false;
    protected boolean vRotating = false;
    protected float targetRotation = rotation;
    protected InputManager inputManager;
    protected Vector3f initialUpVec;
    protected float targetVRotation = vRotation;
    protected float vRotationLerpFactor = 0;
    protected float targetDistance = distance;
    protected float distanceLerpFactor = 0;
    protected boolean zooming = false;
    protected boolean trailing = false;
    protected boolean chasing = false;
    protected boolean veryCloseRotation = true;
    protected boolean canRotate;
    protected float offsetDistance = 0.002f;
    protected Vector3f prevPos;
    protected boolean targetMoves = false;
    protected boolean enabled = true;
    protected Camera cam = null;
    protected final Vector3f targetDir = new Vector3f();
    protected float previousTargetRotation;
    protected final Vector3f pos = new Vector3f();
    protected Vector3f targetLocation = new Vector3f(0, 0, 0);
    protected boolean dragToRotate = true;
    protected Vector3f lookAtOffset = new Vector3f(0, 0, 0);
    protected boolean leftClickRotate = true;
    protected boolean rightClickRotate = true;
    protected Vector3f temp = new Vector3f(0, 0, 0);
    protected boolean invertYaxis = false;
    protected boolean invertXaxis = false;
    
    /**
     * @deprecated use {@link CameraInput#CHASECAM_DOWN}
     */
    @Deprecated
    public final static String ChaseCamDown = "ChaseCamDown";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_UP}
     */
    @Deprecated
    public final static String ChaseCamUp = "ChaseCamUp";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_ZOOMIN}
     */
    @Deprecated
    public final static String ChaseCamZoomIn = "ChaseCamZoomIn";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_ZOOMOUT}
     */
    @Deprecated
    public final static String ChaseCamZoomOut = "ChaseCamZoomOut";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_MOVELEFT}
     */
    @Deprecated
    public final static String ChaseCamMoveLeft = "ChaseCamMoveLeft";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_MOVERIGHT}
     */
    @Deprecated
    public final static String ChaseCamMoveRight = "ChaseCamMoveRight";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_TOGGLEROTATE}
     */
    @Deprecated
    public final static String ChaseCamToggleRotate = "ChaseCamToggleRotate";

    protected boolean zoomin;
    protected boolean hideCursorOnRotate = true;

    /**
     * Constructs the chase camera
     * @param cam the application camera
     * @param target the spatial to follow
     */
    public ChaseCamera(Camera cam, final Spatial target) {
        this(cam);
        target.addControl(this);
    }

    /**
     * Constructs the chase camera
     * if you use this constructor you have to attach the cam later to a spatial
     * doing spatial.addControl(chaseCamera);
     * @param cam the application camera
     */
    public ChaseCamera(Camera cam) {
        this.cam = cam;
        initialUpVec = cam.getUp().clone();
    }

    /**
     * Constructs the chase camera, and registers inputs
     * if you use this constructor you have to attach the cam later to a spatial
     * doing spatial.addControl(chaseCamera);
     * @param cam the application camera
     * @param inputManager the inputManager of the application to register inputs
     */
    public ChaseCamera(Camera cam, InputManager inputManager) {
        this(cam);
        registerWithInput(inputManager);
    }

    /**
     * Constructs the chase camera, and registers inputs
     * @param cam the application camera
     * @param target the spatial to follow
     * @param inputManager the inputManager of the application to register inputs
     */
    public ChaseCamera(Camera cam, final Spatial target, InputManager inputManager) {
        this(cam, target);
        registerWithInput(inputManager);
    }

    public void onAction(String name, boolean keyPressed, float tpf) {
        if (dragToRotate) {
            if (name.equals(CameraInput.CHASECAM_TOGGLEROTATE) && enabled) {
                if (keyPressed) {
                    canRotate = true;
                    if (hideCursorOnRotate) {
                        inputManager.setCursorVisible(false);
                    }
                } else {
                    canRotate = false;
                    if (hideCursorOnRotate) {
                        inputManager.setCursorVisible(true);
                    }
                }
            }
        }

    }


    public void onAnalog(String name, float value, float tpf) {
        if (name.equals(CameraInput.CHASECAM_MOVELEFT)) {
            rotateCamera(-value);
        } else if (name.equals(CameraInput.CHASECAM_MOVERIGHT)) {
            rotateCamera(value);
        } else if (name.equals(CameraInput.CHASECAM_UP)) {
            vRotateCamera(value);
        } else if (name.equals(CameraInput.CHASECAM_DOWN)) {
            vRotateCamera(-value);
        } else if (name.equals(CameraInput.CHASECAM_ZOOMIN)) {
            zoomCamera(-value);
            if (zoomin == false) {
                distanceLerpFactor = 0;
            }
            zoomin = true;
        } else if (name.equals(CameraInput.CHASECAM_ZOOMOUT)) {
            zoomCamera(+value);
            if (zoomin == true) {
                distanceLerpFactor = 0;
            }
            zoomin = false;
        }
    }

    /**
     * Registers inputs with the input manager
     * @param inputManager
     */
    public final void registerWithInput(InputManager inputManager) {

        String[] inputs = {CameraInput.CHASECAM_TOGGLEROTATE,
            CameraInput.CHASECAM_DOWN,
            CameraInput.CHASECAM_UP,
            CameraInput.CHASECAM_MOVELEFT,
            CameraInput.CHASECAM_MOVERIGHT,
            CameraInput.CHASECAM_ZOOMIN,
            CameraInput.CHASECAM_ZOOMOUT};

        this.inputManager = inputManager;
        if (!invertYaxis) {
            inputManager.addMapping(CameraInput.CHASECAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
            inputManager.addMapping(CameraInput.CHASECAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        } else {
            inputManager.addMapping(CameraInput.CHASECAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
            inputManager.addMapping(CameraInput.CHASECAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        }
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMIN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMOUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        if (!invertXaxis) {
            inputManager.addMapping(CameraInput.CHASECAM_MOVELEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
            inputManager.addMapping(CameraInput.CHASECAM_MOVERIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        } else {
            inputManager.addMapping(CameraInput.CHASECAM_MOVELEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
            inputManager.addMapping(CameraInput.CHASECAM_MOVERIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        }
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(this, inputs);
    }
    
    /**
    * Cleans up the input mappings from the input manager.
    * Undoes the work of registerWithInput().
    * @param inputManager InputManager from which to cleanup mappings.
    */
    public void cleanupWithInput(InputManager mgr){
        mgr.deleteMapping(CameraInput.CHASECAM_TOGGLEROTATE);
        mgr.deleteMapping(CameraInput.CHASECAM_DOWN);
        mgr.deleteMapping(CameraInput.CHASECAM_UP);
        mgr.deleteMapping(CameraInput.CHASECAM_MOVELEFT);
        mgr.deleteMapping(CameraInput.CHASECAM_MOVERIGHT);
        mgr.deleteMapping(CameraInput.CHASECAM_ZOOMIN);
        mgr.deleteMapping(CameraInput.CHASECAM_ZOOMOUT);
        mgr.removeListener(this);
    }

    /**
     * Sets custom triggers for toggling the rotation of the cam
     * default are
     * new MouseButtonTrigger(MouseInput.BUTTON_LEFT)  left mouse button
     * new MouseButtonTrigger(MouseInput.BUTTON_RIGHT)  right mouse button
     * @param triggers
     */
    public void setToggleRotationTrigger(Trigger... triggers) {
        inputManager.deleteMapping(CameraInput.CHASECAM_TOGGLEROTATE);
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE, triggers);
        inputManager.addListener(this, CameraInput.CHASECAM_TOGGLEROTATE);
    }

    /**
     * Sets custom triggers for zooming in the cam
     * default is
     * new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true)  mouse wheel up
     * @param triggers
     */
    public void setZoomInTrigger(Trigger... triggers) {
        inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMIN);
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMIN, triggers);
        inputManager.addListener(this, CameraInput.CHASECAM_ZOOMIN);
    }

    /**
     * Sets custom triggers for zooming out the cam
     * default is
     * new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false)  mouse wheel down
     * @param triggers
     */
    public void setZoomOutTrigger(Trigger... triggers) {
        inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMOUT);
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMOUT, triggers);
        inputManager.addListener(this, CameraInput.CHASECAM_ZOOMOUT);
    }

    protected void computePosition() {

        float hDistance = (distance) * FastMath.sin((FastMath.PI / 2) - vRotation);
        pos.set(hDistance * FastMath.cos(rotation), (distance) * FastMath.sin(vRotation), hDistance * FastMath.sin(rotation));
        pos.addLocal(target.getWorldTranslation());
    }

    //rotate the camera around the target on the horizontal plane
    protected void rotateCamera(float value) {
        if (!canRotate || !enabled) {
            return;
        }
        rotating = true;
        targetRotation += value * rotationSpeed;


    }

    //move the camera toward or away the target
    protected void zoomCamera(float value) {
        if (!enabled) {
            return;
        }

        zooming = true;
        targetDistance += value * zoomSensitivity;
        if (targetDistance > maxDistance) {
            targetDistance = maxDistance;
        }
        if (targetDistance < minDistance) {
            targetDistance = minDistance;
        }
        if (veryCloseRotation) {
            if ((targetVRotation < minVerticalRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetVRotation = minVerticalRotation;
            }
        }
    }

    //rotate the camera around the target on the vertical plane
    protected void vRotateCamera(float value) {
        if (!canRotate || !enabled) {
            return;
        }
        vRotating = true;
        float lastGoodRot = targetVRotation;
        targetVRotation += value * rotationSpeed;
        if (targetVRotation > maxVerticalRotation) {
            targetVRotation = lastGoodRot;
        }
        if (veryCloseRotation) {
            if ((targetVRotation < minVerticalRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetVRotation = minVerticalRotation;
            } else if (targetVRotation < -FastMath.DEG_TO_RAD * 90) {
                targetVRotation = lastGoodRot;
            }
        } else {
            if ((targetVRotation < minVerticalRotation)) {
                targetVRotation = lastGoodRot;
            }
        }
    }

    /**
     * Updates the camera, should only be called internally
     */
    protected void updateCamera(float tpf) {
        if (enabled) {
            targetLocation.set(target.getWorldTranslation()).addLocal(lookAtOffset);
            if (smoothMotion) {

                //computation of target direction
                targetDir.set(targetLocation).subtractLocal(prevPos);
                float dist = targetDir.length();

                //Low pass filtering on the target postition to avoid shaking when physics are enabled.
                if (offsetDistance < dist) {
                    //target moves, start chasing.
                    chasing = true;
                    //target moves, start trailing if it has to.
                    if (trailingEnabled) {
                        trailing = true;
                    }
                    //target moves...
                    targetMoves = true;
                } else {
                    //if target was moving, we compute a slight offset in rotation to avoid a rought stop of the cam
                    //We do not if the player is rotationg the cam
                    if (targetMoves && !canRotate) {
                        if (targetRotation - rotation > trailingRotationInertia) {
                            targetRotation = rotation + trailingRotationInertia;
                        } else if (targetRotation - rotation < -trailingRotationInertia) {
                            targetRotation = rotation - trailingRotationInertia;
                        }
                    }
                    //Target stops
                    targetMoves = false;
                }

                //the user is rotating the cam by dragging the mouse
                if (canRotate) {
                    //reseting the trailing lerp factor
                    trailingLerpFactor = 0;
                    //stop trailing user has the control
                    trailing = false;
                }


                if (trailingEnabled && trailing) {
                    if (targetMoves) {
                        //computation if the inverted direction of the target
                        Vector3f a = targetDir.negate().normalizeLocal();
                        //the x unit vector
                        Vector3f b = Vector3f.UNIT_X;
                        //2d is good enough
                        a.y = 0;
                        //computation of the rotation angle between the x axis and the trail
                        if (targetDir.z > 0) {
                            targetRotation = FastMath.TWO_PI - FastMath.acos(a.dot(b));
                        } else {
                            targetRotation = FastMath.acos(a.dot(b));
                        }
                        if (targetRotation - rotation > FastMath.PI || targetRotation - rotation < -FastMath.PI) {
                            targetRotation -= FastMath.TWO_PI;
                        }

                        //if there is an important change in the direction while trailing reset of the lerp factor to avoid jumpy movements
                        if (targetRotation != previousTargetRotation && FastMath.abs(targetRotation - previousTargetRotation) > FastMath.PI / 8) {
                            trailingLerpFactor = 0;
                        }
                        previousTargetRotation = targetRotation;
                    }
                    //computing lerp factor
                    trailingLerpFactor = Math.min(trailingLerpFactor + tpf * tpf * trailingSensitivity, 1);
                    //computing rotation by linear interpolation
                    rotation = FastMath.interpolateLinear(trailingLerpFactor, rotation, targetRotation);

                    //if the rotation is near the target rotation we're good, that's over
                    if (targetRotation + 0.01f >= rotation && targetRotation - 0.01f <= rotation) {
                        trailing = false;
                        trailingLerpFactor = 0;
                    }
                }

                //linear interpolation of the distance while chasing
                if (chasing) {
                    distance = temp.set(targetLocation).subtractLocal(cam.getLocation()).length();
                    distanceLerpFactor = Math.min(distanceLerpFactor + (tpf * tpf * chasingSensitivity * 0.05f), 1);
                    distance = FastMath.interpolateLinear(distanceLerpFactor, distance, targetDistance);
                    if (targetDistance + 0.01f >= distance && targetDistance - 0.01f <= distance) {
                        distanceLerpFactor = 0;
                        chasing = false;
                    }
                }

                //linear interpolation of the distance while zooming
                if (zooming) {
                    distanceLerpFactor = Math.min(distanceLerpFactor + (tpf * tpf * zoomSensitivity), 1);
                    distance = FastMath.interpolateLinear(distanceLerpFactor, distance, targetDistance);
                    if (targetDistance + 0.1f >= distance && targetDistance - 0.1f <= distance) {
                        zooming = false;
                        distanceLerpFactor = 0;
                    }
                }

                //linear interpolation of the rotation while rotating horizontally
                if (rotating) {
                    rotationLerpFactor = Math.min(rotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                    rotation = FastMath.interpolateLinear(rotationLerpFactor, rotation, targetRotation);
                    if (targetRotation + 0.01f >= rotation && targetRotation - 0.01f <= rotation) {
                        rotating = false;
                        rotationLerpFactor = 0;
                    }
                }

                //linear interpolation of the rotation while rotating vertically
                if (vRotating) {
                    vRotationLerpFactor = Math.min(vRotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                    vRotation = FastMath.interpolateLinear(vRotationLerpFactor, vRotation, targetVRotation);
                    if (targetVRotation + 0.01f >= vRotation && targetVRotation - 0.01f <= vRotation) {
                        vRotating = false;
                        vRotationLerpFactor = 0;
                    }
                }
                //computing the position
                computePosition();
                //setting the position at last
                cam.setLocation(pos.addLocal(lookAtOffset));
            } else {
                //easy no smooth motion
                vRotation = targetVRotation;
                rotation = targetRotation;
                distance = targetDistance;
                computePosition();
                cam.setLocation(pos.addLocal(lookAtOffset));
            }
            //keeping track on the previous position of the target
            prevPos.set(targetLocation);

            //the cam looks at the target
            cam.lookAt(targetLocation, initialUpVec);

        }
    }

    /**
     * Return the enabled/disabled state of the camera
     * @return true if the camera is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the camera
     * @param enabled true to enable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            canRotate = false; // reset this flag in-case it was on before
        }
    }

    /**
     * Returns the max zoom distance of the camera (default is 40)
     * @return maxDistance
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets the max zoom distance of the camera (default is 40)
     * @param maxDistance
     */
    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        if (maxDistance < distance) {
            zoomCamera(maxDistance - distance);
        }
    }

    /**
     * Returns the min zoom distance of the camera (default is 1)
     * @return minDistance
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the min zoom distance of the camera (default is 1)
     */
    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
        if (minDistance > distance) {
            zoomCamera(distance - minDistance);
        }
    }

    /**
     * clone this camera for a spatial
     * @param spatial
     * @return
     */
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        ChaseCamera cc = new ChaseCamera(cam, spatial, inputManager);
        cc.setMaxDistance(getMaxDistance());
        cc.setMinDistance(getMinDistance());
        return cc;
    }

    @Override   
    public Object jmeClone() {
        ChaseCamera cc = new ChaseCamera(cam, inputManager);
        cc.target = target;
        cc.setMaxDistance(getMaxDistance());
        cc.setMinDistance(getMinDistance());
        return cc;
    }     

    @Override   
    public void cloneFields( Cloner cloner, Object original ) { 
        this.target = cloner.clone(target);
        computePosition();
        prevPos = new Vector3f(target.getWorldTranslation());
        cam.setLocation(pos);
    }
         
    /**
     * Sets the spacial for the camera control, should only be used internally
     * @param spatial
     */
    public void setSpatial(Spatial spatial) {
        target = spatial;
        if (spatial == null) {
            return;
        }
        computePosition();
        prevPos = new Vector3f(target.getWorldTranslation());
        cam.setLocation(pos);
    }

    /**
     * update the camera control, should only be used internally
     * @param tpf
     */
    public void update(float tpf) {
        updateCamera(tpf);
    }

    /**
     * renders the camera control, should only be used internally
     * @param rm
     * @param vp
     */
    public void render(RenderManager rm, ViewPort vp) {
        //nothing to render
    }

    /**
     * Write the camera
     * @param ex the exporter
     * @throws IOException
     */
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("remove ChaseCamera before saving");
    }

    /**
     * Read the camera
     * @param im
     * @throws IOException
     */
    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        maxDistance = ic.readFloat("maxDistance", 40);
        minDistance = ic.readFloat("minDistance", 1);
    }

    /**
     * @return The maximal vertical rotation angle in radian of the camera around the target
     */
    public float getMaxVerticalRotation() {
        return maxVerticalRotation;
    }

    /**
     * Sets the maximal vertical rotation angle in radian of the camera around the target. Default is Pi/2;
     * @param maxVerticalRotation
     */
    public void setMaxVerticalRotation(float maxVerticalRotation) {
        this.maxVerticalRotation = maxVerticalRotation;
    }

    /**
     *
     * @return The minimal vertical rotation angle in radian of the camera around the target
     */
    public float getMinVerticalRotation() {
        return minVerticalRotation;
    }

    /**
     * Sets the minimal vertical rotation angle in radian of the camera around the target default is 0;
     * @param minHeight
     */
    public void setMinVerticalRotation(float minHeight) {
        this.minVerticalRotation = minHeight;
    }

    /**
     * @return True is smooth motion is enabled for this chase camera
     */
    public boolean isSmoothMotion() {
        return smoothMotion;
    }

    /**
     * Enables smooth motion for this chase camera
     * @param smoothMotion
     */
    public void setSmoothMotion(boolean smoothMotion) {
        this.smoothMotion = smoothMotion;
    }

    /**
     * returns the chasing sensitivity
     * @return
     */
    public float getChasingSensitivity() {
        return chasingSensitivity;
    }

    /**
     *
     * Sets the chasing sensitivity, the lower the value the slower the camera will follow the target when it moves
     * default is 5
     * Only has an effect if smoothMotion is set to true and trailing is enabled
     * @param chasingSensitivity
     */
    public void setChasingSensitivity(float chasingSensitivity) {
        this.chasingSensitivity = chasingSensitivity;
    }

    /**
     * Returns the rotation sensitivity
     * @return
     */
    public float getRotationSensitivity() {
        return rotationSensitivity;
    }

    /**
     * Sets the rotation sensitivity, the lower the value the slower the camera will rotates around the target when dragging with the mouse
     * default is 5, values over 5 should have no effect.
     * If you want a significant slow down try values below 1.
     * Only has an effect if smoothMotion is set to true
     * @param rotationSensitivity
     */
    public void setRotationSensitivity(float rotationSensitivity) {
        this.rotationSensitivity = rotationSensitivity;
    }

    /**
     * returns true if the trailing is enabled
     * @return
     */
    public boolean isTrailingEnabled() {
        return trailingEnabled;
    }

    /**
     * Enable the camera trailing : The camera smoothly go in the targets trail when it moves.
     * Only has an effect if smoothMotion is set to true
     * @param trailingEnabled
     */
    public void setTrailingEnabled(boolean trailingEnabled) {
        this.trailingEnabled = trailingEnabled;
    }

    /**
     *
     * returns the trailing rotation inertia
     * @return
     */
    public float getTrailingRotationInertia() {
        return trailingRotationInertia;
    }

    /**
     * Sets the trailing rotation inertia : default is 0.1. This prevent the camera to roughtly stop when the target stops moving
     * before the camera reached the trail position.
     * Only has an effect if smoothMotion is set to true and trailing is enabled
     * @param trailingRotationInertia
     */
    public void setTrailingRotationInertia(float trailingRotationInertia) {
        this.trailingRotationInertia = trailingRotationInertia;
    }

    /**
     * returns the trailing sensitivity
     * @return
     */
    public float getTrailingSensitivity() {
        return trailingSensitivity;
    }

    /**
     * Only has an effect if smoothMotion is set to true and trailing is enabled
     * Sets the trailing sensitivity, the lower the value, the slower the camera will go in the target trail when it moves.
     * default is 0.5;
     * @param trailingSensitivity
     */
    public void setTrailingSensitivity(float trailingSensitivity) {
        this.trailingSensitivity = trailingSensitivity;
    }

    /**
     * returns the zoom sensitivity
     * @return
     */
    public float getZoomSensitivity() {
        return zoomSensitivity;
    }

    /**
     * Sets the zoom sensitivity, the lower the value, the slower the camera will zoom in and out.
     * default is 2.
     * @param zoomSensitivity
     */
    public void setZoomSensitivity(float zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }
    
    /**
     * Returns the rotation speed when the mouse is moved.
     *
     * @return the rotation speed when the mouse is moved.
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Sets the rotate amount when user moves his mouse, the lower the value,
     * the slower the camera will rotate. default is 1.
     *
     * @param rotationSpeed Rotation speed on mouse movement, default is 1.
     */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Sets the default distance at start of application
     * @param defaultDistance
     */
    public void setDefaultDistance(float defaultDistance) {
        distance = defaultDistance;
        targetDistance = distance;
    }

    /**
     * sets the default horizontal rotation in radian of the camera at start of the application
     * @param angleInRad
     */
    public void setDefaultHorizontalRotation(float angleInRad) {
        rotation = angleInRad;
        targetRotation = angleInRad;
    }

    /**
     * sets the default vertical rotation in radian of the camera at start of the application
     * @param angleInRad
     */
    public void setDefaultVerticalRotation(float angleInRad) {
        vRotation = angleInRad;
        targetVRotation = angleInRad;
    }

    /**
     * @return If drag to rotate feature is enabled.
     *
     * @see FlyByCamera#setDragToRotate(boolean)
     */
    public boolean isDragToRotate() {
        return dragToRotate;
    }

    /**
     * @param dragToRotate When true, the user must hold the mouse button
     * and drag over the screen to rotate the camera, and the cursor is
     * visible until dragged. Otherwise, the cursor is invisible at all times
     * and holding the mouse button is not needed to rotate the camera.
     * This feature is disabled by default.
     */
    public void setDragToRotate(boolean dragToRotate) {
        this.dragToRotate = dragToRotate;
        this.canRotate = !dragToRotate;
        inputManager.setCursorVisible(dragToRotate);
    }

    /**
     * @param rotateOnlyWhenClose When this flag is set to false the chase
     * camera will always rotate around its spatial independently of their
     * distance to one another. If set to true, the chase camera will only
     * be allowed to rotated below the "horizon" when the distance is smaller
     * than minDistance + 1.0f (when fully zoomed-in).
     */
    public void setDownRotateOnCloseViewOnly(boolean rotateOnlyWhenClose) {
        veryCloseRotation = rotateOnlyWhenClose;
    }

    /**
     * @return True if rotation below the vertical plane of the spatial tied
     * to the camera is allowed only when zoomed in at minDistance + 1.0f.
     * False if vertical rotation is always allowed.
     */
    public boolean getDownRotateOnCloseViewOnly() {
        return veryCloseRotation;
    }

    /**
     * return the current distance from the camera to the target
     * @return
     */
    public float getDistanceToTarget() {
        return distance;
    }

    /**
     * returns the current horizontal rotation around the target in radians
     * @return
     */
    public float getHorizontalRotation() {
        return rotation;
    }

    /**
     * returns the current vertical rotation around the target in radians.
     * @return
     */
    public float getVerticalRotation() {
        return vRotation;
    }

    /**
     * returns the offset from the target's position where the camera looks at
     * @return
     */
    public Vector3f getLookAtOffset() {
        return lookAtOffset;
    }

    /**
     * Sets the offset from the target's position where the camera looks at
     * @param lookAtOffset
     */
    public void setLookAtOffset(Vector3f lookAtOffset) {
        this.lookAtOffset = lookAtOffset;
    }

    /**
     * Sets the up vector of the camera used for the lookAt on the target
     * @param up
     */
    public void setUpVector(Vector3f up) {
        initialUpVec = up;
    }

    /**
     * Returns the up vector of the camera used for the lookAt on the target
     * @return
     */
    public Vector3f getUpVector() {
        return initialUpVec;
    }

    public boolean isHideCursorOnRotate() {
        return hideCursorOnRotate;
    }

    public void setHideCursorOnRotate(boolean hideCursorOnRotate) {
        this.hideCursorOnRotate = hideCursorOnRotate;
    }

    /**
     * invert the vertical axis movement of the mouse
     * @param invertYaxis
     */
    public void setInvertVerticalAxis(boolean invertYaxis) {
        this.invertYaxis = invertYaxis;
        inputManager.deleteMapping(CameraInput.CHASECAM_DOWN);
        inputManager.deleteMapping(CameraInput.CHASECAM_UP);
        if (!invertYaxis) {
            inputManager.addMapping(CameraInput.CHASECAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
            inputManager.addMapping(CameraInput.CHASECAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        } else {
            inputManager.addMapping(CameraInput.CHASECAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
            inputManager.addMapping(CameraInput.CHASECAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        }
        inputManager.addListener(this, CameraInput.CHASECAM_DOWN, CameraInput.CHASECAM_UP);
    }

    /**
     * invert the Horizontal axis movement of the mouse
     * @param invertXaxis
     */
    public void setInvertHorizontalAxis(boolean invertXaxis) {
        this.invertXaxis = invertXaxis;
        inputManager.deleteMapping(CameraInput.CHASECAM_MOVELEFT);
        inputManager.deleteMapping(CameraInput.CHASECAM_MOVERIGHT);
        if (!invertXaxis) {
            inputManager.addMapping(CameraInput.CHASECAM_MOVELEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
            inputManager.addMapping(CameraInput.CHASECAM_MOVERIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        } else {
            inputManager.addMapping(CameraInput.CHASECAM_MOVELEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
            inputManager.addMapping(CameraInput.CHASECAM_MOVERIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        }
        inputManager.addListener(this, CameraInput.CHASECAM_MOVELEFT, CameraInput.CHASECAM_MOVERIGHT);
    }
}
