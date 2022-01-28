/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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

import com.jme3.collision.MotionAllowedListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * A first-person camera controller.
 *
 * After creation, you (or FlyCamAppState) must register the controller using
 * {@link #registerWithInput(com.jme3.input.InputManager)}.
 *
 * Controls:
 *  - Move (or, in drag-to-rotate mode, drag) the mouse to rotate the camera
 *  - Mouse wheel for zooming in or out
 *  - WASD keys for moving forward/backward and strafing
 *  - QZ keys raise or lower the camera
 */
public class FlyByCamera implements AnalogListener, ActionListener {

    final private static String[] mappings = new String[]{
        CameraInput.FLYCAM_LEFT,
        CameraInput.FLYCAM_RIGHT,
        CameraInput.FLYCAM_UP,
        CameraInput.FLYCAM_DOWN,

        CameraInput.FLYCAM_STRAFELEFT,
        CameraInput.FLYCAM_STRAFERIGHT,
        CameraInput.FLYCAM_FORWARD,
        CameraInput.FLYCAM_BACKWARD,

        CameraInput.FLYCAM_ZOOMIN,
        CameraInput.FLYCAM_ZOOMOUT,
        CameraInput.FLYCAM_ROTATEDRAG,

        CameraInput.FLYCAM_RISE,
        CameraInput.FLYCAM_LOWER,

        CameraInput.FLYCAM_INVERTY
    };
    /**
     * camera controlled by this controller (not null)
     */
    protected Camera cam;
    /**
     * normalized "up" direction (a unit vector)
     */
    protected Vector3f initialUpVec;
    /**
     * rotation-rate multiplier (1=default)
     */
    protected float rotationSpeed = 1f;
    /**
     * translation speed (in world units per second)
     */
    protected float moveSpeed = 3f;
    /**
     * zoom-rate multiplier (1=default)
     */
    protected float zoomSpeed = 1f;
    protected MotionAllowedListener motionAllowed = null;
    /**
     * enable flag for controller (false&rarr;ignoring input)
     */
    protected boolean enabled = true;
    /**
     * drag-to-rotate mode flag
     */
    protected boolean dragToRotate = false;
    protected boolean canRotate = false;
    protected boolean invertY = false;
    protected InputManager inputManager;

    /**
     * Creates a new FlyByCamera to control the specified camera.
     *
     * @param cam camera to be controlled (not null)
     */
    public FlyByCamera(Camera cam) {
        this.cam = cam;
        initialUpVec = cam.getUp().clone();
    }

    /**
     * Sets the up vector that should be used for the camera.
     *
     * @param upVec the desired direction (not null, unaffected)
     */
    public void setUpVector(Vector3f upVec) {
        initialUpVec.set(upVec);
    }

    public void setMotionAllowedListener(MotionAllowedListener listener){
        this.motionAllowed = listener;
    }

    /**
     * Set the translation speed.
     *
     * @param moveSpeed new speed (in world units per second)
     */
    public void setMoveSpeed(float moveSpeed){
        this.moveSpeed = moveSpeed;
    }

    /**
     * Read the translation speed.
     *
     * @return current speed (in world units per second)
     */
    public float getMoveSpeed(){
        return moveSpeed;
    }

    /**
     * Set the rotation-rate multiplier. The bigger the multiplier, the more
     * rotation for a given movement of the mouse.
     *
     * @param rotationSpeed new rate multiplier (1=default)
     */
    public void setRotationSpeed(float rotationSpeed){
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Read the rotation-rate multiplier. The bigger the multiplier, the more
     * rotation for a given movement of the mouse.
     *
     * @return current rate multiplier (1=default)
     */
    public float getRotationSpeed(){
        return rotationSpeed;
    }

    /**
     * Set the zoom-rate multiplier. The bigger the multiplier, the more zoom
     * for a given movement of the mouse wheel.
     *
     * @param zoomSpeed new rate multiplier (1=default)
     */
    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = zoomSpeed;
    }

    /**
     * Read the zoom-rate multiplier. The bigger the multiplier, the more zoom
     * for a given movement of the mouse wheel.
     *
     * @return current rate multiplier (1=default)
     */
    public float getZoomSpeed() {
        return zoomSpeed;
    }

    /**
     * Enable or disable this controller. When disabled, the controller ignored
     * input.
     *
     * @param enable true to enable, false to disable
     */
    public void setEnabled(boolean enable){
        if (enabled && !enable){
            if (inputManager!= null && (!dragToRotate || (dragToRotate && canRotate))){
                inputManager.setCursorVisible(true);
            }
        }
        enabled = enable;
    }

    /**
     * Test whether this controller is enabled.
     *
     * @return true if enabled, otherwise false
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled(){
        return enabled;
    }

    /**
     * Test whether drag-to-rotate mode is enabled.
     *
     * @return If drag to rotate feature is enabled.
     *
     * @see #setDragToRotate(boolean)
     */
    public boolean isDragToRotate() {
        return dragToRotate;
    }

    /**
     * Enable or disable drag-to-rotate mode.
     *
     * When drag-to-rotate mode is enabled, the user must hold the mouse button
     * and drag over the screen to rotate the camera, and the cursor is visible
     * until dragged. When drag-to-rotate mode is disabled, the cursor is
     * invisible at all times and holding the mouse button is not needed to
     * rotate the camera. This mode is disabled by default.
     *
     * @param dragToRotate true to enable, false to disable
     */
    public void setDragToRotate(boolean dragToRotate) {
        this.dragToRotate = dragToRotate;
        if (inputManager != null) {
            inputManager.setCursorVisible(dragToRotate);
        }
    }

    /**
     * Register this controller to receive input events from the specified input
     * manager.
     *
     * @param inputManager (not null, alias created)
     */
    public void registerWithInput(InputManager inputManager){
        this.inputManager = inputManager;

        // both mouse and button - rotation of cam
        inputManager.addMapping(CameraInput.FLYCAM_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true),
                new KeyTrigger(KeyInput.KEY_LEFT));

        inputManager.addMapping(CameraInput.FLYCAM_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false),
                new KeyTrigger(KeyInput.KEY_RIGHT));

        inputManager.addMapping(CameraInput.FLYCAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false),
                new KeyTrigger(KeyInput.KEY_UP));

        inputManager.addMapping(CameraInput.FLYCAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true),
                new KeyTrigger(KeyInput.KEY_DOWN));

        // mouse only - zoom in/out with wheel, and rotate drag
        inputManager.addMapping(CameraInput.FLYCAM_ZOOMIN, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(CameraInput.FLYCAM_ZOOMOUT, new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping(CameraInput.FLYCAM_ROTATEDRAG, new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        // keyboard only WASD for movement and WZ for rise/lower height
        inputManager.addMapping(CameraInput.FLYCAM_STRAFELEFT, new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping(CameraInput.FLYCAM_STRAFERIGHT, new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping(CameraInput.FLYCAM_FORWARD, new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping(CameraInput.FLYCAM_BACKWARD, new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping(CameraInput.FLYCAM_RISE, new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping(CameraInput.FLYCAM_LOWER, new KeyTrigger(KeyInput.KEY_Z));

        inputManager.addListener(this, mappings);
        inputManager.setCursorVisible(dragToRotate || !isEnabled());

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null && joysticks.length > 0){
            for (Joystick j : joysticks) {
                mapJoystick(j);
            }
        }
    }

    protected void mapJoystick(Joystick joystick) {
        // Map it differently if there are Z axis
        if (joystick.getAxis(JoystickAxis.Z_ROTATION) != null
                && joystick.getAxis(JoystickAxis.Z_AXIS) != null) {

            // Make the left stick move
            joystick.getXAxis().assignAxis(CameraInput.FLYCAM_STRAFERIGHT, CameraInput.FLYCAM_STRAFELEFT);
            joystick.getYAxis().assignAxis(CameraInput.FLYCAM_BACKWARD, CameraInput.FLYCAM_FORWARD);

            // And the right stick control the camera
            joystick.getAxis(JoystickAxis.Z_ROTATION)
                    .assignAxis(CameraInput.FLYCAM_DOWN, CameraInput.FLYCAM_UP);
            joystick.getAxis(JoystickAxis.Z_AXIS)
                    .assignAxis(CameraInput.FLYCAM_RIGHT, CameraInput.FLYCAM_LEFT);

            // And let the dpad be up and down
            joystick.getPovYAxis().assignAxis(CameraInput.FLYCAM_RISE, CameraInput.FLYCAM_LOWER);

            if( joystick.getButton("Button 8") != null) {
                // Let the standard select button be the y invert toggle
                joystick.getButton("Button 8").assignButton(CameraInput.FLYCAM_INVERTY);
            }

        } else {
            joystick.getPovXAxis().assignAxis(CameraInput.FLYCAM_STRAFERIGHT, CameraInput.FLYCAM_STRAFELEFT);
            joystick.getPovYAxis().assignAxis(CameraInput.FLYCAM_FORWARD, CameraInput.FLYCAM_BACKWARD);
            joystick.getXAxis().assignAxis(CameraInput.FLYCAM_RIGHT, CameraInput.FLYCAM_LEFT);
            joystick.getYAxis().assignAxis(CameraInput.FLYCAM_DOWN, CameraInput.FLYCAM_UP);
        }
    }

    /**
     * Unregister this controller from its input manager.
     */
    public void unregisterInput() {
        if (inputManager == null) {
            return;
        }

        for (String s : mappings) {
            if (inputManager.hasMapping(s)) {
                inputManager.deleteMapping(s);
            }
        }

        inputManager.removeListener(this);
        inputManager.setCursorVisible(!dragToRotate);

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null && joysticks.length > 0) {
            // No way to unassign axis
        }
    }

    /**
     * Rotate the camera by the specified amount around the specified axis.
     *
     * @param value rotation amount
     * @param axis direction of rotation (a unit vector)
     */
    protected void rotateCamera(float value, Vector3f axis) {
        if (dragToRotate) {
            if (canRotate) {
//                value = -value;
            } else {
                return;
            }
        }

        Matrix3f mat = new Matrix3f();
        mat.fromAngleNormalAxis(rotationSpeed * value, axis);

        Vector3f up = cam.getUp();
        Vector3f left = cam.getLeft();
        Vector3f dir = cam.getDirection();

        mat.mult(up, up);
        mat.mult(left, left);
        mat.mult(dir, dir);

        Quaternion q = new Quaternion();
        q.fromAxes(left, up, dir);
        q.normalizeLocal();

        cam.setAxes(q);
    }

    /**
     * Zoom the camera by the specified amount.
     *
     * @param value zoom amount
     */
    protected void zoomCamera(float value) {
        float newFov = cam.getFov() + value * 0.1F * zoomSpeed;
        if (newFov > 0) {
            cam.setFov(newFov);
        }
    }

    /**
     * Translate the camera upward by the specified amount.
     *
     * @param value translation amount
     */
    protected void riseCamera(float value) {
        Vector3f vel = initialUpVec.mult(value * moveSpeed);
        Vector3f pos = cam.getLocation().clone();

        if (motionAllowed != null)
            motionAllowed.checkMotionAllowed(pos, vel);
        else
            pos.addLocal(vel);

        cam.setLocation(pos);
    }

    /**
     * Translate the camera left or forward by the specified amount.
     *
     * @param value translation amount
     * @param sideways true&rarr;left, false&rarr;forward
     */
    protected void moveCamera(float value, boolean sideways) {
        Vector3f vel = new Vector3f();
        Vector3f pos = cam.getLocation().clone();

        if (sideways){
            cam.getLeft(vel);
        } else {
            cam.getDirection(vel);
        }
        vel.multLocal(value * moveSpeed);

        if (motionAllowed != null)
            motionAllowed.checkMotionAllowed(pos, vel);
        else
            pos.addLocal(vel);

        cam.setLocation(pos);
    }

    /**
     * Callback to notify this controller of an analog input event.
     *
     * @param name name of the input event
     * @param value value of the axis (from 0 to 1)
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!enabled)
            return;

        if (name.equals(CameraInput.FLYCAM_LEFT)) {
            rotateCamera(value, initialUpVec);
        } else if (name.equals(CameraInput.FLYCAM_RIGHT)) {
            rotateCamera(-value, initialUpVec);
        } else if (name.equals(CameraInput.FLYCAM_UP)) {
            rotateCamera(-value * (invertY ? -1 : 1), cam.getLeft());
        } else if (name.equals(CameraInput.FLYCAM_DOWN)) {
            rotateCamera(value * (invertY ? -1 : 1), cam.getLeft());
        } else if (name.equals(CameraInput.FLYCAM_FORWARD)) {
            moveCamera(value, false);
        } else if (name.equals(CameraInput.FLYCAM_BACKWARD)) {
            moveCamera(-value, false);
        } else if (name.equals(CameraInput.FLYCAM_STRAFELEFT)) {
            moveCamera(value, true);
        } else if (name.equals(CameraInput.FLYCAM_STRAFERIGHT)) {
            moveCamera(-value, true);
        } else if (name.equals(CameraInput.FLYCAM_RISE)) {
            riseCamera(value);
        } else if (name.equals(CameraInput.FLYCAM_LOWER)) {
            riseCamera(-value);
        } else if (name.equals(CameraInput.FLYCAM_ZOOMIN)) {
            zoomCamera(value);
        } else if (name.equals(CameraInput.FLYCAM_ZOOMOUT)) {
            zoomCamera(-value);
        }
    }

    /**
     * Callback to notify this controller of an action input event.
     *
     * @param name name of the input event
     * @param value true if the action is "pressed", false otherwise
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void onAction(String name, boolean value, float tpf) {
        if (!enabled)
            return;

        if (name.equals(CameraInput.FLYCAM_ROTATEDRAG) && dragToRotate) {
            canRotate = value;
            inputManager.setCursorVisible(!value);
        } else if (name.equals(CameraInput.FLYCAM_INVERTY)) {
            // Invert the "up" direction.
            if (!value) {
                invertY = !invertY;
            }
        }
    }
}
