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

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.ui.Picture;

/**
 * @author JohnKkk
 */
public class DeferredShadingPass extends ScreenPass{
    public final static String S_RT_0 = "Context_InGBuff0";
    public final static String S_RT_1 = "Context_InGBuff1";
    public final static String S_RT_2 = "Context_InGBuff2";
    public final static String S_RT_3 = "Context_InGBuff3";
    public final static String S_RT_4 = "Context_InGBuff4";
    public final static String S_LIGHT_DATA = "LIGHT_DATA";
    public final static String S_EXECUTE_STATE = "EXECUTE_STATE";
    protected final static String _S_DEFERRED_PASS = "DeferredPass";
    private static final String _S_DEFERRED_SHADING_PASS_MAT_DEF = "Common/MatDefs/ShadingCommon/DeferredShading.j3md";
    public DeferredShadingPass(){
        this("DeferredShadingPass");
    }
    public DeferredShadingPass(String name) {
        super(name, RenderQueue.Bucket.Opaque);
    }

    @Override
    public void prepare(FGRenderContext renderContext) {
        super.prepare(renderContext);
        ViewPort vp = renderContext.viewPort;
        if(forceViewPort != null){
            vp = forceViewPort;
        }
        ((FGFramebufferCopyBindableSink)getSink(FGGlobal.S_DEFAULT_FB)).setDistFrameBuffer(vp.getOutputFrameBuffer());
    }

    protected Material getMaterial(){
        MaterialDef def = (MaterialDef) assetManager.loadAsset(_S_DEFERRED_SHADING_PASS_MAT_DEF);
        screenMat = new Material(def);
        return screenMat;
    }

    @Override
    public void init() {

        screenRect = new Picture(getName() + "_rect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setMaterial(getMaterial());

        // register Sinks
        registerSink(new FGTextureBindableSink<FGRenderTargetSource.RenderTargetSourceProxy>(S_RT_0, binds, binds.size(), screenMat, VarType.Texture2D));
        registerSink(new FGTextureBindableSink<FGRenderTargetSource.RenderTargetSourceProxy>(S_RT_1, binds, binds.size(), screenMat, VarType.Texture2D));
        registerSink(new FGTextureBindableSink<FGRenderTargetSource.RenderTargetSourceProxy>(S_RT_2, binds, binds.size(), screenMat, VarType.Texture2D));
        registerSink(new FGTextureBindableSink<FGRenderTargetSource.RenderTargetSourceProxy>(S_RT_3, binds, binds.size(), screenMat, VarType.Texture2D));
        registerSink(new FGTextureBindableSink<FGRenderTargetSource.RenderTargetSourceProxy>(S_RT_4, binds, binds.size(), screenMat, VarType.Texture2D));
        registerSink(new DeferredLightDataSink<DeferredLightDataSource.DeferredLightDataProxy>(S_LIGHT_DATA, binds, binds.size()));
        registerSink(new FGVarBindableSink<FGVarSource.FGVarBindableProxy>(S_EXECUTE_STATE, binds, binds.size()));
        registerSink(new FGFramebufferCopyBindableSink<FGFramebufferSource.FrameBufferSourceProxy>(FGGlobal.S_DEFAULT_FB, null, false, true, true, binds, binds.size()));
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        screenMat.selectTechnique(_S_DEFERRED_PASS, renderContext.renderManager);
        DeferredLightDataSink deferredLightDataSink = (DeferredLightDataSink) getSink(S_LIGHT_DATA);
        DeferredLightDataSource.DeferredLightDataProxy deferredLightDataProxy = (DeferredLightDataSource.DeferredLightDataProxy) deferredLightDataSink.getBind();
        LightList lights = deferredLightDataProxy.getLightData();
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenMat.setBoolean("UseLightsCullMode", false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, lights, renderContext.renderManager);
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
    }

    @Override
    public void dispatchPassSetup(RenderQueue renderQueue) {
        boolean executeState = getSink(S_EXECUTE_STATE).isLinkValidate() && ((FGVarSource.FGVarBindableProxy)getSink(S_EXECUTE_STATE).getBind()).getValue() == Boolean.TRUE;
        boolean hasLightData = getSink(S_LIGHT_DATA).isLinkValidate() && ((DeferredLightDataSource.DeferredLightDataProxy)((DeferredLightDataSink) getSink(S_LIGHT_DATA)).getBind()).getLightData().size() > 0;
        canExecute = hasLightData || executeState;
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        // Does not process any drawing in queues and always returns true, because we perform a RectDraw internally
        return true;
    }
}
