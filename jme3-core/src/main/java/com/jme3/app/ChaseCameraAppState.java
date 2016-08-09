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
package com.jme3.app;

import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.CameraInput;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.CameraNode;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.CameraControl;
import com.jme3.util.TempVars;

/**
 * This class is a camera controller that allow the camera to follow a target
 * Spatial.
 *
 * @author Nehon
 */
public class ChaseCameraAppState extends AbstractAppState implements ActionListener, AnalogListener {

    protected Spatial spatial;
    protected Node target;
    protected CameraNode camNode;
    protected InputManager inputManager;
    protected boolean invertYaxis = false;
    protected boolean invertXaxis = false;
    protected boolean hideCursorOnRotate = true;
    protected boolean canRotate;
    protected boolean dragToRotate = true;
    protected float rotationSpeed = 1.0f;
    protected float zoomSpeed = 2.0f;
    //protected boolean zoomin;
    protected float minDistance = 1.0f;
    protected float maxDistance = 40.0f;
    protected float distance = 20;
    protected float maxVerticalRotation = 1.4f;
    protected float verticalRotation = 0f;
    protected float minVerticalRotation = 0f;
    protected float horizontalRotation = 0f;
    //protected float distanceLerpFactor = 0;
    protected Vector3f upVector = new Vector3f();
    protected Vector3f leftVector = new Vector3f();
    protected Trigger[] zoomOutTrigger = {new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true)};
    protected Trigger[] zoomInTrigger = {new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false)};
    protected Trigger[] toggleRotateTrigger = {new MouseButtonTrigger(MouseInput.BUTTON_LEFT), new MouseButtonTrigger(MouseInput.BUTTON_RIGHT)};

//
//    protected boolean rotating = false;
//    protected float rotation = 0;
//    protected float targetRotation = rotation;
    public ChaseCameraAppState() {
        camNode = new CameraNode("ChaseCameraNode", new CameraControl());
    }

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        this.inputManager = app.getInputManager();
        target = new Node("ChaseCamTarget");
        camNode.setCamera(app.getCamera());        
        camNode.setControlDir(CameraControl.ControlDirection.SpatialToCamera);
        target.attachChild(camNode);
        camNode.setLocalTranslation(0, 0, distance);
        upVector = app.getCamera().getUp().clone();
        leftVector = app.getCamera().getLeft().clone();
        registerWithInput();
        rotateCamera();
    }

    /**
     * Registers inputs with the input manager
     *
     */
    public final void registerWithInput() {

        String[] inputs = {CameraInput.CHASECAM_TOGGLEROTATE,
            CameraInput.CHASECAM_DOWN,
            CameraInput.CHASECAM_UP,
            CameraInput.CHASECAM_MOVELEFT,
            CameraInput.CHASECAM_MOVERIGHT,
            CameraInput.CHASECAM_ZOOMIN,
            CameraInput.CHASECAM_ZOOMOUT};
        initVerticalAxisInputs();
        initZoomInput();
        initHorizontalAxisInput();
        initTogleRotateInput();

        inputManager.addListener(this, inputs);
        inputManager.setCursorVisible(dragToRotate);
    }

    public void onAction(String name, boolean keyPressed, float tpf) {
        if (isEnabled()) {
            if (dragToRotate) {
                if (name.equals(CameraInput.CHASECAM_TOGGLEROTATE) && isEnabled()) {
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

    }

    public void onAnalog(String name, float value, float tpf) {
        if (isEnabled()) {
            if (canRotate) {
                if (name.equals(CameraInput.CHASECAM_MOVELEFT)) {
                    horizontalRotation -= value * rotationSpeed;
                    rotateCamera();
                } else if (name.equals(CameraInput.CHASECAM_MOVERIGHT)) {
                    horizontalRotation += value * rotationSpeed;
                    rotateCamera();
                } else if (name.equals(CameraInput.CHASECAM_UP)) {
                    verticalRotation += value * rotationSpeed;
                    rotateCamera();
                } else if (name.equals(CameraInput.CHASECAM_DOWN)) {
                    verticalRotation -= value * rotationSpeed;
                    rotateCamera();
                }
            }
            if (name.equals(CameraInput.CHASECAM_ZOOMIN)) {
                zoomCamera(-value * zoomSpeed);
            } else if (name.equals(CameraInput.CHASECAM_ZOOMOUT)) {
                zoomCamera(+value * zoomSpeed);
            }
        }
    }

    /**
     * rotate the camera around the target
     */
    protected void rotateCamera() {
        verticalRotation = FastMath.clamp(verticalRotation, minVerticalRotation, maxVerticalRotation);
        TempVars vars = TempVars.get();
        Quaternion rot = vars.quat1;
        Quaternion rot2 = vars.quat2;
        rot.fromAngleNormalAxis(verticalRotation, leftVector);
        rot2.fromAngleNormalAxis(horizontalRotation, upVector);
        rot2.multLocal(rot);
        target.setLocalRotation(rot2);
        vars.release();
    }

    /**
     * move the camera toward or away the target
     */
    protected void zoomCamera(float value) {
        distance = FastMath.clamp(distance + value, minDistance, maxDistance);
        camNode.setLocalTranslation(new Vector3f(0, 0, distance));
    }

    public void setTarget(Spatial targetSpatial) {
        spatial = targetSpatial;
    }

    @Override
    public void update(float tpf) {
        if (spatial == null) {
            throw new IllegalArgumentException("The spatial to follow is null, please use the setTarget method");
        }
        target.setLocalTranslation(spatial.getWorldTranslation());
        camNode.lookAt(target.getWorldTranslation(), upVector);

        target.updateLogicalState(tpf);
        target.updateGeometricState();
    }

    /**
     * Sets custom triggers for toggling the rotation of the cam default are
     * new MouseButtonTrigger(MouseInput.BUTTON_LEFT) left mouse button new
     * MouseButtonTrigger(MouseInput.BUTTON_RIGHT) right mouse button
     *
     * @param triggers
     */
    public void setToggleRotationTrigger(Trigger... triggers) {
        toggleRotateTrigger = triggers;
        if (inputManager != null) {
            inputManager.deleteMapping(CameraInput.CHASECAM_TOGGLEROTATE);
            initTogleRotateInput();
            inputManager.addListener(this, CameraInput.CHASECAM_TOGGLEROTATE);
        }
    }

    /**
     * Sets custom triggers for zooming in the cam default is new
     * MouseAxisTrigger(MouseInput.AXIS_WHEEL, true) mouse wheel up
     *
     * @param triggers
     */
    public void setZoomInTrigger(Trigger... triggers) {
        zoomInTrigger = triggers;
        if (inputManager != null) {
            inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMIN);
            inputManager.addMapping(CameraInput.CHASECAM_ZOOMIN, zoomInTrigger);
            inputManager.addListener(this, CameraInput.CHASECAM_ZOOMIN);
        }
    }

    /**
     * Sets custom triggers for zooming out the cam default is new
     * MouseAxisTrigger(MouseInput.AXIS_WHEEL, false) mouse wheel down
     *
     * @param triggers
     */
    public void setZoomOutTrigger(Trigger... triggers) {
        zoomOutTrigger = triggers;
        if (inputManager != null) {
            inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMOUT);
            inputManager.addMapping(CameraInput.CHASECAM_ZOOMOUT, zoomOutTrigger);
            inputManager.addListener(this, CameraInput.CHASECAM_ZOOMOUT);
        }
    }

    /**
     * Returns the max zoom distance of the camera (default is 40)
     *
     * @return maxDistance
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets the max zoom distance of the camera (default is 40)
     *
     * @param maxDistance
     */
    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        if(initialized){
            zoomCamera(distance);
        }
    }

    /**
     * Returns the min zoom distance of the camera (default is 1)
     *
     * @return minDistance
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the min zoom distance of the camera (default is 1)
     * 
     * @param minDistance
     */
    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
        if(initialized){
            zoomCamera(distance);
        }
    }

    /**
     * @return The maximal vertical rotation angle in radian of the camera
     * around the target
     */
    public float getMaxVerticalRotation() {
        return maxVerticalRotation;
    }

    /**
     * Sets the maximal vertical rotation angle in radian of the camera around
     * the target. Default is Pi/2;
     *
     * @param maxVerticalRotation
     */
    public void setMaxVerticalRotation(float maxVerticalRotation) {
        this.maxVerticalRotation = maxVerticalRotation;
        if(initialized){
            rotateCamera();
        }
    }

    /**
     *
     * @return The minimal vertical rotation angle in radian of the camera
     * around the target
     */
    public float getMinVerticalRotation() {
        return minVerticalRotation;
    }

    /**
     * Sets the minimal vertical rotation angle in radian of the camera around
     * the target default is 0;
     *
     * @param minHeight
     */
    public void setMinVerticalRotation(float minHeight) {
        this.minVerticalRotation = minHeight;
        if(initialized){
            rotateCamera();
        }
    }

    /**
     * returns the zoom speed
     *
     * @return
     */
    public float getZoomSpeed() {
        return zoomSpeed;
    }

    /**
     * Sets the zoom speed, the lower the value, the slower the camera will zoom
     * in and out. default is 2.
     *
     * @param zoomSpeed
     */
    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = zoomSpeed;
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
     *
     * @param defaultDistance
     */
    public void setDefaultDistance(float defaultDistance) {
        distance = defaultDistance;
    }

    /**
     * sets the default horizontal rotation in radian of the camera at start of
     * the application
     *
     * @param angleInRad
     */
    public void setDefaultHorizontalRotation(float angleInRad) {
        horizontalRotation = angleInRad;
    }

    /**
     * sets the default vertical rotation in radian of the camera at start of
     * the application
     *
     * @param angleInRad
     */
    public void setDefaultVerticalRotation(float angleInRad) {
        verticalRotation = angleInRad;
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
     * @param dragToRotate When true, the user must hold the mouse button and
     * drag over the screen to rotate the camera, and the cursor is visible
     * until dragged. Otherwise, the cursor is invisible at all times and
     * holding the mouse button is not needed to rotate the camera. This feature
     * is disabled by default.
     */
    public void setDragToRotate(boolean dragToRotate) {
        this.dragToRotate = dragToRotate;
        this.canRotate = !dragToRotate;
        if(inputManager != null){
            inputManager.setCursorVisible(dragToRotate);
        }
    }

    /**
     * invert the vertical axis movement of the mouse
     *
     * @param invertYaxis
     */
    public void setInvertVerticalAxis(boolean invertYaxis) {
        this.invertYaxis = invertYaxis;
        if (inputManager != null) {
            inputManager.deleteMapping(CameraInput.CHASECAM_DOWN);
            inputManager.deleteMapping(CameraInput.CHASECAM_UP);
            initVerticalAxisInputs();
            inputManager.addListener(this, CameraInput.CHASECAM_DOWN, CameraInput.CHASECAM_UP);
        }
    }

    /**
     * invert the Horizontal axis movement of the mouse
     *
     * @param invertXaxis
     */
    public void setInvertHorizontalAxis(boolean invertXaxis) {
        this.invertXaxis = invertXaxis;
        if (inputManager != null) {
            inputManager.deleteMapping(CameraInput.CHASECAM_MOVELEFT);
            inputManager.deleteMapping(CameraInput.CHASECAM_MOVERIGHT);
            initHorizontalAxisInput();
            inputManager.addListener(this, CameraInput.CHASECAM_MOVELEFT, CameraInput.CHASECAM_MOVERIGHT);
        }
    }

    private void initVerticalAxisInputs() {
        if (!invertYaxis) {
            inputManager.addMapping(CameraInput.CHASECAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
            inputManager.addMapping(CameraInput.CHASECAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
        } else {
            inputManager.addMapping(CameraInput.CHASECAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, false));
            inputManager.addMapping(CameraInput.CHASECAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, true));
        }
    }

    private void initHorizontalAxisInput() {
        if (!invertXaxis) {
            inputManager.addMapping(CameraInput.CHASECAM_MOVELEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
            inputManager.addMapping(CameraInput.CHASECAM_MOVERIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
        } else {
            inputManager.addMapping(CameraInput.CHASECAM_MOVELEFT, new MouseAxisTrigger(MouseInput.AXIS_X, false));
            inputManager.addMapping(CameraInput.CHASECAM_MOVERIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, true));
        }
    }

    private void initZoomInput() {
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMIN, zoomInTrigger);
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMOUT, zoomOutTrigger);
    }

    private void initTogleRotateInput() {
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE, toggleRotateTrigger);
    }
}
