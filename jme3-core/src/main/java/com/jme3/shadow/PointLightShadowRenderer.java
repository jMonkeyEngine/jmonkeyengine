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
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * Renders shadows for a {@link PointLight}. This renderer uses six cameras,
 * one for each face of a cube map, to capture shadows from the point light's
 * perspective.
 *
 * @author Rémy Bouquet aka Nehon
 */
public class PointLightShadowRenderer extends AbstractShadowRenderer {

    /**
     * The fixed number of cameras used for rendering point light shadows (6 for a cube map).
     */
    public static final int CAM_NUMBER = 6;

    protected PointLight light;
    protected Camera[] shadowCams;
    protected Geometry[] frustums = null;
    protected final Vector3f X_NEG = Vector3f.UNIT_X.mult(-1f);
    protected final Vector3f Y_NEG = Vector3f.UNIT_Y.mult(-1f);
    protected final Vector3f Z_NEG = Vector3f.UNIT_Z.mult(-1f);

    /**
     * For serialization only. Do not use.
     */
    protected PointLightShadowRenderer() {
        super();
    }

    /**
     * Creates a new {@code PointLightShadowRenderer} instance.
     *
     * @param assetManager The application's asset manager.
     * @param shadowMapSize The size of the rendered shadow maps (e.g., 512, 1024, 2048).
     * Higher values produce better quality shadows but may impact performance.
     */
    public PointLightShadowRenderer(AssetManager assetManager, int shadowMapSize) {
        super(assetManager, shadowMapSize, CAM_NUMBER);
        init(shadowMapSize);
    }

    private void init(int shadowMapSize) {
        shadowCams = new Camera[CAM_NUMBER];
        for (int i = 0; i < shadowCams.length; i++) {
            shadowCams[i] = new Camera(shadowMapSize, shadowMapSize);
        }
    }
    
    @Override
    protected void initFrustumCam() {
        Camera viewCam = viewPort.getCamera();
        frustumCam = viewCam.clone();
        frustumCam.setFrustum(viewCam.getFrustumNear(), zFarOverride,
                viewCam.getFrustumLeft(), viewCam.getFrustumRight(), viewCam.getFrustumTop(), viewCam.getFrustumBottom());
    }

    @Override
    protected void updateShadowCams(Camera viewCam) {

        if (light == null) {
            logger.warning("The light can't be null for a " + getClass().getName());
            return;
        }

        // Configure axes for each of the six cube map cameras (positive/negative X, Y, Z)
        shadowCams[0].setAxes(X_NEG, Z_NEG, Y_NEG);                                 // -Y (bottom)
        shadowCams[1].setAxes(X_NEG, Vector3f.UNIT_Z, Vector3f.UNIT_Y);             // +Y (top)
        shadowCams[2].setAxes(X_NEG, Vector3f.UNIT_Y, Z_NEG);                       // +Z (forward)
        shadowCams[3].setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);   // -Z (backward)
        shadowCams[4].setAxes(Vector3f.UNIT_Z, Vector3f.UNIT_Y, X_NEG);             // -X (left)
        shadowCams[5].setAxes(Z_NEG, Vector3f.UNIT_Y, Vector3f.UNIT_X);             // +X (right)

        // Set perspective and location for all shadow cameras
        for (Camera shadowCam : shadowCams) {
            shadowCam.setFrustumPerspective(90f, 1f, 0.1f, light.getRadius());
            shadowCam.setLocation(light.getPosition());
            shadowCam.update();
            shadowCam.updateViewProjection();
        }
    }

    @Override
    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders) {
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getGeometriesInCamFrustum(scene, shadowCams[shadowMapIndex], RenderQueue.ShadowMode.Cast, shadowMapOccluders);
        }
        return shadowMapOccluders;
    }

    @Override
    protected void getReceivers(GeometryList lightReceivers) {
        lightReceivers.clear();
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getLitGeometriesInViewPort(scene, viewPort.getCamera(), shadowCams, RenderQueue.ShadowMode.Receive, lightReceivers);
        }
    }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCams[shadowMapIndex];
    }

    @Override
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        frustums = new Geometry[CAM_NUMBER];
        Vector3f[] points = new Vector3f[8];

        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

        for (int i = 0; i < CAM_NUMBER; i++) {
            ShadowUtil.updateFrustumPoints2(shadowCams[i], points);
            frustums[i] = createFrustum(points, i);
        }

        Geometry geo = frustums[shadowMapIndex];
        getMainScene().attachChild(geo);
    }

    @Override
    protected void setMaterialParameters(Material material) {
        material.setVector3("LightPos", light == null ? new Vector3f() : light.getPosition());
    }

    @Override
    protected void clearMaterialParameters(Material material) {
        material.clearParam("LightPos");        
    }
    
    /**
     * gets the point light used to cast shadows with this processor
     *
     * @return the point light
     */
    public PointLight getLight() {
        return light;
    }

    /**
     * sets the light to use for casting shadows with this processor
     *
     * @param light the point light
     */
    public void setLight(PointLight light) {
        this.light = light;
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        light = cloner.clone(light);
        init((int) shadowMapSize);
        frustums = null;
        super.cloneFields(cloner, original);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        light = (PointLight) ic.readSavable("light", null);
        init((int) shadowMapSize);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(light, "light", null);
    }
    
    /**
     *
     * @param viewCam a Camera to define the view frustum
     * @return true if intersects
     */
    @Override
    protected boolean checkCulling(Camera viewCam) {

        if (light == null) {
            return false;
        }

        Camera cam = viewCam;
        if (frustumCam != null) {
            cam = frustumCam;
            cam.setLocation(viewCam.getLocation());
            cam.setRotation(viewCam.getRotation());
        }
        TempVars vars = TempVars.get();
        boolean intersects = light.intersectsFrustum(cam, vars);
        vars.release();
        return intersects;
    }
}
