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
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.util.BufferUtils;


/**
 *  Registers a few keys that will dump debug information
 *  to the console.
 *
 *  @author    Paul Speed
 */
public class DebugKeysAppState extends AbstractAppState {

    public static final String INPUT_MAPPING_CAMERA_POS = "SIMPLEAPP_CameraPos";
    public static final String INPUT_MAPPING_MEMORY = "SIMPLEAPP_Memory";
    
    private Application app;
    private DebugKeyListener keyListener = new DebugKeyListener();
    private InputManager inputManager;

    public DebugKeysAppState() {
    }    

    @Override
    public void initialize(AppStateManager stateManager, Application app) {
        super.initialize(stateManager, app);
        
        this.app = app;
        this.inputManager = app.getInputManager();
        
        if (app.getInputManager() != null) {
 
            inputManager.addMapping(INPUT_MAPPING_CAMERA_POS, new KeyTrigger(KeyInput.KEY_C));
            inputManager.addMapping(INPUT_MAPPING_MEMORY, new KeyTrigger(KeyInput.KEY_M));
            
            inputManager.addListener(keyListener, 
                                     INPUT_MAPPING_CAMERA_POS, 
                                     INPUT_MAPPING_MEMORY);                   
        }               
    }
            
    @Override
    public void cleanup() {
        super.cleanup();

        if (inputManager.hasMapping(INPUT_MAPPING_CAMERA_POS))
            inputManager.deleteMapping(INPUT_MAPPING_CAMERA_POS);
        if (inputManager.hasMapping(INPUT_MAPPING_MEMORY))
            inputManager.deleteMapping(INPUT_MAPPING_MEMORY);
        
        inputManager.removeListener(keyListener);
    }

    
    private class DebugKeyListener implements ActionListener {

        public void onAction(String name, boolean value, float tpf) {
            if (!value) {
                return;
            }

            if (name.equals(INPUT_MAPPING_CAMERA_POS)) {
                Camera cam = app.getCamera();
                if (cam != null) {
                    Vector3f loc = cam.getLocation();
                    Quaternion rot = cam.getRotation();
                    System.out.println("Camera Position: ("
                            + loc.x + ", " + loc.y + ", " + loc.z + ")");
                    System.out.println("Camera Rotation: " + rot);
                    System.out.println("Camera Direction: " + cam.getDirection());
                }
            } else if (name.equals(INPUT_MAPPING_MEMORY)) {
                BufferUtils.printCurrentDirectMemory(null);
            }
        }
    }
}
