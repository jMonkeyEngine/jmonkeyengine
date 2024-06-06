/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.light.LightList;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.light.LightImagePacker;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class LightImagePass extends RenderPass {
    
    private LightImagePacker packer = new LightImagePacker();
    private ResourceTicket<LightList> lights;
    private ResourceTicket<Texture2D>[] textures = new ResourceTicket[3];
    private TextureDef<Texture2D> texDef;
    private int maxLights = 500;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        lights = addInput("Lights");
        textures[0] = addOutput("Texture1");
        textures[1] = addOutput("Texture2");
        textures[2] = addOutput("Texture3");
        texDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
        texDef.setFormat(Image.Format.RGBA32F);
        texDef.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        texDef.setMagFilter(Texture.MagFilter.Nearest);
        texDef.setWrap(Texture.WrapMode.EdgeClamp);
        texDef.setNumPixels(maxLights, true, true, false);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        for (ResourceTicket<Texture2D> t : textures) {
            declare(texDef, t);
            reserve(t);
        }
        reference(lights);
    }
    @Override
    protected void execute(FGRenderContext context) {
        packer.setTextures(
            resources.acquire(textures[0]),
            resources.acquire(textures[1]),
            resources.acquire(textures[2]));
        packer.packLightsToTextures(resources.acquire(lights));
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
}
