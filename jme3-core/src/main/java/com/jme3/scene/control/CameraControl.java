/*
 * Copyright (c) 2009-2025 jMonkeyEngine
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

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.util.TempVars;
import java.io.IOException;

/**
 * `CameraControl` synchronizes the world transformation (position and rotation)
 * of a `Camera` with its attached `Spatial`.
 * This control allows a camera to follow a spatial, or a spatial to follow a camera,
 * depending on the chosen {@link ControlDirection}.
 * <p>
 * This is particularly useful for attaching cameras to player characters,
 * vehicles, or dynamically controlled objects, ensuring the camera's view
 * or the spatial's position/orientation remains synchronized.
 * </p>
 *
 * @author tim
 */
public class CameraControl extends AbstractControl {

    public enum ControlDirection {
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
    private ControlDirection controlDir = ControlDirection.SpatialToCamera;

    /**
     * For serialization only. Do not use.
     */
    public CameraControl() {
    }

    /**
     * Creates a new `CameraControl` that synchronizes the spatial's transform to the camera.
     * The camera will follow the spatial.
     *
     * @param camera The Camera to be synced. Cannot be null.
     */
    public CameraControl(Camera camera) {
        this.camera = camera;
    }

    /**
     * Creates a new `CameraControl` with a specified synchronization direction.
     *
     * @param camera The Camera to be synced. Cannot be null.
     * @param controlDir The direction of synchronization (SpatialToCamera or CameraToSpatial).
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

    @Override
    protected void controlUpdate(float tpf) {
        switch (controlDir) {
            case SpatialToCamera:
                spatialToCamera();
                break;
            case CameraToSpatial:
                cameraToSpatial();
                break;
        }
    }

    /**
     * Updates the camera's position and rotation to match the spatial's
     * world transformation. The camera will follow the spatial.
     */
    private void spatialToCamera() {
        camera.setLocation(spatial.getWorldTranslation());
        camera.setRotation(spatial.getWorldRotation());
    }

    /**
     * Updates the spatial's local transformation (position and rotation)
     * such that its world transformation matches the camera's world transformation.
     * The spatial will follow the camera.
     */
    private void cameraToSpatial() {
        TempVars vars = TempVars.get();

        Vector3f position = vars.vect1.set(camera.getLocation()).subtractLocal(spatial.getWorldTranslation());
        spatial.setLocalTranslation(position.addLocal(spatial.getLocalTranslation()));
        
        Quaternion rotation = vars.quat1.set(camera.getRotation()).subtractLocal(spatial.getWorldRotation());
        spatial.setLocalRotation(rotation.addLocal(spatial.getLocalRotation()));

        vars.release();
    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
        // nothing to do
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        controlDir = ic.readEnum("controlDir", ControlDirection.class, ControlDirection.SpatialToCamera);
        camera = (Camera) ic.readSavable("camera", null);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(controlDir, "controlDir", ControlDirection.SpatialToCamera);
        oc.write(camera, "camera", null);
    }
}
