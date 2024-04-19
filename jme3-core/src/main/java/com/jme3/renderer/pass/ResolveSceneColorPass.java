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
package com.jme3.renderer.pass;

import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.*;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.shader.VarType;
import com.jme3.ui.Picture;

/**
 * @author JohnKkk
 */
public class ResolveSceneColorPass extends ScreenPass {
    
    public final static String S_SCENE_COLOR_RT = "SceneColorRT";
    public final static String S_SCENE_DEPTH = "SceneDepth";
    private static final String _S_RESOLVE_SCENE_COLOR_MAT_DEF = "Common/MatDefs/Misc/ResolveSceneColor.j3md";

    public ResolveSceneColorPass(String name) {
        super(name, RenderQueue.Bucket.Opaque);
    }

    @Override
    public void dispatchPassSetup(RenderQueue renderQueue) {
        canExecute = true;
    }

    @Override
    public void executeDrawCommands(RenderContext renderContext) {
        renderContext.getRenderer().setFrameBuffer(null);
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, renderContext.getRenderManager());
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
    }
    public void updateExposure(float exposure){
        screenMat.setFloat("Exposure", exposure);
    }

    @Override
    public boolean drawGeometry(RenderManager rm, Geometry geom) {
        return true;
    }

    @Override
    public void init() {
        MaterialDef def = (MaterialDef) assetManager.loadAsset(_S_RESOLVE_SCENE_COLOR_MAT_DEF);
        screenMat = new Material(def);
        screenRect = new Picture(getName() + "_rect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setMaterial(screenMat);

        // register Sinks
        registerBindableSink(new MatParamSink(S_SCENE_COLOR_RT, screenMat, VarType.Texture2D));
        registerBindableSink(new FGFramebufferCopyBindableSink(FGGlobal.S_DEFAULT_FB, null, false, true, true));
    }
}
