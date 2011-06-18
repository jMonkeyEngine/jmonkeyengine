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
package com.jme3.scene;

import com.jme3.renderer.Camera;
import com.jme3.scene.control.CameraControl;
import com.jme3.scene.control.CameraControl.ControlDirection;

/**
 * <code>CameraNode</code> simply uses {@link CameraControl} to implement
 * linking of camera and node data.
 *
 * @author Tim8Dev
 */
public class CameraNode extends Node {

    private CameraControl camControl;

    /**
     * Serialization only. Do not use.
     */
    public CameraNode() {
    }

    public CameraNode(String name, Camera camera) {
        this(name, new CameraControl(camera));
    }

    public CameraNode(String name, CameraControl control) {
        super(name);
        addControl(control);
        camControl = control;
    }

    public void setEnabled(boolean enabled) {
        camControl.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return camControl.isEnabled();
    }

    public void setControlDir(ControlDirection controlDir) {
        camControl.setControlDir(controlDir);
    }

    public void setCamera(Camera camera) {
        camControl.setCamera(camera);
    }

    public ControlDirection getControlDir() {
        return camControl.getControlDir();
    }

    public Camera getCamera() {
        return camControl.getCamera();
    }

//    @Override
//    public void lookAt(Vector3f position, Vector3f upVector) {
//        this.lookAt(position, upVector);
//        camControl.getCamera().lookAt(position, upVector);
//    }
}
