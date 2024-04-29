/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class GBufferPass extends RenderPass {
    
    private ResourceTicket<Texture2D> depthTicket = new ResourceTicket("SceneDepth");
    private ResourceTicket<Texture2D> texTicket1 = ResourceTicket.createUnlinked("GBufferTex1");
    
    @Override
    public void createResources(RenderContext context, ResourceList resources) {
        resources.register(this, new TextureDef2D(1024, 1024, Image.Format.RGBA8), texTicket1);
        resources.reference(depthTicket);
    }
    @Override
    public void execute(RenderContext context) {
        
    }
    
}
