/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.shadow.next;

import com.jme3.shadow.next.pssm.DirectionalShadowParameters;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.Light.Type;
import com.jme3.light.PointLight;
import com.jme3.light.SpotLight;
import com.jme3.material.MatParamOverride;
import com.jme3.material.RenderState;
import com.jme3.math.Vector3f;
import com.jme3.post.SceneProcessor;
import com.jme3.profile.AppProfiler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.renderer.queue.OpaqueComparator;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.shader.VarType;
import com.jme3.shadow.next.array.DirectionalArrayShadowMap;
import com.jme3.shadow.next.array.PointArrayShadowMap;
import com.jme3.shadow.next.array.SpotArrayShadowMap;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.ShadowCompareMode;
import com.jme3.texture.TextureArray;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.ListMap;
import com.jme3.util.TempVars;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * The 4th generation of shadow mapping in jME3.
 * <p>
 * This version is primarily focused on rendering in-pass shadows, so pre-pass
 * and subsequent stages are separated.
 *
 * @author Kirill Vainer
 */
public class PreShadowArrayRenderer implements SceneProcessor {

    private static final String PRE_SHADOW_TECHNIQUE_NAME = "PreShadow";

    private RenderManager renderManager;
    private ViewPort viewPort;
    private final Vector3f[] points = new Vector3f[8];
    private final GeometryList shadowCasters = new GeometryList(new OpaqueComparator());
    private final ListMap<Light, ShadowMap> shadowedLights = new ListMap<>();
    private final RenderState prePassRenderState = RenderState.ADDITIONAL.clone();
    private final MatParamOverride pointLightOverride = new MatParamOverride(VarType.Boolean, "IsPointLight", true);
    private final TextureArray array = new TextureArray();
    
    private int textureSize = 1024;
    private int nextArraySlice = 0;

    // parameters for directional lights
    private final DirectionalShadowParameters directionalParams = new DirectionalShadowParameters();

    public PreShadowArrayRenderer() {
        for (int i = 0; i < points.length; i++) {
            points[i] = new Vector3f();
        }

        prePassRenderState.setFaceCullMode(RenderState.FaceCullMode.Back);
        prePassRenderState.setColorWrite(false);
        prePassRenderState.setDepthWrite(true);
        prePassRenderState.setDepthTest(true);
        prePassRenderState.setPolyOffset(0, 0);

        array.setAnisotropicFilter(1);
        array.setShadowCompareMode(ShadowCompareMode.LessOrEqual);
        array.setMagFilter(MagFilter.Nearest);
        array.setMinFilter(MinFilter.NearestNoMipMaps);
        
        array.setMagFilter(MagFilter.Bilinear);
        array.setMinFilter(MinFilter.BilinearNoMipMaps);
    }

    @Override
    public void initialize(RenderManager rm, ViewPort vp) {
        this.renderManager = rm;
        this.viewPort = vp;
    }

    public DirectionalShadowParameters directional() {
        return directionalParams;
    }

    public void setPolyOffset(float factor, float units) {
        prePassRenderState.setPolyOffset(factor, units);
    }

    public int getTextureSize() {
        return textureSize;
    }

    public void setTextureSize(int textureSize) {
        // TODO: support changing texture size after shadow maps are created
        this.textureSize = textureSize;
    }
    
    public void addLight(Light light) {
        if (array.getImage() == null) {
            array.setImage(new Image(
                    Format.Depth16,
                    textureSize,
                    textureSize,
                    0,
                    new ArrayList<ByteBuffer>(),
                    ColorSpace.Linear));
        }

        ShadowMap shadowMap;
        switch (light.getType()) {
            case Directional:
                shadowMap = new DirectionalArrayShadowMap(
                        (DirectionalLight) light,
                        array,
                        nextArraySlice,
                        textureSize,
                        directionalParams.getNumSplits());
                break;
            case Point:
                shadowMap = new PointArrayShadowMap(
                        (PointLight) light,
                        array,
                        nextArraySlice,
                        textureSize);
                break;
            case Spot:
                shadowMap = new SpotArrayShadowMap(
                        (SpotLight) light,
                        array,
                        nextArraySlice,
                        textureSize);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        
        shadowedLights.put(light, shadowMap);
        nextArraySlice += shadowMap.getNumSlices();
    }

    @Override
    public void reshape(ViewPort vp, int w, int h) {
    }

    @Override
    public boolean isInitialized() {
        return this.viewPort != null;
    }

    @Override
    public void preFrame(float tpf) {
    }

    private void renderShadowMaps(ViewPort viewPort) {
        renderManager.setForcedRenderState(prePassRenderState);
        renderManager.setForcedTechnique(PRE_SHADOW_TECHNIQUE_NAME);
        renderManager.addForcedMatParam(pointLightOverride);

        for (int i = 0; i < shadowedLights.size(); i++) {
            Light light = shadowedLights.getKey(i);
            ShadowMap shadowMap = shadowedLights.getValue(i);

            TempVars vars = TempVars.get();
            try {
                light.setFrustumCheckNeeded(false);
                light.setIntersectsFrustum(light.intersectsFrustum(viewPort.getCamera(), vars));
                if (!light.isIntersectsFrustum()) {
                    continue;
                }
            } finally {
                vars.release();
            }

            pointLightOverride.setEnabled(shadowMap.getLightType() == Type.Point);
            
            switch (shadowMap.getLightType()) {
                case Directional:
                    DirectionalArrayShadowMap directionalShadow = (DirectionalArrayShadowMap) shadowMap;
                    directionalShadow.renderShadowMap(renderManager, viewPort, directionalParams, shadowCasters, points);
                    break;
                case Point:
                    PointArrayShadowMap pointShadow = (PointArrayShadowMap) shadowMap;
                    pointShadow.renderShadowMap(renderManager, viewPort, shadowCasters);
                    break;
                case Spot:
                    SpotArrayShadowMap spotShadow = (SpotArrayShadowMap) shadowMap;
                    spotShadow.renderShadowMap(renderManager, viewPort, shadowCasters);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
            
            light.setShadowMap(shadowMap);
        }

        Renderer renderer = renderManager.getRenderer();
        renderer.setFrameBuffer(viewPort.getOutputFrameBuffer());
        renderManager.removeForcedMatParam(pointLightOverride);
        renderManager.setForcedRenderState(null);
        renderManager.setForcedTechnique(null);
        renderManager.setCamera(viewPort.getCamera(), false);
    }

    @Override
    public void postQueue(RenderQueue rq) {
        directionalParams.updateSplitPositions(viewPort.getCamera());
        renderShadowMaps(viewPort);
    }

    @Override
    public void postFrame(FrameBuffer out) {
        // TODO: call discard contents on all the framebuffers.
        for (int i = 0; i < shadowedLights.size(); i++) {
            Light light = shadowedLights.getKey(i);
            light.setShadowMap(null);
        }
    }

    @Override
    public void cleanup() {
    }

    @Override
    public void setProfiler(AppProfiler profiler) {
    }

}
