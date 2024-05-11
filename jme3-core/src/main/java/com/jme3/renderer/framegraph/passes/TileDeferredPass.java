/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.material.TechniqueDef;
import com.jme3.material.logic.TileBasedDeferredSinglePassLightingLogic;
import com.jme3.material.logic.TiledRenderGrid;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 *
 * @author codex
 */
public class TileDeferredPass extends RenderPass {
    
    private ResourceTicket<Texture2D> diffuse, specular, emissive, normal, depth, outColor;
    private ResourceTicket<LightList> lights;
    private ResourceTicket<TiledRenderGrid> tiles;
    private Material material;
    private final TiledRenderGrid tileInfo = new TiledRenderGrid();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        diffuse = addInput("Diffuse");
        specular = addInput("Specular");
        emissive = addInput("Emissive");
        normal = addInput("Normal");
        depth = addInput("Depth");
        outColor = addOutput("Color");
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/ShadingCommon/TileBasedDeferredShading.j3md");
        for (TechniqueDef t : material.getMaterialDef().getTechniqueDefs("TileBasedDeferredPass")) {
            t.setLogic(new TileBasedDeferredSinglePassLightingLogic(t, tileInfo));
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(new TextureDef(context.getWidth(), context.getHeight(), Image.Format.RGBA8), outColor);
        reserve(outColor);
        reference(diffuse, specular, emissive, normal, depth);
        referenceOptional(lights, tiles);
    }
    @Override
    protected void execute(FGRenderContext context) {
        resources.acquireColorTargets(frameBuffer, outColor);
        context.getRenderer().setFrameBuffer(frameBuffer);
        context.getRenderer().clearBuffers(true, true, true);
        TiledRenderGrid trg = resources.acquireOrElse(tiles, null);
        if (trg != null) {
            tileInfo.copyFrom(trg);
        }
        tileInfo.update(context.getViewPort().getCamera());
        material.setTexture("Context_InGBuff0", resources.acquire(diffuse));
        material.setTexture("Context_InGBuff1", resources.acquire(specular));
        material.setTexture("Context_InGBuff2", resources.acquire(emissive));
        material.setTexture("Context_InGBuff3", resources.acquire(normal));
        material.setTexture("Context_InGBuff4", resources.acquire(depth));
        material.selectTechnique("TileBasedDeferredPass", context.getRenderManager());
        LightList lightList = resources.acquireOrElse(lights, null);
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
    public void setDepth(ResourceTicket<Texture2D> depth) {
        this.depth = depth;
    }
    public void setLights(ResourceTicket<LightList> lights) {
        this.lights = lights;
    }
    public void setTiles(ResourceTicket<TiledRenderGrid> tiles) {
        this.tiles = tiles;
    }

    public ResourceTicket<Texture2D> getOutColor() {
        return outColor;
    }
    
}
