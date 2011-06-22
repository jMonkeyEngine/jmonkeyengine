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
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
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
public class CameraControl extends AbstractControl {

    public static enum ControlDirection {

        /**
         * Means, that the Camera's transform is "copied"
         * to the Transform of the Spatial.
         */
        CameraToSpatial,
        /**
         * Means, that the Spatial's transform is "copied"
         * to the Transform of the Camera.
         */
        SpatialToCamera;
    }
    private Camera camera;
    private ControlDirection controlDir = ControlDirection.CameraToSpatial;

    /**
     * Constructor used for Serialization.
     */
    public CameraControl() {
    }

    /**
     * @param camera The Camera to be synced.
     */
    public CameraControl(Camera camera) {
        this.camera = camera;
    }

    /**
     * @param camera The Camera to be synced.
     */
    public CameraControl(Camera camera, ControlDirection controlDir) {
        this.camera = camera;
        this.controlDir = controlDir;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
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
        if (spatial != null && camera != null) {
            switch (controlDir) {
                case SpatialToCamera:
                    camera.setLocation(spatial.getWorldTranslation());
                    camera.setRotation(spatial.getWorldRotation());
                    break;
                case CameraToSpatial:
                    // set the localtransform, so that the worldtransform would be equal to the camera's transform.
                    // Location:
                    TempVars vars = TempVars.get();

                    Vector3f vecDiff = vars.vect1.set(camera.getLocation()).subtractLocal(spatial.getWorldTranslation());
                    spatial.setLocalTranslation(vecDiff.addLocal(spatial.getLocalTranslation()));

                    // Rotation:
                    Quaternion worldDiff = vars.quat1.set(camera.getRotation()).subtractLocal(spatial.getWorldRotation());
                    spatial.setLocalRotation(worldDiff.addLocal(spatial.getLocalRotation()));
                    vars.release();
                    break;
            }
        }
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing to do
    }

    @Override
    public Control cloneForSpatial(Spatial newSpatial) {
        CameraControl control = new CameraControl(camera, controlDir);
        control.setSpatial(newSpatial);
        control.setEnabled(isEnabled());
        return control;
    }
    private static final String CONTROL_DIR_NAME = "controlDir";

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        im.getCapsule(this).readEnum(CONTROL_DIR_NAME,
                ControlDirection.class, ControlDirection.SpatialToCamera);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        ex.getCapsule(this).write(controlDir, CONTROL_DIR_NAME,
                ControlDirection.SpatialToCamera);
    }
}