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

import com.jme3.light.Light;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.control.LightControl.ControlDirection;

/**
 * This Node is a shorthand for using a CameraControl.
 *
 * @author Tim8Dev
 */
public class LightNode extends Node {

    private LightControl lightControl;

    /**
     * for IO purpose
     */
    public LightNode() {
    }

    public LightNode(String name, Light light) {
        this(name, new LightControl(light));
    }

    public LightNode(String name, LightControl control) {
        super(name);
        addControl(control);
        lightControl = control;
    }

    public void setEnabled(boolean enabled) {
        lightControl.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return lightControl.isEnabled();
    }

    public void setControlDir(ControlDirection controlDir) {
        lightControl.setControlDir(controlDir);
    }

    public void setCamera(Light light) {
        lightControl.setLight(light);
    }

    public ControlDirection getControlDir() {
        return lightControl.getControlDir();
    }

    public Light getCamera() {
        return lightControl.getLight();
    }
}
