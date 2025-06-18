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

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.MouseAxisTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.input.controls.Trigger;
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
 * A camera control that follows a {@link Spatial} target, allowing for
 * orbiting, zooming, and smooth trailing motion. This control provides
 * configurable input mappings and interpolation for a fluid camera experience.
 * <p>
 * The camera can be controlled via mouse input for rotation and zooming.
 * Smooth motion features allow for gradual transitions when the target moves
 * or when the user manipulates the camera.
 *
 * @author nehon
 */
public class ChaseCamera implements ActionListener, AnalogListener, Control, JmeCloneable {

    // --- Camera State and Configuration Fields ---
    protected Spatial target = null;
    protected Camera cam = null;
    protected InputManager inputManager;

    protected float minVerticalRotation = 0.00f;
    protected float maxVerticalRotation = FastMath.PI / 2;
    protected float minDistance = 1.0f;
    protected float maxDistance = 40.0f;
    protected float distance = 20; // Current distance from target
    protected float rotation = 0;  // Current horizontal rotation angle
    protected float vRotation = FastMath.PI / 6; // Current vertical rotation angle

    protected float rotationSpeed = 1.0f;
    protected float zoomSensitivity = 2f;
    protected float rotationSensitivity = 5f;
    protected float chasingSensitivity = 5f;
    protected float trailingSensitivity = 0.5f;
    protected float trailingRotationInertia = 0.05f;

    protected boolean smoothMotion = false;
    protected boolean trailingEnabled = true;
    protected boolean veryCloseRotation = true; // Allows vertical rotation when very close to target
    protected boolean dragToRotate = true; // Requires mouse button hold to rotate
    protected boolean hideCursorOnRotate = true; // Hides cursor when rotating
    protected boolean invertYaxis = false; // Inverts vertical mouse axis for rotation
    protected boolean invertXaxis = false; // Inverts horizontal mouse axis for rotation

    // --- Internal State for Smooth Motion and Input Handling ---
    protected float targetRotation = rotation;
    protected float targetVRotation = vRotation;
    protected float targetDistance = distance;

    protected float rotationLerpFactor = 0;
    protected float trailingLerpFactor = 0;
    protected float vRotationLerpFactor = 0;
    protected float distanceLerpFactor = 0;

    protected boolean rotating = false;
    protected boolean vRotating = false;
    protected boolean zooming = false;
    protected boolean trailing = false;
    protected boolean chasing = false;
    protected boolean targetMoves = false;
    protected boolean enabled = true;
    protected boolean canRotate = false; // Flag for active rotation (when dragToRotate is true)
    protected boolean zoomin = false; // Tracks zoom direction

    // --- Temporary Vectors for Calculations (to avoid object creation in loops) ---
    protected final Vector3f targetDir = new Vector3f();
    protected final Vector3f pos = new Vector3f();
    protected final Vector3f temp = new Vector3f();
    protected Vector3f prevPos; // Previous position of the target
    protected float previousTargetRotation; // Used for trailing logic

    protected Vector3f initialUpVec; // Initial camera up vector, used for lookAt
    protected Vector3f lookAtOffset = new Vector3f(0, 0, 0); // Offset from target's world translation for lookAt point
    protected Vector3f targetLocation = new Vector3f(0, 0, 0);

    protected float offsetDistance = 0.002f; // Distance threshold to detect target movement

    /**
     * @deprecated use {@link CameraInput#CHASECAM_DOWN}
     */
    @Deprecated
    public static final String ChaseCamDown = "ChaseCamDown";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_UP}
     */
    @Deprecated
    public static final String ChaseCamUp = "ChaseCamUp";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_ZOOMIN}
     */
    @Deprecated
    public static final String ChaseCamZoomIn = "ChaseCamZoomIn";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_ZOOMOUT}
     */
    @Deprecated
    public static final String ChaseCamZoomOut = "ChaseCamZoomOut";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_MOVELEFT}
     */
    @Deprecated
    public static final String ChaseCamMoveLeft = "ChaseCamMoveLeft";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_MOVERIGHT}
     */
    @Deprecated
    public static final String ChaseCamMoveRight = "ChaseCamMoveRight";
    /**
     * @deprecated use {@link CameraInput#CHASECAM_TOGGLEROTATE}
     */
    @Deprecated
    public static final String ChaseCamToggleRotate = "ChaseCamToggleRotate";

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

    @Override
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

    @Override
    public void onAnalog(String name, float value, float tpf) {
        if (!enabled) {
            return;
        }

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
            if (!zoomin) {
                distanceLerpFactor = 0;
            }
            zoomin = true;
        } else if (name.equals(CameraInput.CHASECAM_ZOOMOUT)) {
            zoomCamera(+value);
            if (zoomin) {
                distanceLerpFactor = 0;
            }
            zoomin = false;
        }
    }

    /**
     * Registers default input mappings for the chase camera
     * with the provided {@link InputManager}.
     *
     * @param inputManager The {@link InputManager} instance to register inputs with.
     */
    public final void registerWithInput(InputManager inputManager) {
        String[] inputs = {
                CameraInput.CHASECAM_TOGGLEROTATE,
                CameraInput.CHASECAM_DOWN,
                CameraInput.CHASECAM_UP,
                CameraInput.CHASECAM_MOVELEFT,
                CameraInput.CHASECAM_MOVERIGHT,
                CameraInput.CHASECAM_ZOOMIN,
                CameraInput.CHASECAM_ZOOMOUT
        };

        this.inputManager = inputManager;

        // Vertical rotation (Y-axis)
        addAxisMapping(CameraInput.CHASECAM_DOWN, MouseInput.AXIS_Y, true, invertYaxis);
        addAxisMapping(CameraInput.CHASECAM_UP, MouseInput.AXIS_Y, false, invertYaxis);

        // Horizontal rotation (X-axis)
        addAxisMapping(CameraInput.CHASECAM_MOVELEFT, MouseInput.AXIS_X, true, invertXaxis);
        addAxisMapping(CameraInput.CHASECAM_MOVERIGHT, MouseInput.AXIS_X, false, invertXaxis);

        // Zoom (Mouse Wheel)
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMIN,
                new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMOUT,
                new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));

        // Toggle rotation (Mouse Buttons)
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE,
                new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE,
                new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));

        inputManager.addListener(this, inputs);
    }

    /**
     * Cleans up all input mappings and listeners associated with this
     * chase camera from the provided {@link InputManager}. This method
     * effectively undoes the work of {@link #registerWithInput(InputManager)}.
     *
     * @param inputManager The {@link InputManager} to clean up.
     */
    public void cleanupWithInput(InputManager inputManager) {
        if (inputManager != null) {
            inputManager.deleteMapping(CameraInput.CHASECAM_TOGGLEROTATE);
            inputManager.deleteMapping(CameraInput.CHASECAM_DOWN);
            inputManager.deleteMapping(CameraInput.CHASECAM_UP);
            inputManager.deleteMapping(CameraInput.CHASECAM_MOVELEFT);
            inputManager.deleteMapping(CameraInput.CHASECAM_MOVERIGHT);
            inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMIN);
            inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMOUT);
            inputManager.removeListener(this);
            this.inputManager = null; // Clear reference
        }
    }

    /**
     * Sets custom {@link Trigger}s for toggling the camera's rotation.
     * By default, this is mapped to left and right mouse buttons.
     *
     * @param triggers An array of {@link Trigger}s to assign for rotation toggling.
     */
    public void setToggleRotationTrigger(Trigger... triggers) {
        inputManager.deleteMapping(CameraInput.CHASECAM_TOGGLEROTATE);
        inputManager.addMapping(CameraInput.CHASECAM_TOGGLEROTATE, triggers);
        inputManager.addListener(this, CameraInput.CHASECAM_TOGGLEROTATE);
    }

    /**
     * Sets custom {@link Trigger}s for zooming the camera in.
     * By default, this is mapped to the mouse wheel up.
     *
     * @param triggers An array of {@link Trigger}s to assign for zooming in.
     */
    public void setZoomInTrigger(Trigger... triggers) {
        inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMIN);
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMIN, triggers);
        inputManager.addListener(this, CameraInput.CHASECAM_ZOOMIN);
    }

    /**
     * Sets custom {@link Trigger}s for zooming the camera out.
     * By default, this is mapped to the mouse wheel down.
     *
     * @param triggers An array of {@link Trigger}s to assign for zooming out.
     */
    public void setZoomOutTrigger(Trigger... triggers) {
        inputManager.deleteMapping(CameraInput.CHASECAM_ZOOMOUT);
        inputManager.addMapping(CameraInput.CHASECAM_ZOOMOUT, triggers);
        inputManager.addListener(this, CameraInput.CHASECAM_ZOOMOUT);
    }

    /**
     * Computes the camera's position based on its current distance,
     * horizontal rotation, and vertical rotation relative to the target.
     * The result is stored in the internal {@code pos} vector.
     */
    protected void computePosition() {
        // Calculate horizontal distance from target
        float hDistance = (distance) * FastMath.sin((FastMath.PI / 2) - vRotation);
        // Calculate camera position in spherical coordinates relative to target
        pos.set(hDistance * FastMath.cos(rotation),
                distance * FastMath.sin(vRotation),
                hDistance * FastMath.sin(rotation));
        // Add target's world translation to get absolute camera position
        pos.addLocal(target.getWorldTranslation());
    }

    // Rotates the camera horizontally around the target.
    protected void rotateCamera(float value) {
        if (!canRotate || !enabled) {
            return;
        }
        rotating = true;
        targetRotation += value * rotationSpeed;
    }

    // Zooms the camera in or out by adjusting its distance from the target.
    protected void zoomCamera(float value) {
        if (!enabled) {
            return;
        }

        zooming = true;
        targetDistance += value * zoomSensitivity;
        targetDistance = FastMath.clamp(targetDistance, minDistance, maxDistance);

        // Adjust vertical rotation if very close and moving away
        if (veryCloseRotation) {
            if ((targetVRotation < minVerticalRotation) && (targetDistance > (minDistance + 1.0f))) {
                targetVRotation = minVerticalRotation;
            }
        }
    }

    // Rotates the camera vertically around the target.
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
                // Prevent camera from flipping upside down
                targetVRotation = lastGoodRot;
            }
        } else {
            if (targetVRotation < minVerticalRotation) {
                targetVRotation = lastGoodRot;
            }
        }
    }

    /**
     * Updates the camera, should only be called internally
     *
     * @param tpf time per frame (in seconds)
     */
    protected void updateCamera(float tpf) {
        if (enabled) {

            // Update target's current world location, applying lookAtOffset
            targetLocation.set(target.getWorldTranslation()).addLocal(lookAtOffset);
            if (smoothMotion) {

                // --- Target Movement Detection and Chasing/Trailing Logic ---
                targetDir.set(targetLocation).subtractLocal(prevPos);
                float dist = targetDir.length();

                // Detect if target is moving significantly
                if (offsetDistance < dist) {
                    // Target moves, start chasing.
                    chasing = true;
                    // Target moves, start trailing if it has to.
                    if (trailingEnabled) {
                        trailing = true;
                    }
                    // Target is currently moving.
                    targetMoves = true;
                } else {
                    //if target was moving, we compute a slight offset in rotation to avoid a rough stop of the cam
                    //We do not if the player is rotating the cam
                    if (targetMoves && !canRotate) {
                        if (targetRotation - rotation > trailingRotationInertia) {
                            targetRotation = rotation + trailingRotationInertia;
                        } else if (targetRotation - rotation < -trailingRotationInertia) {
                            targetRotation = rotation - trailingRotationInertia;
                        }
                    }
                    // Target has stopped.
                    targetMoves = false;
                }

                // If user is actively rotating, disable trailing and reset its lerp factor
                if (canRotate) {
                    trailingLerpFactor = 0;
                    trailing = false;
                }

                // --- Trailing Motion Interpolation ---
                if (trailingEnabled && trailing) {
                    if (targetMoves) {
                        // Compute the reversed direction of the target for trailing
                        Vector3f a = targetDir.negate().normalizeLocal();
                        Vector3f b = Vector3f.UNIT_X; // Reference vector (X-axis)
                        a.y = 0; // Project to 2D plane for horizontal angle

                        // Compute the angle between X-axis and the trail direction
                        if (targetDir.z > 0) {
                            targetRotation = FastMath.TWO_PI - FastMath.acos(a.dot(b));
                        } else {
                            targetRotation = FastMath.acos(a.dot(b));
                        }
                        if (targetRotation - rotation > FastMath.PI || targetRotation - rotation < -FastMath.PI) {
                            targetRotation -= FastMath.TWO_PI;
                        }

                        // If there's a significant change in direction while trailing, reset lerp factor
                        if (targetRotation != previousTargetRotation && FastMath.abs(targetRotation - previousTargetRotation) > FastMath.PI / 8) {
                            trailingLerpFactor = 0;
                        }
                        previousTargetRotation = targetRotation;
                    }

                    // Interpolate rotation towards the target trailing angle
                    trailingLerpFactor = Math.min(trailingLerpFactor + tpf * tpf * trailingSensitivity, 1);
                    rotation = FastMath.interpolateLinear(trailingLerpFactor, rotation, targetRotation);

                    // If the rotation is near the target rotation, we're good, that's over.
                    if (targetRotation + 0.01f >= rotation && targetRotation - 0.01f <= rotation) {
                        trailing = false;
                        trailingLerpFactor = 0;
                    }
                }

                // --- Chasing (Distance) Interpolation ---
                if (chasing) {
                    // Calculate current distance from target to camera
                    distance = temp.set(targetLocation).subtractLocal(cam.getLocation()).length();
                    distanceLerpFactor = Math.min(distanceLerpFactor + (tpf * tpf * chasingSensitivity * 0.05f), 1);
                    distance = FastMath.interpolateLinear(distanceLerpFactor, distance, targetDistance);

                    // If distance is very close to target, stop chasing
                    if (targetDistance + 0.01f >= distance && targetDistance - 0.01f <= distance) {
                        distanceLerpFactor = 0;
                        chasing = false;
                    }
                }

                // --- Zooming Interpolation (User initiated) ---
                if (zooming) {
                    distanceLerpFactor = Math.min(distanceLerpFactor + (tpf * tpf * zoomSensitivity), 1);
                    distance = FastMath.interpolateLinear(distanceLerpFactor, distance, targetDistance);

                    // If distance is very close to target, stop zooming
                    if (targetDistance + 0.1f >= distance && targetDistance - 0.1f <= distance) {
                        zooming = false;
                        distanceLerpFactor = 0;
                    }
                }

                // --- Horizontal Rotation Interpolation (User initiated) ---
                if (rotating) {
                    rotationLerpFactor = Math.min(rotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                    rotation = FastMath.interpolateLinear(rotationLerpFactor, rotation, targetRotation);

                    // If rotation is very close to target, stop rotating
                    if (targetRotation + 0.01f >= rotation && targetRotation - 0.01f <= rotation) {
                        rotating = false;
                        rotationLerpFactor = 0;
                    }
                }

                // --- Vertical Rotation Interpolation (User initiated) ---
                if (vRotating) {
                    vRotationLerpFactor = Math.min(vRotationLerpFactor + tpf * tpf * rotationSensitivity, 1);
                    vRotation = FastMath.interpolateLinear(vRotationLerpFactor, vRotation, targetVRotation);

                    // If vertical rotation is very close to target, stop vRotating
                    if (targetVRotation + 0.01f >= vRotation && targetVRotation - 0.01f <= vRotation) {
                        vRotating = false;
                        vRotationLerpFactor = 0;
                    }
                }

                // --- Final Camera Position and Orientation Update ---
                computePosition();
                cam.setLocation(pos.addLocal(lookAtOffset));
            } else {
                // No smooth motion: directly set camera state to target values
                vRotation = targetVRotation;
                rotation = targetRotation;
                distance = targetDistance;
                computePosition();
                cam.setLocation(pos.addLocal(lookAtOffset));
            }
            // Keep track of the target's previous position for next frame's movement detection
            prevPos.set(targetLocation);

            // Make the camera look at the target's location (with offset)
            cam.lookAt(targetLocation, initialUpVec);
        }
    }

    /**
     * Returns the enabled/disabled state of the camera.
     * When disabled, the camera will not update its position or respond to input.
     *
     * @return True if the camera is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enables or disables the camera. When disabled, the camera will not update
     * its position or respond to input.
     *
     * @param enabled True to enable the camera, false to disable.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            // On enable, set canRotate based on dragToRotate setting
            this.canRotate = !dragToRotate;
        } else {
            // If disabling, ensure rotation is stopped and cursor is visible
            canRotate = false;
        }
    }

    /**
     * Returns the maximum zoom distance of the camera from its target.
     *
     * @return The configured maximum distance in world units (default is 40.0f).
     */
    public float getMaxDistance() {
        return maxDistance;
    }

    /**
     * Sets the maximum zoom distance of the camera from its target.
     * If the current distance is greater than the new max distance, the camera
     * will zoom in to the new max distance.
     *
     * @param maxDistance The desired maximum distance in world units (default is 40.0f).
     */
    public void setMaxDistance(float maxDistance) {
        this.maxDistance = maxDistance;
        if (maxDistance < distance) {
            zoomCamera(maxDistance - distance);
        }
    }

    /**
     * Returns the minimum zoom distance of the camera from its target.
     *
     * @return The configured minimum distance in world units (default is 1.0f).
     */
    public float getMinDistance() {
        return minDistance;
    }

    /**
     * Sets the minimum zoom distance of the camera from its target.
     * If the current distance is less than the new min distance, the camera
     * will zoom out to the new min distance.
     *
     * @param minDistance The desired minimum distance in world units (default is 1.0f).
     */
    public void setMinDistance(float minDistance) {
        this.minDistance = minDistance;
        if (minDistance > distance) {
            zoomCamera(distance - minDistance);
        }
    }

    /**
     * clone this camera for a spatial
     *
     * @param spatial ignored
     * @return never
     */
    @Deprecated
    @Override
    public Control cloneForSpatial(Spatial spatial) {
        throw new UnsupportedOperationException();
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
    public void cloneFields(Cloner cloner, Object original) {
        this.target = cloner.clone(target);
        computePosition();
        prevPos = new Vector3f(target.getWorldTranslation());
        cam.setLocation(pos);
    }

    /**
     * Sets the spatial for the camera control, should only be used internally
     *
     * @param spatial the desired camera target, or null for none
     */
    @Override
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
     *
     * @param tpf time per frame (in seconds)
     */
    @Override
    public void update(float tpf) {
        updateCamera(tpf);
    }

    /**
     * renders the camera control, should only be used internally
     *
     * @param rm ignored
     * @param vp ignored
     */
    @Override
    public void render(RenderManager rm, ViewPort vp) {
        // nothing to render
    }

    /**
     * Write the camera
     * @param ex the exporter
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        throw new UnsupportedOperationException("ChaseCamera serialization not supported");
    }

    /**
     * Read the camera
     *
     * @param im the importer (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        throw new UnsupportedOperationException("ChaseCamera deserialization not supported");
    }

    /**
     * Returns the maximal vertical rotation angle in radians of the camera around the target.
     *
     * @return The maximal vertical rotation angle (in radians).
     */
    public float getMaxVerticalRotation() {
        return maxVerticalRotation;
    }

    /**
     * Sets the maximal vertical rotation angle in radians of the camera around the target.
     * The default value is {@code FastMath.PI / 2} (90 degrees).
     *
     * @param maxVerticalRotation The desired angle in radians.
     */
    public void setMaxVerticalRotation(float maxVerticalRotation) {
        this.maxVerticalRotation = maxVerticalRotation;
    }

    /**
     * Returns the minimal vertical rotation angle in radians of the camera around the target.
     *
     * @return The minimal vertical rotation angle (in radians).
     */
    public float getMinVerticalRotation() {
        return minVerticalRotation;
    }

    /**
     * Sets the minimal vertical rotation angle in radians of the camera around the target.
     * The default value is {@code 0.00f}.
     *
     * @param minVerticalRotation The desired angle in radians.
     */
    public void setMinVerticalRotation(float minVerticalRotation) {
        this.minVerticalRotation = minVerticalRotation;
    }

    /**
     * Checks if smooth motion is enabled for this chase camera.
     *
     * @return True if smooth motion is enabled, false otherwise.
     */
    public boolean isSmoothMotion() {
        return smoothMotion;
    }

    /**
     *  Enables or disables smooth motion for this chase camera.
     *
     * @param smoothMotion True to enable, false to disable (default is false).
     */
    public void setSmoothMotion(boolean smoothMotion) {
        this.smoothMotion = smoothMotion;
    }

    /**
     * Returns the chasing sensitivity.
     * This value affects how quickly the camera follows the target when it moves,
     * assuming smooth motion is enabled.
     *
     * @return The chasing sensitivity value.
     */
    public float getChasingSensitivity() {
        return chasingSensitivity;
    }

    /**
     * Sets the chasing sensitivity. The lower the value, the slower the camera
     * will follow the target when it moves.
     * This setting only has an effect if {@link #isSmoothMotion()} is true.
     * Default is 5.0f.
     *
     * @param chasingSensitivity The desired sensitivity value (default is 5.0f).
     */
    public void setChasingSensitivity(float chasingSensitivity) {
        this.chasingSensitivity = chasingSensitivity;
    }

    /**
     * Returns the rotation sensitivity.
     * This value affects how quickly the camera rotates around the target
     * when dragging with the mouse, assuming smooth motion is enabled.
     *
     * @return The rotation sensitivity value.
     */
    public float getRotationSensitivity() {
        return rotationSensitivity;
    }

    /**
     * Sets the rotation sensitivity. The lower the value, the slower the camera
     * will rotate around the target when dragging with the mouse.
     * Values over 5.0f typically have little additional effect.
     * This setting only has an effect if {@link #isSmoothMotion()} is true.
     * Default is 5.0f.
     *
     * @param rotationSensitivity The desired sensitivity value (default is 5.0f).
     */
    public void setRotationSensitivity(float rotationSensitivity) {
        this.rotationSensitivity = rotationSensitivity;
    }

    /**
     * Checks if camera trailing is enabled.
     * When enabled (and smooth motion is also enabled), the camera will smoothly
     * adjust its horizontal rotation to follow the target's movement direction.
     *
     * @return True if trailing is enabled, false otherwise.
     */
    public boolean isTrailingEnabled() {
        return trailingEnabled;
    }

    /**
     * Enables or disables camera trailing.
     * When enabled (and smooth motion is also enabled), the camera will smoothly
     * adjust its horizontal rotation to follow the target's movement direction.
     *
     * @param trailingEnabled True to enable trailing, false to disable (default is true).
     */
    public void setTrailingEnabled(boolean trailingEnabled) {
        this.trailingEnabled = trailingEnabled;
    }

    /**
     * Returns the trailing rotation inertia.
     * This value influences how abruptly the camera stops rotating to trail
     * the target when the target stops moving. A higher value means the camera
     * will stop more roughly before reaching the exact trail position.
     *
     * @return The trailing rotation inertia value.
     */
    public float getTrailingRotationInertia() {
        return trailingRotationInertia;
    }

    /**
     * Sets the trailing rotation inertia. Default is 0.05f.
     * This causes the camera to stop roughly when the target stops moving
     * before the camera reaches the trail position.
     * This setting only has an effect if {@link #isSmoothMotion()} is true and
     * {@link #isTrailingEnabled()} is true.
     *
     * @param trailingRotationInertia The desired inertia value (default is 0.05f).
     */
    public void setTrailingRotationInertia(float trailingRotationInertia) {
        this.trailingRotationInertia = trailingRotationInertia;
    }

    /**
     * Returns the trailing sensitivity.
     * This value affects how quickly the camera adjusts its horizontal rotation
     * to follow the target's movement direction when trailing is enabled.
     *
     * @return The trailing sensitivity value.
     */
    public float getTrailingSensitivity() {
        return trailingSensitivity;
    }

    /**
     * Sets the trailing sensitivity. The lower the value, the slower the camera
     * will go in the target's trail when it moves.
     * This setting only has an effect if {@link #isSmoothMotion()} is true and
     * {@link #isTrailingEnabled()} is true.
     *
     * @param trailingSensitivity The desired sensitivity value (default is 0.5f).
     */
    public void setTrailingSensitivity(float trailingSensitivity) {
        this.trailingSensitivity = trailingSensitivity;
    }

    /**
     * Returns the zoom sensitivity.
     * This value affects how quickly the camera zooms in and out in response
     * to zoom input.
     *
     * @return The zoom sensitivity value.
     */
    public float getZoomSensitivity() {
        return zoomSensitivity;
    }

    /**
     * Sets the zoom sensitivity. The lower the value, the slower the camera
     * will zoom in and out.
     *
     * @param zoomSensitivity The desired sensitivity value (default is 2.0f).
     */
    public void setZoomSensitivity(float zoomSensitivity) {
        this.zoomSensitivity = zoomSensitivity;
    }

    /**
     * Returns the rotation speed when the mouse is moved.
     * This value scales the input delta for horizontal and vertical rotation.
     *
     * @return The rotation speed.
     */
    public float getRotationSpeed() {
        return rotationSpeed;
    }

    /**
     * Sets the rotation speed when the mouse is moved. The lower the value,
     * the slower the camera will rotate.
     *
     * @param rotationSpeed The desired rotation speed (default is 1.0f).
     */
    public void setRotationSpeed(float rotationSpeed) {
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * Sets the default distance of the camera from the target at the start of
     * the application or when the camera is initialized.
     *
     * @param defaultDistance The desired default distance in world units (default is 20.0f).
     */
    public void setDefaultDistance(float defaultDistance) {
        distance = defaultDistance;
        targetDistance = distance;
    }

    /**
     * Sets the default horizontal rotation angle (in radians) of the camera
     * at the start of the application or when the camera is initialized.
     *
     * @param angleInRad The desired angle in radians (default is 0.0f).
     */
    public void setDefaultHorizontalRotation(float angleInRad) {
        rotation = angleInRad;
        targetRotation = angleInRad;
    }

    /**
     * Sets the default vertical rotation angle (in radians) of the camera
     * at the start of the application or when the camera is initialized.
     *
     * @param angleInRad The desired angle in radians (default is {@code FastMath.PI / 6}).
     */
    public void setDefaultVerticalRotation(float angleInRad) {
        vRotation = angleInRad;
        targetVRotation = angleInRad;
    }

    /**
     * Checks if the "drag to rotate" feature is enabled.
     *
     * @return True if drag to rotate is enabled, false otherwise.
     * @see #setDragToRotate(boolean)
     */
    public boolean isDragToRotate() {
        return dragToRotate;
    }

    /**
     * Sets whether the "drag to rotate" feature is enabled.
     * <ul>
     * <li>If {@code true}, the user must hold a mouse button (default left/right click)
     * and drag to rotate the camera. The cursor remains visible until dragging,
     * and is hidden while dragging.</li>
     * <li>If {@code false}, the cursor is always invisible, and holding a mouse button
     * is not required for rotation; mouse movement directly rotates the camera.</li>
     * </ul>
     *
     * @param dragToRotate True to enable drag to rotate, false to disable.
     */
    public void setDragToRotate(boolean dragToRotate) {
        this.dragToRotate = dragToRotate;
        this.canRotate = !dragToRotate;
        if (inputManager != null) {
            inputManager.setCursorVisible(dragToRotate); // Show cursor if dragToRotate is true
        }
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
     * Returns the current distance of the camera from its target.
     * This is the interpolated distance if smooth motion is enabled.
     *
     * @return The current distance in world units.
     */
    public float getDistanceToTarget() {
        return distance;
    }

    /**
     * Returns the current horizontal rotation angle of the camera around the target.
     * This is the interpolated angle if smooth motion is enabled.
     *
     * @return The current horizontal rotation angle in radians.
     */
    public float getHorizontalRotation() {
        return rotation;
    }

    /**
     * Returns the current vertical rotation angle of the camera around the target.
     * This is the interpolated angle if smooth motion is enabled.
     *
     * @return The current vertical rotation angle in radians.
     */
    public float getVerticalRotation() {
        return vRotation;
    }

    /**
     * Returns the offset vector applied to the target's world translation
     * to determine the camera's look-at point.
     *
     * @return The look-at offset {@link Vector3f}.
     */
    public Vector3f getLookAtOffset() {
        return lookAtOffset;
    }

    /**
     * Sets the offset from the target's position where the camera looks at
     *
     * @param lookAtOffset the desired offset (alias created)
     */
    public void setLookAtOffset(Vector3f lookAtOffset) {
        this.lookAtOffset = lookAtOffset;
    }

    /**
     * Sets the up vector of the camera used for the lookAt on the target
     *
     * @param up the desired direction (alias created)
     */
    public void setUpVector(Vector3f up) {
        initialUpVec = up;
    }

    /**
     * Returns the up vector of the camera used for the lookAt on the target
     * @return the pre-existing vector
     */
    public Vector3f getUpVector() {
        return initialUpVec;
    }

    /**
     * Checks if the cursor should be hidden when the camera is actively rotating
     * (i.e., when drag-to-rotate is enabled and the rotation button is held).
     *
     * @return True if the cursor is hidden during rotation, false otherwise.
     */
    public boolean isHideCursorOnRotate() {
        return hideCursorOnRotate;
    }

    /**
     * Sets whether the cursor should be hidden when the camera is actively rotating.
     * This is only relevant if {@link #isDragToRotate()} is true.
     *
     * @param hideCursorOnRotate True to hide the cursor, false to keep it visible.
     */
    public void setHideCursorOnRotate(boolean hideCursorOnRotate) {
        this.hideCursorOnRotate = hideCursorOnRotate;
    }

    /**
     * invert the vertical axis movement of the mouse
     *
     * @param invertYaxis true&rarr;invert, false&rarr;don't invert
     */
    public void setInvertVerticalAxis(boolean invertYaxis) {
        this.invertYaxis = invertYaxis;
        inputManager.deleteMapping(CameraInput.CHASECAM_DOWN);
        inputManager.deleteMapping(CameraInput.CHASECAM_UP);
        addAxisMapping(CameraInput.CHASECAM_DOWN, MouseInput.AXIS_Y, true, invertYaxis);
        addAxisMapping(CameraInput.CHASECAM_UP, MouseInput.AXIS_Y, false, invertYaxis);
        inputManager.addListener(this, CameraInput.CHASECAM_DOWN, CameraInput.CHASECAM_UP);
    }

    /**
     * invert the Horizontal axis movement of the mouse
     *
     * @param invertXaxis true&rarr;invert, false&rarr;don't invert
     */
    public void setInvertHorizontalAxis(boolean invertXaxis) {
        this.invertXaxis = invertXaxis;
        inputManager.deleteMapping(CameraInput.CHASECAM_MOVELEFT);
        inputManager.deleteMapping(CameraInput.CHASECAM_MOVERIGHT);
        addAxisMapping(CameraInput.CHASECAM_MOVELEFT, MouseInput.AXIS_X, true, invertXaxis);
        addAxisMapping(CameraInput.CHASECAM_MOVERIGHT, MouseInput.AXIS_X, false, invertXaxis);
        inputManager.addListener(this, CameraInput.CHASECAM_MOVELEFT, CameraInput.CHASECAM_MOVERIGHT);
    }

    /**
     * Helper method to add an axis mapping with optional inversion.
     *
     * @param mappingName The name of the input mapping.
     * @param axis        The mouse axis to trigger on.
     * @param positive    True for positive axis movement, false for negative.
     * @param invertAxis  True to invert the mapping (positive becomes negative, negative becomes positive).
     */
    private void addAxisMapping(String mappingName, int axis, boolean positive, boolean invertAxis) {
        if (invertAxis) {
            inputManager.addMapping(mappingName, new MouseAxisTrigger(axis, !positive));
        } else {
            inputManager.addMapping(mappingName, new MouseAxisTrigger(axis, positive));
        }
    }
}
