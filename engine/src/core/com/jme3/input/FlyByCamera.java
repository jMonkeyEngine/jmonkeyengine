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

import com.jme3.collision.MotionAllowedListener;
import com.jme3.input.controls.*;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

/**
 * A first person view camera controller.
 * After creation, you must register the camera controller with the
 * dispatcher using #registerWithDispatcher().
 *
 * Controls:
 *  - Move the mouse to rotate the camera
 *  - Mouse wheel for zooming in or out
 *  - WASD keys for moving forward/backward and strafing
 *  - QZ keys raise or lower the camera
 */
public class FlyByCamera implements AnalogListener, ActionListener {

    private static String[] mappings = new String[]{
            "FLYCAM_Left",
            "FLYCAM_Right",
            "FLYCAM_Up",
            "FLYCAM_Down",

            "FLYCAM_StrafeLeft",
            "FLYCAM_StrafeRight",
            "FLYCAM_Forward",
            "FLYCAM_Backward",

            "FLYCAM_ZoomIn",
            "FLYCAM_ZoomOut",
            "FLYCAM_RotateDrag",

            "FLYCAM_Rise",
            "FLYCAM_Lower"
        };

    protected Camera cam;
    protected Vector3f initialUpVec;
    protected float rotationSpeed = 1f;
    protected float moveSpeed = 3f;
    protected MotionAllowedListener motionAllowed = null;
    protected boolean enabled = true;
    protected boolean dragToRotate = false;
    protected boolean canRotate = false;
    protected InputManager inputManager;
    
    /**
     * Creates a new FlyByCamera to control the given Camera object.
     * @param cam
     */
    public FlyByCamera(Camera cam){
        this.cam = cam;
        initialUpVec = cam.getUp().clone();
    }

    /**
     * Sets the up vector that should be used for the camera.
     * @param upVec
     */
    public void setUpVector(Vector3f upVec) {
       initialUpVec.set(upVec);
    }

    public void setMotionAllowedListener(MotionAllowedListener listener){
        this.motionAllowed = listener;
    }

    /**
     * Sets the move speed. The speed is given in world units per second.
     * @param moveSpeed
     */
    public void setMoveSpeed(float moveSpeed){
        this.moveSpeed = moveSpeed;
    }

    /**
     * Sets the rotation speed.
     * @param rotationSpeed
     */
    public void setRotationSpeed(float rotationSpeed){
        this.rotationSpeed = rotationSpeed;
    }

    /**
     * @param enable If false, the camera will ignore input.
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
     * @return If enabled
     * @see FlyByCamera#setEnabled(boolean)
     */
    public boolean isEnabled(){
        return enabled;
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
     * Set if drag to rotate mode is enabled.
     * 
     * When true, the user must hold the mouse button
     * and drag over the screen to rotate the camera, and the cursor is
     * visible until dragged. Otherwise, the cursor is invisible at all times
     * and holding the mouse button is not needed to rotate the camera.
     * This feature is disabled by default.
     * 
     * @param dragToRotate True if drag to rotate mode is enabled.
     */
    public void setDragToRotate(boolean dragToRotate) {
        this.dragToRotate = dragToRotate;
        if (inputManager != null) {
            inputManager.setCursorVisible(dragToRotate);
        }
    }

    /**
     * Registers the FlyByCamera to receive input events from the provided
     * Dispatcher.
     * @param inputManager
     */
    public void registerWithInput(InputManager inputManager){
        this.inputManager = inputManager;
        
        // both mouse and button - rotation of cam
        inputManager.addMapping("FLYCAM_Left", new MouseAxisTrigger(MouseInput.AXIS_X, true),
                                               new KeyTrigger(KeyInput.KEY_LEFT));

        inputManager.addMapping("FLYCAM_Right", new MouseAxisTrigger(MouseInput.AXIS_X, false),
                                                new KeyTrigger(KeyInput.KEY_RIGHT));

        inputManager.addMapping("FLYCAM_Up", new MouseAxisTrigger(MouseInput.AXIS_Y, false),
                                             new KeyTrigger(KeyInput.KEY_UP));

        inputManager.addMapping("FLYCAM_Down", new MouseAxisTrigger(MouseInput.AXIS_Y, true),
                                               new KeyTrigger(KeyInput.KEY_DOWN));

        // mouse only - zoom in/out with wheel, and rotate drag
        inputManager.addMapping("FLYCAM_ZoomIn", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, false));
        inputManager.addMapping("FLYCAM_ZoomOut", new MouseAxisTrigger(MouseInput.AXIS_WHEEL, true));
        inputManager.addMapping("FLYCAM_RotateDrag", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));

        // keyboard only WASD for movement and WZ for rise/lower height
        inputManager.addMapping("FLYCAM_StrafeLeft", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("FLYCAM_StrafeRight", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("FLYCAM_Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("FLYCAM_Backward", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("FLYCAM_Rise", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("FLYCAM_Lower", new KeyTrigger(KeyInput.KEY_Z));

        inputManager.addListener(this, mappings);
        inputManager.setCursorVisible(dragToRotate || !isEnabled());

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null && joysticks.length > 0){
            Joystick joystick = joysticks[0];
            joystick.assignAxis("FLYCAM_StrafeRight", "FLYCAM_StrafeLeft", JoyInput.AXIS_POV_X);
            joystick.assignAxis("FLYCAM_Forward", "FLYCAM_Backward", JoyInput.AXIS_POV_Y);
            joystick.assignAxis("FLYCAM_Right", "FLYCAM_Left", joystick.getXAxisIndex());
            joystick.assignAxis("FLYCAM_Down", "FLYCAM_Up", joystick.getYAxisIndex());
        }
    }

    /**
     * Registers the FlyByCamera to receive input events from the provided
     * Dispatcher.
     * @param inputManager
     */
    public void unregisterInput(){
    
        if (inputManager == null) {
            return;
        }
    
        for (String s : mappings) {
            if (inputManager.hasMapping(s)) {
                inputManager.deleteMapping( s );
            }
        }

        inputManager.removeListener(this);
        inputManager.setCursorVisible(!dragToRotate);

        Joystick[] joysticks = inputManager.getJoysticks();
        if (joysticks != null && joysticks.length > 0){
            Joystick joystick = joysticks[0];
            
            // No way to unassing axis
        }
    }

    protected void rotateCamera(float value, Vector3f axis){
        if (dragToRotate){
            if (canRotate){
//                value = -value;
            }else{
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
        q.normalize();

        cam.setAxes(q);
    }

    protected void zoomCamera(float value){
        // derive fovY value
        float h = cam.getFrustumTop();
        float w = cam.getFrustumRight();
        float aspect = w / h;

        float near = cam.getFrustumNear();

        float fovY = FastMath.atan(h / near)
                  / (FastMath.DEG_TO_RAD * .5f);
        fovY += value * 0.1f;

        h = FastMath.tan( fovY * FastMath.DEG_TO_RAD * .5f) * near;
        w = h * aspect;

        cam.setFrustumTop(h);
        cam.setFrustumBottom(-h);
        cam.setFrustumLeft(-w);
        cam.setFrustumRight(w);
    }

    protected void riseCamera(float value){
        Vector3f vel = new Vector3f(0, value * moveSpeed, 0);
        Vector3f pos = cam.getLocation().clone();

        if (motionAllowed != null)
            motionAllowed.checkMotionAllowed(pos, vel);
        else
            pos.addLocal(vel);

        cam.setLocation(pos);
    }

    protected void moveCamera(float value, boolean sideways){
        Vector3f vel = new Vector3f();
        Vector3f pos = cam.getLocation().clone();

        if (sideways){
            cam.getLeft(vel);
        }else{
            cam.getDirection(vel);
        }
        vel.multLocal(value * moveSpeed);

        if (motionAllowed != null)
            motionAllowed.checkMotionAllowed(pos, vel);
        else
            pos.addLocal(vel);

        cam.setLocation(pos);
    }

    public void onAnalog(String name, float value, float tpf) {
        if (!enabled)
            return;

        if (name.equals("FLYCAM_Left")){
            rotateCamera(value, initialUpVec);
        }else if (name.equals("FLYCAM_Right")){
            rotateCamera(-value, initialUpVec);
        }else if (name.equals("FLYCAM_Up")){
            rotateCamera(-value, cam.getLeft());
        }else if (name.equals("FLYCAM_Down")){
            rotateCamera(value, cam.getLeft());
        }else if (name.equals("FLYCAM_Forward")){
            moveCamera(value, false);
        }else if (name.equals("FLYCAM_Backward")){
            moveCamera(-value, false);
        }else if (name.equals("FLYCAM_StrafeLeft")){
            moveCamera(value, true);
        }else if (name.equals("FLYCAM_StrafeRight")){
            moveCamera(-value, true);
        }else if (name.equals("FLYCAM_Rise")){
            riseCamera(value);
        }else if (name.equals("FLYCAM_Lower")){
            riseCamera(-value);
        }else if (name.equals("FLYCAM_ZoomIn")){
            zoomCamera(value);
        }else if (name.equals("FLYCAM_ZoomOut")){
            zoomCamera(-value);
        }
    }

    public void onAction(String name, boolean value, float tpf) {
        if (!enabled)
            return;

        if (name.equals("FLYCAM_RotateDrag") && dragToRotate){
            canRotate = value;
            inputManager.setCursorVisible(!value);
        }
    }

}
