/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.pass;

import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.ui.Picture;

/**
 *
 * @author codex
 */
public abstract class ScreenModule extends AbstractModule {

    protected Picture screenRect;
    
    @Override
    public void initialize(FrameGraph frameGraph) {
        screenRect = new Picture("ScreenRect");
        screenRect.setWidth(1);
        screenRect.setHeight(1);
        screenRect.setIgnoreTransform(true);
        Material mat = createScreenMaterial();
        screenRect.setMaterial(mat);
        RenderState rs = mat.getAdditionalRenderState();
        rs.setDepthWrite(false);
        rs.setDepthTest(false);
    }
    
    protected abstract Material createScreenMaterial();
    
}
