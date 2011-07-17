/*
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved. <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *  * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. <p/> * Redistributions
 * in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. <p/> * Neither the name of
 * 'jMonkeyEngine' nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written
 * permission. <p/> THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT
 * NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.gde.core.scene.controller;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.sceneviewer.SceneViewerTopComponent;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import java.awt.FocusTraversalPolicy;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.openide.util.Exceptions;

/**
 *
 * @author normenhansen
 */
public abstract class AbstractCameraController extends AbstractAppState implements ActionListener, AnalogListener, RawInputListener {

    protected boolean leftMouse, rightMouse, middleMouse;
    protected float deltaX, deltaY, deltaZ, deltaWheel;
    protected int mouseX = 0;
    protected int mouseY = 0;
    protected Quaternion rot = new Quaternion();
    protected Vector3f vector = new Vector3f(0, 0, 5);
    protected Vector3f focus = new Vector3f();
    protected Camera cam;
    protected InputManager inputManager;
    protected Object master;
    protected boolean moved = false;
    protected boolean movedR = false;
    protected boolean buttonDownL = false;
    protected boolean buttonDownR = false;
    protected boolean buttonDownM = false;
    protected boolean checkClickL = false;
    protected boolean checkClickR = false;
    protected boolean checkClickM = false;
    protected boolean checkReleaseL = false;
    protected boolean checkReleaseR = false;
    protected boolean checkReleaseM = false;
    protected boolean checkDragged = false;
    protected boolean checkDraggedR = false;
    protected boolean checkReleaseLeft = false;
    protected boolean checkReleaseRight = false;

    public AbstractCameraController(Camera cam, InputManager inputManager) {
        this.cam = cam;
        this.inputManager = inputManager;
    }

    public void setMaster(Object component) {
        this.master = component;
    }

    public void enable() {
        inputManager.addRawInputListener(this);
        inputManager.addListener(this, "MouseAxisX", "MouseAxisY", "MouseAxisX-", "MouseAxisY-", "MouseWheel", "MouseWheel-", "MouseButtonLeft", "MouseButtonMiddle", "MouseButtonRight");
        SceneApplication.getApplication().getStateManager().attach(this);
    }

    public void disable() {
        inputManager.removeRawInputListener(this);
        inputManager.removeListener(this);
        SceneApplication.getApplication().getStateManager().detach(this);
    }

    public void setCamFocus(final Vector3f focus) {
        SceneApplication.getApplication().enqueue(new Callable<Object>() {

            public Object call() throws Exception {
                doSetCamFocus(focus);
                return null;
            }
        });

    }

    public void doSetCamFocus(Vector3f focus) {
        cam.setLocation(cam.getLocation().add(focus.subtract(this.focus)));
        this.focus.set(focus);
    }

    /*
     * methods to move camera
     */
    protected void rotateCamera(Vector3f axis, float amount) {
        if (axis.equals(cam.getLeft())) {
            float elevation = -FastMath.asin(cam.getDirection().y);
            amount = Math.min(Math.max(elevation + amount,
                    -FastMath.HALF_PI), FastMath.HALF_PI)
                    - elevation;
        }
        rot.fromAngleAxis(amount, axis);
        cam.getLocation().subtract(focus, vector);
        rot.mult(vector, vector);
        focus.add(vector, cam.getLocation());

        Quaternion curRot = cam.getRotation().clone();
        cam.setRotation(rot.mult(curRot));
    }

    protected void panCamera(float left, float up) {
        cam.getLeft().mult(left, vector);
        vector.scaleAdd(up, cam.getUp(), vector);
        vector.multLocal(cam.getLocation().distance(focus));
        cam.setLocation(cam.getLocation().add(vector));
        focus.addLocal(vector);
    }

    protected void moveCamera(float forward) {
        cam.getDirection().mult(forward, vector);
        cam.setLocation(cam.getLocation().add(vector));
    }

    protected void zoomCamera(float amount) {
        amount = cam.getLocation().distance(focus) * amount;
        float dist = cam.getLocation().distance(focus);
        amount = dist - Math.max(0f, dist - amount);
        Vector3f loc = cam.getLocation().clone();
        loc.scaleAdd(amount, cam.getDirection(), loc);
        cam.setLocation(loc);

        if (cam.isParallelProjection()) {
            float aspect = (float) cam.getWidth() / cam.getHeight();
            float h = FastMath.tan(45f * FastMath.DEG_TO_RAD * .5f) * dist;
            float w = h * aspect;
            cam.setFrustum(-1000, 1000, -w, w, h, -h);
        }

    }

    public boolean toggleOrthoPerspMode() {
        try {
            return SceneApplication.getApplication().enqueue(new Callable<Boolean>() {

                public Boolean call() throws Exception {
                    return doToggleOrthoPerspMode();
                }
            }).get();
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ExecutionException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;

    }

    protected boolean doToggleOrthoPerspMode() {

        float aspect = (float) cam.getWidth() / cam.getHeight();
        if (!cam.isParallelProjection()) {
            cam.setParallelProjection(true);
            float h = cam.getFrustumTop();
            float w = cam.getFrustumRight();
            float dist = cam.getLocation().distance(focus);
            float fovY = FastMath.atan(h) / (FastMath.DEG_TO_RAD * .5f);
            h = FastMath.tan(fovY * FastMath.DEG_TO_RAD * .5f) * dist;
            w = h * aspect;
            cam.setFrustum(-1000, 1000, -w, w, h, -h);
            return true;
        } else {
            cam.setParallelProjection(false);
            cam.setFrustumPerspective(45f, aspect, 1, 1000);
            return false;
        }




    }

    /*public void onAction(String string, boolean bln, float f) {
        if ("MouseButtonLeft".equals(string)) {
            if (bln) {
                leftMouse = true;
                moved = false;
            } else {
                if (leftMouse)
                    checkReleaseLeft = true;
                leftMouse = false;
                if (!moved) {
                    if (checkClick == false)
                        checkClick = true;
                    checkDragged = false;
                } else {
                    checkDragged = true;
                    checkClick = false;
                }
            }
        }
        if ("MouseButtonRight".equals(string)) {
            if (bln) {
                rightMouse = true;
                movedR = false;
            } else {
                if (rightMouse)
                    checkReleaseRight = true;
                rightMouse = false;
                if (!movedR) {
                    if (checkClickR == false)
                        checkClickR = true;
                    checkDraggedR = false;
                } else {
                    checkDraggedR = true;
                    checkClickR = false;
                }
            }
        }
    }*/
    public void onAnalog(String string, float f1, float f) {
        if ("MouseAxisX".equals(string)) {
            moved = true;
            movedR = true;
            if (buttonDownL || buttonDownM) {
                rotateCamera(Vector3f.UNIT_Y, -f1 * 2.5f);
            }
            if (buttonDownR) {
                panCamera(f1 * 2.5f, 0);
            }
        } else if ("MouseAxisY".equals(string)) {
            moved = true;
            movedR = true;
            if (buttonDownL || buttonDownM) {
                rotateCamera(cam.getLeft(), -f1 * 2.5f);
            }
            if (buttonDownR) {
                panCamera(0, -f1 * 2.5f);
            }
        } else if ("MouseAxisX-".equals(string)) {
            moved = true;
            movedR = true;
            if (buttonDownL || buttonDownM) {
                rotateCamera(Vector3f.UNIT_Y, f1 * 2.5f);
            }
            if (buttonDownR) {
                panCamera(-f1 * 2.5f, 0);
            }
        } else if ("MouseAxisY-".equals(string)) {
            moved = true;
            movedR = true;
            if (buttonDownL || buttonDownM) {
                rotateCamera(cam.getLeft(), f1 * 2.5f);
            }
            if (buttonDownR) {
                panCamera(0, f1 * 2.5f);
            }
        } else if ("MouseWheel".equals(string)) {
            zoomCamera(.1f);
        } else if ("MouseWheel-".equals(string)) {
            zoomCamera(-.1f);
        }
    }

    public void onAction(String string, boolean pressed, float f) {
        if ("MouseButtonLeft".equals(string)) {


            if (pressed) {
                if (!buttonDownL) { // mouse clicked
                    checkClickL = true;
                    checkReleaseL = false;
                }
            } else {
                if (buttonDownL) { // mouse released
                    checkReleaseL = true;
                    checkClickL = false;
                }
            }
            buttonDownL = pressed;
        }
        if ("MouseButtonRight".equals(string)) {
            if (pressed) {
                if (!buttonDownR) { // mouse clicked
                    checkClickR = true;
                    checkReleaseR = false;
                }
            } else {
                if (buttonDownR) { // mouse released
                    checkReleaseR = true;
                    checkClickR = false;
                }
            }
            buttonDownR = pressed;
        }
        if ("MouseButtonMiddle".equals(string)) {
            if (pressed) {
                if (!buttonDownM) { // mouse clicked
                    checkClickM = true;
                    checkReleaseM = false;
                }
            } else {
                if (buttonDownM) { // mouse released
                    checkReleaseM = true;
                    checkClickM = false;
                }
            }
            buttonDownM = pressed;
        }


    }

    public void onJoyAxisEvent(JoyAxisEvent jae) {
    }

    public void onJoyButtonEvent(JoyButtonEvent jbe) {
    }

    public void onMouseMotionEvent(MouseMotionEvent mme) {
        mouseX = mme.getX();
        mouseY = mme.getY();
    }

    public void onMouseButtonEvent(MouseButtonEvent mbe) {
        //on a click release we request the focus for the top component
        //this allow netbeans to catch keyEvents and trigger actions according to keymapping
        if (mbe.isReleased()) {
            java.awt.EventQueue.invokeLater(new Runnable() {

                public void run() {
                    SceneViewerTopComponent.findInstance().requestActive();
                }
            });
        }

    }

    public void onKeyEvent(KeyInputEvent kie) {
    }

    public void onTouchEvent(TouchEvent evt) {
    }
    /**
     * APPSTATE*
     */
    private boolean appInit = false;

    @Override
    public void initialize(AppStateManager asm, Application aplctn) {
        appInit = true;
    }

    @Override
    public boolean isInitialized() {
        return appInit;
    }

    @Override
    public void stateAttached(AppStateManager asm) {
    }

    @Override
    public void stateDetached(AppStateManager asm) {
    }

    @Override
    public void update(float f) {
        if (moved) {
            // moved, check for drags
            if (checkReleaseL || checkReleaseR || checkReleaseM) {
                // drag released
                if (checkReleaseL) {
                    checkDragged(0, false);
                }
                if (checkReleaseR) {
                    checkDragged(1, false);
                }
                if (checkReleaseM) {
                    checkDragged(2, false);
                }
                checkReleaseL = false;
                checkReleaseR = false;
                checkReleaseM = false;
            } else {
                if (buttonDownL) {
                    checkDragged(0, true);
                } else if (buttonDownR) {
                    checkDragged(1, true);
                } else if (buttonDownM) {
                    checkDragged(2, true);
                } else {
                    checkMoved(); // no dragging, just moved
                }
            }

            moved = false;
        } else {
            // not moved, check for just clicks
            if (checkClickL) {
                checkClick(0, true);
                checkClickL = false;
            }
            if (checkReleaseL) {
                checkClick(0, false);
                checkReleaseL = false;
            }
            if (checkClickR) {
                checkClick(1, true);
                checkClickR = false;
            }
            if (checkReleaseR) {
                checkClick(1, false);
                checkReleaseR = false;
            }
            if (checkClickM) {
                checkClick(2, true);
                checkClickM = false;
            }
            if (checkReleaseM) {
                checkClick(2, false);
                checkReleaseM = false;
            }
        }

        /*
         * if (checkDragged || checkDraggedR) { if (checkDragged) {
         * checkDragged(0); checkReleaseLeft = false; checkDragged = false;
         * checkClick = false; checkClickR = false; } if (checkDraggedR) {
         * checkDragged(1); checkReleaseRight = false; checkDraggedR = false;
         * checkClick = false; checkClickR = false; } } else { if (checkClick) {
         * checkClick(0, checkReleaseLeft); checkReleaseLeft = false; checkClick
         * = false; checkDragged = false; checkDraggedR = false; } if
         * (checkClickR) { checkClick(1, checkReleaseRight); checkReleaseRight =
         * false; checkClickR = false; checkDragged = false; checkDraggedR =
         * false; } }
         */
    }

    /**
     * mouse clicked, not dragged
     * @param pressed true if pressed, false if released
     */
    protected abstract void checkClick(int button, boolean pressed);

    /**
     * Mouse dragged while button is depressed
     */
    protected void checkDragged(int button, boolean pressed) {
        // override in sub classes
    }

    /**
     * The mouse moved, no dragging or buttons pressed
     */
    protected void checkMoved() {
        // override in subclasses
    }

    @Override
    public void render(RenderManager rm) {
    }

    @Override
    public void postRender() {
    }

    @Override
    public void cleanup() {
    }

    public void beginInput() {
    }

    public void endInput() {
    }
}
