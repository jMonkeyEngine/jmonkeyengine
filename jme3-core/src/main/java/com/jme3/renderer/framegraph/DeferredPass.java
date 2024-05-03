/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class DeferredPass extends RenderPass {

    private ResourceTicket<Texture2D> depth, diffuse, specular, emissive, normal, result;
    private ResourceTicket<LightList> lights;
    private Material material;
    private final CameraSize camSize = new CameraSize();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/ShadingCommon/DeferredShading.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        result = register( new TextureDef2D(w, h, Image.Format.RGBA8), result);
        reference(depth, diffuse, specular, emissive, normal, lights);
    }
    @Override
    protected void execute(FGRenderContext context) {
        material.setTexture("Context_InGBuff0", resources.acquire(diffuse));
        material.setTexture("Context_InGBuff1", resources.acquire(specular));
        material.setTexture("Context_InGBuff2", resources.acquire(emissive));
        material.setTexture("Context_InGBuff3", resources.acquire(normal));
        material.setTexture("Context_InGBuff4", resources.acquire(depth));
        resources.acquireColorTargets(frameBuffer, result);
        material.selectTechnique("DeferredPass", context.getRenderManager());
        context.setFrameBuffer(frameBuffer, true, true, true);
        context.renderFullscreen(material);
        frameBuffer.clearColorTargets();
    }
    @Override
    public void reset(FGRenderContext context) {}
    @Override
    public void cleanup(FrameGraph frameGraph) {}
    @Override
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        return new FrameBuffer(context.getWidth(), context.getHeight(), 1);
    }
    
}
