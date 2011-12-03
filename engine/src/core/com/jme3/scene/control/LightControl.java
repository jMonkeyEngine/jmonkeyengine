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
package com.jme3.scene.control;

import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * This Control maintains a reference to a Camera,
 * which will be synched with the position (worldTranslation)
 * of the current spatial.
 * @author tim
 */
public class LightControl extends AbstractControl {

    public static enum ControlDirection {

        /**
         * Means, that the Light's transform is "copied"
         * to the Transform of the Spatial.
         */
        LightToSpatial,
        /**
         * Means, that the Spatial's transform is "copied"
         * to the Transform of the light.
         */
        SpatialToLight;
    }
    private Light light;
    private ControlDirection controlDir = ControlDirection.SpatialToLight;

    /**
     * Constructor used for Serialization.
     */
    public LightControl() {
    }

    /**
     * @param light The light to be synced.
     */
    public LightControl(Light light) {
        this.light = light;
    }

    /**
     * @param light The light to be synced.
     */
    public LightControl(Light light, ControlDirection controlDir) {
        this.light = light;
        this.controlDir = controlDir;
    }

    public Light getLight() {
        return light;
    }

    public void setLight(Light light) {
        this.light = light;
    }

    public ControlDirection getControlDir() {
        return controlDir;
    }

    public void setControlDir(ControlDirection controlDir) {
        this.controlDir = controlDir;
    }

    // fields used, when inversing ControlDirection:
    @Override
    protected void controlUpdate(float tpf) {
        if (spatial != null && light != null) {
            switch (controlDir) {
                case SpatialToLight:
                    spatialTolight(light);
                    break;
                case LightToSpatial:
                    lightToSpatial(light);
                    break;
            }
        }
    }

    private void spatialTolight(Light light) {
        if (light instanceof PointLight) {
            ((PointLight) light).setPosition(spatial.getWorldTranslation());
        }
        TempVars vars = TempVars.get();

        if (light instanceof DirectionalLight) {
            ((DirectionalLight) light).setDirection(vars.vect1.set(spatial.getWorldTranslation()).multLocal(-1.0f));
        }
        vars.release();
        //TODO add code for Spot light here when it's done
//        if( light instanceof SpotLight){
//            ((SpotLight)light).setPosition(spatial.getWorldTranslation());                
//            ((SpotLight)light).setRotation(spatial.getWorldRotation());
//        }

    }

    private void lightToSpatial(Light light) {
        TempVars vars = TempVars.get();
        if (light instanceof PointLight) {

            PointLight pLight = (PointLight) light;

            Vector3f vecDiff = vars.vect1.set(pLight.getPosition()).subtractLocal(spatial.getWorldTranslation());
            spatial.setLocalTranslation(vecDiff.addLocal(spatial.getLocalTranslation()));
        }

        if (light instanceof DirectionalLight) {
            DirectionalLight dLight = (DirectionalLight) light;
            vars.vect1.set(dLight.getDirection()).multLocal(-1.0f);
            Vector3f vecDiff = vars.vect1.subtractLocal(spatial.getWorldTranslation());
            spatial.setLocalTranslation(vecDiff.addLocal(spatial.getLocalTranslation()));
        }
        vars.release();
        //TODO add code for Spot light here when it's done


    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing to do
    }

    @Override
    public Control cloneForSpatial(Spatial newSpatial) {
        LightControl control = new LightControl(light, controlDir);
        control.setSpatial(newSpatial);
        control.setEnabled(isEnabled());
        return control;
    }
    private static final String CONTROL_DIR_NAME = "controlDir";

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        im.getCapsule(this).readEnum(CONTROL_DIR_NAME,
                ControlDirection.class, ControlDirection.SpatialToLight);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        ex.getCapsule(this).write(controlDir, CONTROL_DIR_NAME,
                ControlDirection.SpatialToLight);
    }
}