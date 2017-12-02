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
import com.jme3.light.SpotLight;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * SpotLightShadowRenderer renderer use Parrallel Split Shadow Mapping technique
 * (pssm)<br> It splits the view frustum in several parts and compute a shadow
 * map for each one.<br> splits are distributed so that the closer they are from
 * the camera, the smaller they are to maximize the resolution used of the
 * shadow map.<br> This result in a better quality shadow than standard shadow
 * mapping.<br> for more informations on this read this <a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a><br>
 * <p/>
 * @author Rémy Bouquet aka Nehon
 */
public class SpotLightShadowRenderer extends AbstractShadowRenderer {

    protected Camera shadowCam;    
    protected SpotLight light;
    protected Vector3f[] points = new Vector3f[8];
    //Holding the info for fading shadows in the far distance 
   

    /**
     * Used for serialization use SpotLightShadowRenderer#SpotLightShadowRenderer(AssetManager assetManager, int shadowMapSize)
     */
    public SpotLightShadowRenderer() {
        super();
    }
    
    /**
     * Create a SpotLightShadowRenderer This use standard shadow mapping
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...) the more quality, the less fps).
     */
    public SpotLightShadowRenderer(AssetManager assetManager, int shadowMapSize) {
        super(assetManager, shadowMapSize, 1);
        init(shadowMapSize);
    }

    
    private void init(int shadowMapSize) {
        shadowCam = new Camera(shadowMapSize, shadowMapSize);
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    @Override
    protected void initFrustumCam() {
        Camera viewCam = viewPort.getCamera();
        frustumCam = viewCam.clone();
        frustumCam.setFrustum(viewCam.getFrustumNear(), zFarOverride, viewCam.getFrustumLeft(), viewCam.getFrustumRight(), viewCam.getFrustumTop(), viewCam.getFrustumBottom());
    }

    /**
     * return the light used to cast shadows
     *
     * @return the SpotLight
     */
    public SpotLight getLight() {
        return light;
    }

    /**
     * Sets the light to use to cast shadows
     *
     * @param light a SpotLight
     */
    public void setLight(SpotLight light) {
        this.light = light;
    }

    @Override
    protected void updateShadowCams(Camera viewCam) {

        if (light == null) {
            logger.warning("The light can't be null for a " + getClass().getName());
            return;
        }

        float zFar = zFarOverride;
        if (zFar == 0) {
            zFar = viewCam.getFrustumFar();
        }

        //We prevent computing the frustum points and splits with zeroed or negative near clip value
        float frustumNear = Math.max(viewCam.getFrustumNear(), 0.001f);
        ShadowUtil.updateFrustumPoints(viewCam, frustumNear, zFar, 1.0f, points);
        //shadowCam.setDirection(direction);

        shadowCam.setFrustumPerspective(light.getSpotOuterAngle() * FastMath.RAD_TO_DEG * 2.0f, 1, 1f, light.getSpotRange());
        shadowCam.getRotation().lookAt(light.getDirection(), shadowCam.getUp());
        shadowCam.setLocation(light.getPosition());

        shadowCam.update();
        shadowCam.updateViewProjection();
    }

    @Override
    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders) {
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getGeometriesInCamFrustum(scene, shadowCam, RenderQueue.ShadowMode.Cast, shadowMapOccluders);
        }
        return shadowMapOccluders;
    }

    @Override
    protected void getReceivers(GeometryList lightReceivers) {
        lightReceivers.clear();
        Camera[] cameras = new Camera[1];
        cameras[0] = shadowCam;
        for (Spatial scene : viewPort.getScenes()) {
            ShadowUtil.getLitGeometriesInViewPort(scene, viewPort.getCamera(), cameras, RenderQueue.ShadowMode.Receive, lightReceivers);
        }
    }
    
    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCam;
    }

    @Override
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        Vector3f[] points2 = points.clone();

        ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, shadowMapIndex));
        ShadowUtil.updateFrustumPoints2(shadowCam, points2);
        ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points2, shadowMapIndex));
    }

    @Override
    protected void setMaterialParameters(Material material) {    
         material.setVector3("LightPos", light.getPosition());
         material.setVector3("LightDir", light.getDirection());
    }

    @Override
    protected void clearMaterialParameters(Material material) {
        material.clearParam("LightPos");
        material.clearParam("LightDir");
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        light = cloner.clone(light);
        init((int) shadowMapSize);
        super.cloneFields(cloner, original);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        zFarOverride = ic.readInt("zFarOverride", 0);
        light = (SpotLight) ic.readSavable("light", null);
        fadeInfo = (Vector2f) ic.readSavable("fadeInfo", null);
        fadeLength = ic.readFloat("fadeLength", 0f);
        init((int) shadowMapSize);

    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(zFarOverride, "zFarOverride", 0);
        oc.write(light, "light", null);
        oc.write(fadeInfo, "fadeInfo", null);
        oc.write(fadeLength, "fadeLength", 0f);
    }
    
    /**
     *
     * @param viewCam
     * @return 
     */
    @Override
    protected boolean checkCulling(Camera viewCam) {      
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
