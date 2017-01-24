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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * PointLightShadowRenderer renders shadows for a point light
 *
 * @author Rémy Bouquet aka Nehon
 */
public class PointLightShadowRenderer extends AbstractShadowRenderer {

    public static final int CAM_NUMBER = 6;
    protected PointLight light;
    protected Camera[] shadowCams;
    private Geometry[] frustums = null;

    /**
     * Used for serialization use
     * PointLightShadowRenderer"PointLightShadowRenderer(AssetManager
     * assetManager, int shadowMapSize)
     */
    public PointLightShadowRenderer() {
        super();
    }

    /**
     * Creates a PointLightShadowRenderer
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     */
    public PointLightShadowRenderer(AssetManager assetManager, int shadowMapSize) {
        super(assetManager, shadowMapSize, CAM_NUMBER);
        init(shadowMapSize);
    }

    private void init(int shadowMapSize) {
        shadowCams = new Camera[CAM_NUMBER];
        for (int i = 0; i < CAM_NUMBER; i++) {
            shadowCams[i] = new Camera(shadowMapSize, shadowMapSize);
        }
    }
    
    @Override
    protected void initFrustumCam() {
        Camera viewCam = viewPort.getCamera();
        frustumCam = viewCam.clone();
        frustumCam.setFrustum(viewCam.getFrustumNear(), zFarOverride, viewCam.getFrustumLeft(), viewCam.getFrustumRight(), viewCam.getFrustumTop(), viewCam.getFrustumBottom());
    }
    

    @Override
    protected void updateShadowCams(Camera viewCam) {

        if (light == null) {
            logger.warning("The light can't be null for a " + getClass().getName());
            return;
        }

        //bottom
        shadowCams[0].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y.mult(-1f));

        //top
        shadowCams[1].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Z, Vector3f.UNIT_Y);

        //forward
        shadowCams[2].setAxes(Vector3f.UNIT_X.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_Z.mult(-1f));

        //backward
        shadowCams[3].setAxes(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z);

        //left
        shadowCams[4].setAxes(Vector3f.UNIT_Z, Vector3f.UNIT_Y, Vector3f.UNIT_X.mult(-1f));

        //right
        shadowCams[5].setAxes(Vector3f.UNIT_Z.mult(-1f), Vector3f.UNIT_Y, Vector3f.UNIT_X);

        for (int i = 0; i < CAM_NUMBER; i++) {
            shadowCams[i].setFrustumPerspective(90f, 1f, 0.1f, light.getRadius());
            shadowCams[i].setLocation(light.getPosition());
            shadowCams[i].update();
            shadowCams[i].updateViewProjection();
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
        if (frustums == null) {
            frustums = new Geometry[CAM_NUMBER];
            Vector3f[] points = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                points[i] = new Vector3f();
            }
            for (int i = 0; i < CAM_NUMBER; i++) {
                ShadowUtil.updateFrustumPoints2(shadowCams[i], points);
                frustums[i] = createFrustum(points, i);
            }
        }
        if (frustums[shadowMapIndex].getParent() == null) {
            ((Node) viewPort.getScenes().get(0)).attachChild(frustums[shadowMapIndex]);
        }
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
     * @param viewCam
     * @return 
     */
    @Override
    protected boolean checkCulling(Camera viewCam) {

        if (light == null) {
            return false;
        }

        Camera cam = viewCam;
        if(frustumCam != null){
            cam = frustumCam;            
            cam.setLocation(viewCam.getLocation());
            cam.setRotation(viewCam.getRotation());
        }
        TempVars vars = TempVars.get();
        boolean intersects = light.intersectsFrustum(cam,vars);
        vars.release();
        return intersects;
    }
}
