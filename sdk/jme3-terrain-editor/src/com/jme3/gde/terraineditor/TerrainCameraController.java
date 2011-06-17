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
package com.jme3.gde.terraineditor;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.gde.core.scene.SceneApplication;
import com.jme3.gde.core.scene.controller.AbstractCameraController;
import com.jme3.gde.core.sceneexplorer.nodes.JmeNode;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import java.util.logging.Logger;


/**
 * Runs in the JME thread, not awt thread.
 * Listens to mouse/camera input and relays the movements
 * to other controllers: editorController, toolController
 * 
 * @author normenhansen, bowens
 */
public class TerrainCameraController extends AbstractCameraController {

    private TerrainToolController toolController;
    private TerrainEditorController editorController;
    private boolean forceCameraControls = false; // when user holds shift, this is true

    private boolean terrainEditToolActivated = false;
    protected Application app;
    private float toolModifyRate = 0.05f; // how frequently (in seconds) it should update to throttle down the tool effect
    private float lastModifyTime; // last time the tool executed

    public TerrainCameraController(Camera cam) {
        super(cam, SceneApplication.getApplication().getInputManager());
        app = SceneApplication.getApplication();
    }

    public void setToolController(TerrainToolController toolController) {
        this.toolController = toolController;
    }

    public void setEditorController(TerrainEditorController editorController) {
        this.editorController = editorController;
    }

    
    @Override
    public void onMouseMotionEvent(MouseMotionEvent mme) {
        super.onMouseMotionEvent(mme);


        // if one of the terrain edit buttons is not enabled, return
        if (!isTerrainEditButtonEnabled())
            return;

        // move the marker
        Vector3f pos = getTerrainCollisionPoint();
        if (pos != null)
            toolController.doMoveEditTool(pos);
    }
    
    private boolean isTerrainEditButtonEnabled() {
        return toolController.isTerrainEditButtonEnabled();
    }

    @Override
    public void onAnalog(String string, float f1, float f) {
        if ("MouseAxisX".equals(string)) {
            moved = true;
            movedR = true;
            if (isTerrainEditButtonEnabled() && !forceCameraControls) {
                if (leftMouse)
                    terrainEditToolActivated = true;
            }
            else {
                if (leftMouse) {
                    rotateCamera(Vector3f.UNIT_Y, -f1 * 2.5f);
                }
                if (rightMouse) {
                    panCamera(f1 * 2.5f, 0);
                }
            }
        } else if ("MouseAxisY".equals(string)) {
            moved = true;
            movedR = true;
            if (isTerrainEditButtonEnabled() && !forceCameraControls) {
                if (leftMouse)
                    terrainEditToolActivated = true;
            }
            else {
                if (leftMouse) {
                    rotateCamera(cam.getLeft(), -f1 * 2.5f);
                }
                if (rightMouse) {
                    panCamera(0, -f1 * 2.5f);
                }
            }
        } else if ("MouseAxisX-".equals(string)) {
            moved = true;
            movedR = true;
            if (isTerrainEditButtonEnabled() && !forceCameraControls) {
                if (leftMouse)
                    terrainEditToolActivated = true;
            }
            else {
                if (leftMouse) {
                    rotateCamera(Vector3f.UNIT_Y, f1 * 2.5f);
                }
                if (rightMouse) {
                    panCamera(-f1 * 2.5f, 0);
                }
            }
        } else if ("MouseAxisY-".equals(string)) {
            moved = true;
            movedR = true;
            if (isTerrainEditButtonEnabled() && !forceCameraControls) {
                if (leftMouse)
                    terrainEditToolActivated = true;
            }
            else {
                if (leftMouse) {
                    rotateCamera(cam.getLeft(), f1 * 2.5f);
                }
                if (rightMouse) {
                    panCamera(0, f1 * 2.5f);
                }
            }
        } else if ("MouseWheel".equals(string)) {
            zoomCamera(.1f);
        } else if ("MouseWheel-".equals(string)) {
            zoomCamera(-.1f);
        }
    }

    @Override
    public void update(float f) {
        super.update(f);
        
        doTerrainUpdates(f);
    }

    @Override
    protected void checkClick(int button) {
        if (button == 0) {
            if (isTerrainEditButtonEnabled() && !forceCameraControls) {
                if (leftMouse)
                    terrainEditToolActivated = true;
            }
        }
        if (button == 1) {
            if (isTerrainEditButtonEnabled() && !forceCameraControls) {
                toolController.doTerrainEditToolAlternateActivated();
            }
        }
    }

    /**
     * Update the terrain if it has had any editing done on it.
     * We do it with a Timer to control the intensity and frequency
     * of the editing.
     */
    private void doTerrainUpdates(float dt) {

        if (terrainEditToolActivated) {
            lastModifyTime += dt;
            
            if (lastModifyTime >= toolModifyRate) {
                
                lastModifyTime = 0;
                if (terrainEditToolActivated)
                    toolController.doTerrainEditToolActivated();
                terrainEditToolActivated = false;
                lastModifyTime = app.getContext().getTimer().getTime();
            }
        }
    }

    @Override
    public void onKeyEvent(KeyInputEvent kie) {
        if (kie.isPressed()) {
            if ( KeyInput.KEY_LSHIFT == kie.getKeyCode() ) {
                forceCameraControls = true;
            }
        } else if (kie.isReleased()){
            if ( KeyInput.KEY_LSHIFT == kie.getKeyCode() ) {
                forceCameraControls = false;
            }
        }
    }

    /**
     * Find where on the terrain the mouse intersects.
     */
    protected Vector3f getTerrainCollisionPoint() {

        if (editorController.getTerrain(null) == null)
            return null;

        CollisionResults results = new CollisionResults();
        Ray ray = new Ray();
        Vector3f pos = cam.getWorldCoordinates(new Vector2f(mouseX, mouseY), 0).clone();
        Vector3f dir = cam.getWorldCoordinates(new Vector2f(mouseX, mouseY), 0.3f).clone();
        dir.subtractLocal(pos).normalizeLocal();
        ray.setOrigin(pos);
        ray.setDirection(dir);
        editorController.getTerrain(null).collideWith(ray, results);
        if (results == null) {
            return null;
        }
        final CollisionResult result = results.getClosestCollision();
        if (result == null)
            return null;
        return result.getContactPoint();
    }

    
    

}
