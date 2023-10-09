package com.jme3.renderer.renderPass;

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.MaterialDef;
import com.jme3.renderer.framegraph.FGRenderContext;

public class TileDeferredShadingPass extends DeferredShadingPass{
    private static final String _S_TILE_BASED_DEFERRED_SHADING_PASS_MAT_DEF = "Common/MatDefs/ShadingCommon/TileBasedDeferredShading.j3md";
    protected final static String _S_TILE_BASED_DEFERRED_PASS = "TileBasedDeferredPass";

    public TileDeferredShadingPass() {
        super("TileDeferredShadingPass");
    }

    @Override
    protected Material getMaterial() {
        MaterialDef def = (MaterialDef) assetManager.loadAsset(_S_TILE_BASED_DEFERRED_SHADING_PASS_MAT_DEF);
        screenMat = new Material(def);
        return screenMat;
    }

    @Override
    public void executeDrawCommandList(FGRenderContext renderContext) {
        DeferredLightDataSink deferredLightDataSink = (DeferredLightDataSink) getSink(S_LIGHT_DATA);
        DeferredLightDataSource.DeferredLightDataProxy deferredLightDataProxy = (DeferredLightDataSource.DeferredLightDataProxy) deferredLightDataSink.getBind();
        LightList lights = deferredLightDataProxy.getLightData();

        // Handle FullScreenLights
        screenMat.selectTechnique(_S_TILE_BASED_DEFERRED_PASS, renderContext.renderManager);
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.setBoolean("UseLightsCullMode", false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, lights, renderContext.renderManager);
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);

        // Handle non-fullscreen lights
    }

}
