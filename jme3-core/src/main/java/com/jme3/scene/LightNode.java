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
package com.jme3.scene;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.light.Light;
import com.jme3.scene.control.LightControl;
import com.jme3.scene.control.LightControl.ControlDirection;
import com.jme3.util.clone.Cloner;
import java.io.IOException;

/**
 * <code>LightNode</code> is used to link together a {@link Light} object
 * with a {@link Node} object.
 *
 * @author Tim8Dev
 */
public class LightNode extends Node {

    private LightControl lightControl;

    /**
     * Serialization only. Do not use.
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

    /**
     * Enable or disable the <code>LightNode</code> functionality.
     *
     * @param enabled If false, the functionality of LightNode will
     * be disabled.
     */
    public void setEnabled(boolean enabled) {
        lightControl.setEnabled(enabled);
    }

    public boolean isEnabled() {
        return lightControl.isEnabled();
    }

    public void setControlDir(ControlDirection controlDir) {
        lightControl.setControlDir(controlDir);
    }

    public void setLight(Light light) {
        lightControl.setLight(light);
    }

    public ControlDirection getControlDir() {
        return lightControl.getControlDir();
    }

    public Light getLight() {
        return lightControl.getLight();
    }

    /**
     *  Called internally by com.jme3.util.clone.Cloner.  Do not call directly.
     */
    @Override
    public void cloneFields( Cloner cloner, Object original ) {
        super.cloneFields(cloner, original);

        // A change in behavior... I think previously LightNode was probably
        // not really cloneable... or at least its lightControl would be pointing
        // to the wrong control. -pspeed
        this.lightControl = cloner.clone(lightControl);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        lightControl = (LightControl)im.getCapsule(this).readSavable("lightControl", null);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        ex.getCapsule(this).write(lightControl, "lightControl", null);
    }
}
