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
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.util.clone.Cloner;

import java.io.IOException;

/**
 * DirectionalLightShadowRenderer renderer use Parrallel Split Shadow Mapping
 * technique (pssm)<br> It splits the view frustum in several parts and compute
 * a shadow map for each one.<br> splits are distributed so that the closer they
 * are from the camera, the smaller they are to maximize the resolution used of
 * the shadow map.<br> This result in a better quality shadow than standard
 * shadow mapping.<br> for more informations on this read this <a
 * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a><br>
 * <p/>
 * @author Rémy Bouquet aka Nehon
 */
public class DirectionalLightShadowRenderer extends AbstractShadowRenderer {

    protected float lambda = 0.65f;    
    protected Camera shadowCam;
    protected ColorRGBA splits;
    protected float[] splitsArray;
    protected DirectionalLight light;
    protected Vector3f[] points = new Vector3f[8];
    //Holding the info for fading shadows in the far distance   
    private boolean stabilize = true;

    /**
     * Used for serialzation use
     * DirectionalLightShadowRenderer#DirectionalLightShadowRenderer(AssetManager
     * assetManager, int shadowMapSize, int nbSplits)
     */
    public DirectionalLightShadowRenderer() {
        super();
    }

    /**
     * Create a DirectionalLightShadowRenderer More info on the technique at <a
     * href="http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html">http://http.developer.nvidia.com/GPUGems3/gpugems3_ch10.html</a>
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...)
     * @param nbSplits the number of shadow maps rendered (the more shadow maps
     * the more quality, the less fps).
     */
    public DirectionalLightShadowRenderer(AssetManager assetManager, int shadowMapSize, int nbSplits) {
        super(assetManager, shadowMapSize, nbSplits);
        init(nbSplits, shadowMapSize);
    }

    private void init(int nbSplits, int shadowMapSize) {
        nbShadowMaps = Math.max(Math.min(nbSplits, 4), 1);
        if (nbShadowMaps != nbSplits) {
            throw new IllegalArgumentException("Number of splits must be between 1 and 4. Given value : " + nbSplits);
        }
        splits = new ColorRGBA();
        splitsArray = new float[nbSplits + 1];
        shadowCam = new Camera(shadowMapSize, shadowMapSize);
        shadowCam.setParallelProjection(true);
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }
    }

    @Override
    protected void initFrustumCam() {
        //nothing to do
    }

    /**
     * return the light used to cast shadows
     *
     * @return the DirectionalLight
     */
    public DirectionalLight getLight() {
        return light;
    }

    /**
     * Sets the light to use to cast shadows
     *
     * @param light a DirectionalLight
     */
    public void setLight(DirectionalLight light) {
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

        shadowCam.setFrustumFar(zFar);
        shadowCam.getRotation().lookAt(light.getDirection(), shadowCam.getUp());
        shadowCam.update();
        shadowCam.updateViewProjection();

        PssmShadowUtil.updateFrustumSplits(splitsArray, frustumNear, zFar, lambda);

        // in parallel projection shadow position goe from 0 to 1
        if(viewCam.isParallelProjection()){
            for (int i = 0; i < nbShadowMaps; i++) {
                splitsArray[i] = splitsArray[i]/(zFar- frustumNear);
            }
        }

        switch (splitsArray.length) {
            case 5:
                splits.a = splitsArray[4];
            case 4:
                splits.b = splitsArray[3];
            case 3:
                splits.g = splitsArray[2];
            case 2:
            case 1:
                splits.r = splitsArray[1];
                break;
        }

    }
    
    @Override
    protected GeometryList getOccludersToRender(int shadowMapIndex, GeometryList shadowMapOccluders) {

        // update frustum points based on current camera and split
        ShadowUtil.updateFrustumPoints(viewPort.getCamera(), splitsArray[shadowMapIndex], splitsArray[shadowMapIndex + 1], 1.0f, points);

        //Updating shadow cam with curent split frustra
        if (lightReceivers.size()==0) {
            for (Spatial scene : viewPort.getScenes()) {
              ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), RenderQueue.ShadowMode.Receive, lightReceivers);
            }
        }
        ShadowUtil.updateShadowCamera(viewPort, lightReceivers, shadowCam, points, shadowMapOccluders, stabilize?shadowMapSize:0);

        return shadowMapOccluders;
    }

    @Override
    protected void getReceivers(GeometryList lightReceivers) {
        if (lightReceivers.size()==0) {
            for (Spatial scene : viewPort.getScenes()) {
                ShadowUtil.getGeometriesInCamFrustum(scene, viewPort.getCamera(), RenderQueue.ShadowMode.Receive, lightReceivers);
            }
        }
    }

    @Override
    protected Camera getShadowCam(int shadowMapIndex) {
        return shadowCam;
    }

    @Override
    protected void doDisplayFrustumDebug(int shadowMapIndex) {
        ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, shadowMapIndex));
        ShadowUtil.updateFrustumPoints2(shadowCam, points);
        ((Node) viewPort.getScenes().get(0)).attachChild(createFrustum(points, shadowMapIndex));
    }

    @Override
    protected void setMaterialParameters(Material material) {
        material.setColor("Splits", splits);
        material.setVector3("LightDir", light == null ? new Vector3f() : light.getDirection());
        if (fadeInfo != null) {
            material.setVector2("FadeInfo", fadeInfo);
        }
    }

    @Override
    protected void clearMaterialParameters(Material material) {
        material.clearParam("Splits");
        material.clearParam("FadeInfo");
        material.clearParam("LightDir");
    }

    /**
     * returns the labda parameter see #setLambda(float lambda)
     *
     * @return lambda
     */
    public float getLambda() {
        return lambda;
    }

    /*
     * Adjust the repartition of the different shadow maps in the shadow extend
     * usualy goes from 0.0 to 1.0
     * a low value give a more linear repartition resulting in a constant quality in the shadow over the extends, but near shadows could look very jagged
     * a high value give a more logarithmic repartition resulting in a high quality for near shadows, but the quality quickly decrease over the extend.
     * the default value is set to 0.65f (theoric optimal value).
     * @param lambda the lambda value.
     */
    public void setLambda(float lambda) {
        this.lambda = lambda;
    }
    
    /**
     * @return true if stabilization is enabled
     */
    public boolean isEnabledStabilization() {
        return stabilize;
    }
    
    /**
     * Enables the stabilization of the shadows's edges. (default is true)
     * This prevents shadows' edges to flicker when the camera moves
     * However it can lead to some shadow quality loss in some particular scenes.
     * @param stabilize 
     */
    public void setEnabledStabilization(boolean stabilize) {
        this.stabilize = stabilize;
    }

    @Override
    public void cloneFields(final Cloner cloner, final Object original) {
        light = cloner.clone(light);
        init(nbShadowMaps, (int) shadowMapSize);
        super.cloneFields(cloner, original);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        lambda = ic.readFloat("lambda", 0.65f);
        zFarOverride = ic.readInt("zFarOverride", 0);
        light = (DirectionalLight) ic.readSavable("light", null);
        fadeInfo = (Vector2f) ic.readSavable("fadeInfo", null);
        fadeLength = ic.readFloat("fadeLength", 0f);
        init(nbShadowMaps, (int) shadowMapSize);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(lambda, "lambda", 0.65f);
        oc.write(zFarOverride, "zFarOverride", 0);
        oc.write(light, "light", null);
        oc.write(fadeInfo, "fadeInfo", null);
        oc.write(fadeLength, "fadeLength", 0f);
    }

    /**
     * Directional light are always in the view frustum
     * @param viewCam
     * @return 
     */
    @Override
    protected boolean checkCulling(Camera viewCam) {
        return true;
    }
}
