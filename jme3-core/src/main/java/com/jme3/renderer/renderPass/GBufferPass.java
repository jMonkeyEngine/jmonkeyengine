/*
 * Copyright (c) 2009-2023 jMonkeyEngine
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
package com.jme3.renderer.renderPass;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGFramebufferSource;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FGRenderTargetSource;
import com.jme3.renderer.framegraph.FGVarSource;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

import java.util.ArrayList;
import java.util.List;

/**
 * @author JohnKkk
 */
public class GBufferPass extends OpaquePass{
    private final static String S_GBUFFER_PASS = "GBufferPass";
    public final static String S_RT_0 = "RT_0";
    public final static String S_RT_1 = "RT_1";
    public final static String S_RT_2 = "RT_2";
    public final static String S_RT_3 = "RT_3";
    public final static String S_RT_4 = "RT_4";
    public final static String S_FB = "GBufferFramebuffer";
    public final static String S_LIGHT_DATA = "LIGHT_DATA";
    public final static String S_EXECUTE_STATE = "EXECUTE_STATE";
    private final LightList lightData = new LightList(null);
    private final List<Light> tempLights = new ArrayList<Light>();
    private Boolean bHasDraw = new Boolean(false);
    private FGVarSource<Boolean> bHasDrawVarSource = null;
    // gBuffer
    private FrameBuffer gBuffer;
    private Texture2D gBufferData0 = null;
    private Texture2D gBufferData1 = null;
    private Texture2D gBufferData2 = null;
    private Texture2D gBufferData3 = null;
    private Texture2D gBufferData4 = null;
    private ColorRGBA gBufferMask = new ColorRGBA(0, 0, 0, 0);
    private int frameBufferWidth, frameBufferHeight;

    public GBufferPass() {
        super("GBufferPass");
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        if(canExecute){
            bHasDraw = false;
            tempLights.clear();
            lightData.clear();
            ViewPort vp = null;
            if(forceViewPort != null){
                vp = forceViewPort;
            }
            else{
                vp = renderContext.viewPort;
            }
            reshape(renderContext.renderManager.getRenderer(), vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
            FrameBuffer opfb = vp.getOutputFrameBuffer();
            vp.setOutputFrameBuffer(gBuffer);
            ColorRGBA opClearColor = vp.getBackgroundColor();
            gBufferMask.set(opClearColor);
            gBufferMask.a = 0.0f;
            renderContext.renderManager.getRenderer().setFrameBuffer(gBuffer);
            renderContext.renderManager.getRenderer().setBackgroundColor(gBufferMask);
            renderContext.renderManager.getRenderer().clearBuffers(vp.isClearColor(), vp.isClearDepth(), vp.isClearStencil());
            String techOrig = renderContext.renderManager.getForcedTechnique();
            renderContext.renderManager.setForcedTechnique(S_GBUFFER_PASS);
            super.executeDrawCommandList(renderContext);
            renderContext.renderManager.setForcedTechnique(techOrig);
            vp.setOutputFrameBuffer(opfb);
            renderContext.renderManager.getRenderer().setBackgroundColor(opClearColor);
            renderContext.renderManager.getRenderer().setFrameBuffer(vp.getOutputFrameBuffer());
            bHasDrawVarSource.setValue(bHasDraw);
            if(bHasDraw){
                for(Light light : tempLights){
                    lightData.add(light);
                }
//                renderContext.renderManager.getRenderer().copyFrameBuffer(gBuffer, vp.getOutputFrameBuffer(), false, true);
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        tempLights.clear();
        lightData.clear();
        bHasDraw = false;
        bHasDrawVarSource.setValue(bHasDraw);
    }

    public void reshape(Renderer renderer, ViewPort vp, int w, int h){
        boolean recreate = false;
        if(gBuffer != null){
            if(frameBufferWidth != w || frameBufferHeight != h){
                gBuffer.dispose();
                gBuffer.deleteObject(renderer);

                frameBufferWidth = w;
                frameBufferHeight = h;

                recreate = true;
            }
        }
        else{
            recreate = true;
            frameBufferWidth = w;
            frameBufferHeight = h;
        }

        if(recreate){
            // recreate
            // To ensure accurate results, 32bit is used here for generalization.
            gBufferData0 = new Texture2D(w, h, Image.Format.RGBA16F);
            gBufferData1 = new Texture2D(w, h, Image.Format.RGBA16F);
            gBufferData2 = new Texture2D(w, h, Image.Format.RGBA16F);
            gBufferData3 = new Texture2D(w, h, Image.Format.RGBA32F);   // The third buffer provides 32-bit floating point to store high-precision information, such as normals
            this.getSinks().clear();
            // Depth16/Depth32/Depth32F provide higher precision to prevent clipping when camera gets close, but it seems some devices do not support copying Depth16/Depth32/Depth32F to default FrameBuffer.
            gBufferData4 = new Texture2D(w, h, Image.Format.Depth);
            gBuffer = new FrameBuffer(w, h, 1);
            FrameBuffer.FrameBufferTextureTarget rt0 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData0);
            FrameBuffer.FrameBufferTextureTarget rt1 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData1);
            FrameBuffer.FrameBufferTextureTarget rt2 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData2);
            FrameBuffer.FrameBufferTextureTarget rt3 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData3);
            FrameBuffer.FrameBufferTextureTarget rt4 = FrameBuffer.FrameBufferTarget.newTarget(gBufferData4);
            gBuffer.addColorTarget(rt0);
            gBuffer.addColorTarget(rt1);
            gBuffer.addColorTarget(rt2);
            gBuffer.addColorTarget(rt3);
            gBuffer.setDepthTarget(rt4);
            gBuffer.setMultiTarget(true);
            registerSource(new FGRenderTargetSource(S_RT_0, rt0));
            registerSource(new FGRenderTargetSource(S_RT_1, rt1));
            registerSource(new FGRenderTargetSource(S_RT_2, rt2));
            registerSource(new FGRenderTargetSource(S_RT_3, rt3));
            registerSource(new FGRenderTargetSource(S_RT_4, rt4));
            registerSource(new DeferredLightDataSource(S_LIGHT_DATA, lightData));
            bHasDrawVarSource = new FGVarSource<Boolean>(S_EXECUTE_STATE, bHasDraw);
            registerSource(bHasDrawVarSource);
            registerSource(new FGFramebufferSource(S_FB, gBuffer));
        }
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if(material.getMaterialDef().getTechniqueDefs(rm.getForcedTechnique()) == null)return false;
        rm.renderGeometry(geom);
        if(material.getActiveTechnique() != null){
            if(material.getMaterialDef().getTechniqueDefs(S_GBUFFER_PASS) != null){
                LightList lights = geom.getFilterWorldLights();
                for(Light light : lights){
                    if(!tempLights.contains(light)){
                        tempLights.add(light);
                    }
                }
                // Whether it has lights or not, material objects containing GBufferPass will perform DeferredShading, and shade according to shadingModelId
                bHasDraw = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void prepare(FGRenderContext renderContext) {
        super.prepare(renderContext);
        ViewPort vp = null;
        if(forceViewPort != null){
            vp = forceViewPort;
        }
        else{
            vp = renderContext.viewPort;
        }
        reshape(renderContext.renderManager.getRenderer(), vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    }
}
