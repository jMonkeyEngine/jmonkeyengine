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
 * <p>
 * After creation, you (or FlyCamAppState) must register the controller using
 * {@link #registerWithInput(com.jme3.input.InputManager)}.
 * <p>
 * Controls:
 *  - Move (or, in drag-to-rotate mode, drag) the mouse to rotate the camera
 *  - Mouse wheel for zooming in or out
 *  - WASD keys for moving forward/backward and strafing
 *  - QZ keys raise or lower the camera
 */
public class FlyByCamera implements AnalogListener, ActionListener {

    private static final String[] mappings = new String[]{
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
    protected Vector3f initialUpVec = new Vector3f();
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

    // Reusable temporary objects to reduce allocations during updates
    private final Matrix3f tempMat = new Matrix3f();
    private final Quaternion tempQuat = new Quaternion();
    private final Vector3f tempUp = new Vector3f();
    private final Vector3f tempLeft = new Vector3f();
    private final Vector3f tempDir = new Vector3f();
    private final Vector3f tempVel = new Vector3f();
    private final Vector3f tempPos = new Vector3f();

    /**
     * Creates a new FlyByCamera to control the specified camera.
     *
     * @param cam camera to be controlled (not null)
     */
    public FlyByCamera(Camera cam) {
        this.cam = cam;
        cam.getUp(initialUpVec);
    }

    /**
     * Sets the up vector that should be used for the camera.
     *
     * @param upVec the desired direction (not null, unaffected)
     */
    public void setUpVector(Vector3f upVec) {
        initialUpVec.set(upVec);
    }

    public void setMotionAllowedListener(MotionAllowedListener listener) {
        this.motionAllowed = listener;
    }

    /**
     * Sets the translation speed of the camera.
     *
     * @param moveSpeed The new translation speed in world units per second. Must be non-negative.
     */
    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    /**
     * Retrieves the current translation speed of the camera.
     *
     * @return The current speed in world units per second.
     */
    public float getMoveSpeed() {
        return moveSpeed;
    }

    /**
     * Sets the rotation-rate multiplier for mouse input. A higher value
     * means the camera rotates more for a given mouse movement.
     *
     * @param rotationSpeed The new rate multiplier (1.0 is default). Must be non-negative.
     */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Retrieves the current rotation-rate multiplier.
     *
     * @return The current rate multiplier.
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Sets the zoom-rate multiplier for mouse wheel input. A higher value
     * means the camera zooms more for a given mouse wheel scroll.
     *
     * @param zoomSpeed The new rate multiplier (1.0 is default). Must be non-negative.
     */
    public void setZoomSpeed(float zoomSpeed) {
        this.zoomSpeed = zoomSpeed;
    }

    /**
     * Retrieves the current zoom-rate multiplier.
     *
     * @return The current rate multiplier.
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
    public void setEnabled(boolean enable) {
        if (enabled && !enable) {
            if (inputManager != null && (!dragToRotate || (dragToRotate && canRotate))) {
                inputManager.setCursorVisible(true);
            }
        }
        enabled = enable;
    }

    /**
     * Checks whether this camera controller is currently enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise.
     * @see #setEnabled(boolean)
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks whether drag-to-rotate mode is currently enabled.
     *
     * @return {@code true} if drag-to-rotate is enabled, {@code false} otherwise.
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
     * Registers this controller to receive input events from the specified
     * {@link InputManager}. This method sets up all the necessary input mappings
     * for mouse, keyboard, and joysticks.
     *
     * @param inputManager The InputManager instance to register with (must not be null).
     */
    public void registerWithInput(InputManager inputManager) {
        this.inputManager = inputManager;

        // Mouse and Keyboard Mappings for Rotation
        inputManager.addMapping(CameraInput.FLYCAM_LEFT, new MouseAxisTrigger(MouseInput.AXIS_X, true),
                new KeyTrigger(KeyInput.KEY_LEFT));

        inputManager.addMapping(CameraInput.FLYCAM_RIGHT, new MouseAxisTrigger(MouseInput.AXIS_X, false),
                new KeyTrigger(KeyInput.KEY_RIGHT));

        inputManager.addMapping(CameraInput.FLYCAM_UP, new MouseAxisTrigger(MouseInput.AXIS_Y, false),
                new KeyTrigger(KeyInput.KEY_UP));

        inputManager.addMapping(CameraInput.FLYCAM_DOWN, new MouseAxisTrigger(MouseInput.AXIS_Y, true),
                new KeyTrigger(KeyInput.KEY_DOWN));

        // Mouse Mappings for Zoom and Drag-to-Rotate
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
        if (joysticks != null && joysticks.length > 0) {
            for (Joystick j : joysticks) {
                mapJoystick(j);
            }
        }
    }

    /**
     * Configures joystick input mappings for the camera controller. This method
     * attempts to map joystick axes and buttons to camera actions.
     *
     * @param joystick The {@link Joystick} to map (not null).
     */
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

            if (joystick.getButton("Button 8") != null) {
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
     * Unregisters this controller from its currently associated {@link InputManager}.
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

        // Joysticks cannot be "unassigned" in the same way, but mappings are removed with listener.
        // Joystick-specific mapping might persist but won't trigger this listener.
        inputManager = null; // Clear reference
    }

    /**
     * Rotates the camera by the specified amount around the given axis.
     *
     * @param value The amount of rotation.
     * @param axis  The axis around which to rotate (a unit vector, unaffected).
     */
    protected void rotateCamera(float value, Vector3f axis) {
        if (dragToRotate && !canRotate) {
            return; // In drag-to-rotate mode, only rotate if canRotate is true.
        }

        tempMat.fromAngleNormalAxis(rotationSpeed * value, axis);

        // Get current camera axes into temporary vectors
        cam.getUp(tempUp);
        cam.getLeft(tempLeft);
        cam.getDirection(tempDir);

        // Apply rotation to the camera's axes
        tempMat.mult(tempUp, tempUp);
        tempMat.mult(tempLeft, tempLeft);
        tempMat.mult(tempDir, tempDir);

        // Set camera axes using a temporary Quaternion
        tempQuat.fromAxes(tempLeft, tempUp, tempDir);
        tempQuat.normalizeLocal(); // Ensure quaternion is normalized

        cam.setAxes(tempQuat);
    }

    /**
     * Zooms the camera by the specified amount. This method handles both
     * perspective and parallel projections.
     *
     * @param value The amount to zoom. Positive values typically zoom in, negative out.
     */
    protected void zoomCamera(float value) {
        if (cam.isParallelProjection()) {
            float zoomFactor = 1.0F + value * 0.01F * zoomSpeed;
            if (zoomFactor > 0F) {
                float left   = zoomFactor * cam.getFrustumLeft();
                float right  = zoomFactor * cam.getFrustumRight();
                float top    = zoomFactor * cam.getFrustumTop();
                float bottom = zoomFactor * cam.getFrustumBottom();
                float near   = cam.getFrustumNear();
                float far    = cam.getFrustumFar();
                cam.setFrustum(near, far, left, right, top, bottom);
            }

        } else { // perspective projection
            float newFov = cam.getFov() + value * 0.1F * zoomSpeed;
            // Use a small epsilon to prevent near-zero FoV issues
            if (newFov > 0.01f) {
                cam.setFov(newFov);
            }
        }
    }

    /**
     * Translates the camera vertically (up or down) by the specified amount,
     * considering the {@code initialUpVec}.
     *
     * @param value The translation amount. Positive values move the camera up, negative down.
     */
    protected void riseCamera(float value) {
        tempVel.set(initialUpVec).multLocal(value * moveSpeed);
        tempPos.set(cam.getLocation());

        if (motionAllowed != null) {
            motionAllowed.checkMotionAllowed(tempPos.clone(), tempVel.clone());
        } else {
            tempPos.addLocal(tempVel);
        }

        cam.setLocation(tempPos);
    }

    /**
     * Translates the camera left/right or forward/backward by the specified amount.
     *
     * @param value    The translation amount. Positive values move in the primary
     *                 direction (right/forward), negative in the opposite.
     * @param sideways If {@code true}, the camera moves left/right (strafes).
     *                 If {@code false}, the camera moves forward/backward.
     */
    protected void moveCamera(float value, boolean sideways) {
        if (sideways) {
            cam.getLeft(tempVel);
        } else {
            cam.getDirection(tempVel);
        }
        tempVel.multLocal(value * moveSpeed);
        tempPos.set(cam.getLocation());

        if (motionAllowed != null) {
            motionAllowed.checkMotionAllowed(tempPos.clone(), tempVel.clone());
        } else {
            tempPos.addLocal(tempVel);
        }

        cam.setLocation(tempPos);
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
            rotateCamera(-value * (invertY ? -1 : 1), cam.getLeft(tempLeft));
        } else if (name.equals(CameraInput.FLYCAM_DOWN)) {
            rotateCamera(value * (invertY ? -1 : 1), cam.getLeft(tempLeft));
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
     * @param isPressed true if the action is "pressed", false otherwise
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!enabled)
            return;

        if (name.equals(CameraInput.FLYCAM_ROTATEDRAG) && dragToRotate) {
            canRotate = isPressed;
            inputManager.setCursorVisible(!isPressed);
        } else if (name.equals(CameraInput.FLYCAM_INVERTY)) {
            // Invert the "up" direction.
            if (!isPressed) {
                invertY = !invertY;
            }
        }
    }
}
