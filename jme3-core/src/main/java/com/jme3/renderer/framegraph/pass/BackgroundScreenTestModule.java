/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.RenderContext;
import com.jme3.texture.Texture2D;
import com.jme3.ui.Picture;

/**
 *
 * @author codex
 */
public class BackgroundScreenTestModule extends AbstractModule {

    private final AssetManager assetManager;
    private Picture screen;
    
    public BackgroundScreenTestModule(AssetManager assetManager) {
        this.assetManager = assetManager;
    }
    
    @Override
    public void initialize(FrameGraph frameGraph) {
    
        screen = new Picture("TestScreen");
        screen.setWidth(1f);
        screen.setHeight(1f);
        
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Screen.j3md");
        mat.setTexture("ColorMap", assetManager.loadTexture("Textures/Terrain/Pond/Pond.jpg"));
        screen.setMaterial(mat);
        
        RenderState rs = screen.getMaterial().getAdditionalRenderState();
        rs.setDepthTest(!true);
        rs.setDepthWrite(!true);
        //rs.setDepthFunc(RenderState.TestFunction.Less);
    
    }
    @Override
    public boolean prepare(RenderContext context) {
        return true;
    }
    @Override
    public void execute(RenderContext context) {
        
        //context.setDepthRange(1, 1);
        screen.updateGeometricState();
        screen.getMaterial().render(screen, screen.getLocalLightList(), context.getRenderManager());
        //context.getRenderer().clearBuffers(false, true, false);
    
    }
    @Override
    public void reset() {}
    
}
