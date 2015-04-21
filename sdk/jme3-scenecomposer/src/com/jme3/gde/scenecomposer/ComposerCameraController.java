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
package com.jme3.gde.scenecomposer;

import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.AbstractCameraController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;

/**
 *
 * @author normenhansen
 */
public class ComposerCameraController extends AbstractCameraController {

    private SceneComposerToolController toolController;
    private boolean forceCameraControls = false; // when user holds shift, this is true

    public ComposerCameraController(Camera cam, JmeNode rootNode) {
        super(cam, SceneApplication.getApplication().getInputManager());
    }

    private boolean isEditButtonEnabled() {
        return toolController.isEditToolEnabled();
    }

    public void setToolController(SceneComposerToolController toolController) {
        this.toolController = toolController;
    }

    public boolean isToolUsesCameraControls() {
        return !toolController.isOverrideCameraControl();
    }

    public Camera getCamera() {
        return cam;
    }

    @Override
    public void onKeyEvent(KeyInputEvent kie) {
        //don't forget the super call
        super.onKeyEvent(kie);
        if (kie.isPressed()) {
            if (KeyInput.KEY_LSHIFT == kie.getKeyCode()) {
                forceCameraControls = true;
            }
        } else if (kie.isReleased()) {
            if (KeyInput.KEY_LSHIFT == kie.getKeyCode()) {
                forceCameraControls = false;
            }
        }
        toolController.doKeyPressed(kie);
    }

    @Override
    public void checkClick(int button, boolean pressed) {
        if (!forceCameraControls || !pressed) { // dont call toolController while forceCam but on button release (for UndoRedo)
            if (button == 0) {
                toolController.doEditToolActivatedPrimary(new Vector2f(mouseX, mouseY), pressed, cam);
            }
            if (button == 1) {
                toolController.doEditToolActivatedSecondary(new Vector2f(mouseX, mouseY), pressed, cam);
            }
        }
    }

    @Override
    protected void checkDragged(int button, boolean pressed) {
        if (!forceCameraControls || !pressed) {
            if (button == 0) {
                toolController.doEditToolDraggedPrimary(new Vector2f(mouseX, mouseY), pressed, cam);
            } else if (button == 1) {
                toolController.doEditToolDraggedSecondary(new Vector2f(mouseX, mouseY), pressed, cam);
            }
        }
    }

    @Override
    protected void checkMoved() {
        toolController.doEditToolMoved(new Vector2f(mouseX, mouseY), cam);
    }

    @Override
    public boolean useCameraControls() {
        return isToolUsesCameraControls() || forceCameraControls;
    }

}
