/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.pass;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.Camera;
import com.jme3.renderer.framegraph.RenderContext;

/**
 *
 * @author codex
 */
public class TileDeferredShadingModule extends DeferredShadingModule {
    
    private static final String MATDEF = "Common/MatDefs/ShadingCommon/TileBasedDeferredShading.j3md";
    protected final static String PASS = "TileBasedDeferredPass";
    
    public TileDeferredShadingModule(AssetManager assetManager) {
        super(assetManager);
    }
    
    @Override
    protected Material createMaterial() {
        return new Material(assetManager, MATDEF);
    }
    
    @Override
    public void executeDrawCommands(RenderContext context) {
        
        // Handle FullScreenLights
        screenMat.selectTechnique(PASS, context.getRenderManager());
        boolean depthWrite = screenMat.getAdditionalRenderState().isDepthWrite();
        boolean depthTest = screenMat.getAdditionalRenderState().isDepthTest();
        screenMat.getAdditionalRenderState().setDepthTest(false);
        screenMat.getAdditionalRenderState().setDepthWrite(false);
        screenMat.setBoolean("UseLightsCullMode", false);
        screenRect.updateGeometricState();
        screenMat.render(screenRect, lightList.produce(), context.getRenderManager());
        screenMat.getAdditionalRenderState().setDepthTest(depthTest);
        screenMat.getAdditionalRenderState().setDepthWrite(depthWrite);

        // Handle non-fullscreen lights
    }
    
    @Override
    public void prepare(RenderContext context) {
        super.prepare(context);
        context.getRenderManager().calculateTileInfo();
    }
    
}
