/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.DeferredSinglePassLightingLogic;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class DeferredPass extends RenderPass {

    private ResourceTicket<Texture2D> depth, diffuse, specular, emissive, normal, outColor;
    private ResourceTicket<LightList> lights;
    private Material material;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        depth = addInput("Depth");
        diffuse = addInput("Diffuse");
        specular = addInput("Specular");
        emissive = addInput("Emissive");
        normal = addInput("Normal");
        outColor = addOutput("Color");
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/ShadingCommon/DeferredShading.j3md");
        for (TechniqueDef t : material.getMaterialDef().getTechniqueDefs("DeferredPass")) {
            t.setLogic(new DeferredSinglePassLightingLogic(t));
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        declare(new TextureDef(w, h, Image.Format.RGBA8), outColor);
        reserve(outColor);
        reference(depth, diffuse, specular, emissive, normal);
        referenceOptional(lights);
    }
    @Override
    protected void execute(FGRenderContext context) {
        resources.acquireColorTargets(frameBuffer, outColor);
        context.getRenderer().setFrameBuffer(frameBuffer);
        context.getRenderer().clearBuffers(true, true, true);
        material.setTexture("Context_InGBuff0", resources.acquire(diffuse));
        material.setTexture("Context_InGBuff1", resources.acquire(specular));
        material.setTexture("Context_InGBuff2", resources.acquire(emissive));
        material.setTexture("Context_InGBuff3", resources.acquire(normal));
        material.setTexture("Context_InGBuff4", resources.acquire(depth));
        material.selectTechnique("DeferredPass", context.getRenderManager());
        LightList lightList = resources.acquireOrElse(lights, null);
        context.getRenderer().setDepthRange(0, 1);
        if (lightList != null) {
            context.getScreen().render(context.getRenderManager(), material, lightList);
        } else {
            context.renderFullscreen(material);
        }
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        return new FrameBuffer(context.getWidth(), context.getHeight(), 1);
    }

    public void setDepth(ResourceTicket<Texture2D> depth) {
        this.depth = depth;
    }
    public void setDiffuse(ResourceTicket<Texture2D> diffuse) {
        this.diffuse = diffuse;
    }
    public void setSpecular(ResourceTicket<Texture2D> specular) {
        this.specular = specular;
    }
    public void setEmissive(ResourceTicket<Texture2D> emissive) {
        this.emissive = emissive;
    }
    public void setNormal(ResourceTicket<Texture2D> normal) {
        this.normal = normal;
    }
    public void setLights(ResourceTicket<LightList> lights) {
        this.lights = lights;
    }

    public ResourceTicket<Texture2D> getOutColor() {
        return outColor;
    }
    
}
