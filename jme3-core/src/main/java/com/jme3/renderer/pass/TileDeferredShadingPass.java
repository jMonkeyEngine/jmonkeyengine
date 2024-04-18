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

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.framegraph.FGRenderContext;

public class TileDeferredShadingPass extends DeferredShadingPass{
    
    private static final String MAT_DEF = "Common/MatDefs/ShadingCommon/TileBasedDeferredShading.j3md";
    protected final static String PASS = "TileBasedDeferredPass";

    public TileDeferredShadingPass() {
        super("TileDeferredShadingPass");
    }

    @Override
    protected Material getMaterial() {
        MaterialDef def = (MaterialDef) assetManager.loadAsset(MAT_DEF);
        screenMat = new Material(def);
        return screenMat;
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        DeferredLightDataSink deferredLightDataSink = (DeferredLightDataSink) getSink(S_LIGHT_DATA);
        DeferredLightDataSource.DeferredLightDataProxy deferredLightDataProxy = (DeferredLightDataSource.DeferredLightDataProxy) deferredLightDataSink.getBind();
        LightList lights = deferredLightDataProxy.getLightData();

        // Handle FullScreenLights
        screenMat.selectTechnique(PASS, renderContext.getRenderManager());
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.setBoolean("UseLightsCullMode", false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, lights, renderContext.getRenderManager());
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);

        // Handle non-fullscreen lights
    }

}
