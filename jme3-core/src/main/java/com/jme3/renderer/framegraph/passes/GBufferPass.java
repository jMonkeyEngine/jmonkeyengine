/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph.passes;

import com.jme3.renderer.framegraph.definitions.ValueDef;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.util.LinkedList;
import java.util.function.Function;

/**
 *
 * @author codex
 */
public class GBufferPass extends RenderPass implements GeometryRenderHandler {
    
    private final static String GBUFFER_PASS = "GBufferPass";
    
    private ResourceTicket<Texture2D> diffuse, specular, emissive, normal, depth;
    private ResourceTicket<LightList> lights;
    private TextureDef<Texture2D>[] texDefs = new TextureDef[5];
    private ValueDef<LightList> lightDef ;
    private final LinkedList<Light> accumulatedLights = new LinkedList<>();
    private final ColorRGBA mask = new ColorRGBA();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        diffuse  = addOutput("Diffuse");
        specular = addOutput("Specular");
        emissive = addOutput("Emissive");
        normal   = addOutput("normal");
        depth    = addOutput("Depth");
        Function<Image, Texture2D> tex = img -> new Texture2D(img);
        texDefs[0] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[1] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[2] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[3] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA32F);
        texDefs[4] = new TextureDef<>(Texture2D.class, tex, Image.Format.Depth);
        for (TextureDef<Texture2D> d : texDefs) {
            d.setFormatFlexible(true);
        }
        lightDef = new ValueDef(LightList.class, n -> new LightList(null));
        lightDef.setReviser(list -> list.clear());
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        for (TextureDef<Texture2D> d : texDefs) {
            d.setSize(w, h);
        }
        declare(texDefs[0], diffuse);
        declare(texDefs[1], specular);
        declare(texDefs[2], emissive);
        declare(texDefs[3], normal);
        declare(texDefs[4], depth);
        declare(lightDef, lights);
        reserve(diffuse, specular, emissive, normal, depth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        // acquire texture targets
        resources.acquireColorTargets(frameBuffer, diffuse, specular, emissive, normal);
        resources.acquireDepthTarget(frameBuffer, depth);
        context.getRenderer().setFrameBuffer(frameBuffer);
        context.getRenderer().clearBuffers(true, true, true);
        LightList lightList = resources.acquire(lights);
        // render to gBuffer
        context.getRenderer().setBackgroundColor(mask.set(context.getViewPort().getBackgroundColor()).setAlpha(0));
        context.getRenderManager().setForcedTechnique(GBUFFER_PASS);
        context.getRenderManager().setGeometryRenderHandler(this);
        context.getRenderer().setDepthRange(0, 1);
        context.renderViewPortQueue(RenderQueue.Bucket.Opaque, true);
        // add accumulated lights
        while (!accumulatedLights.isEmpty()) {
            lightList.add(accumulatedLights.pollFirst());
        }
        frameBuffer.clearColorTargets();
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    protected FrameBuffer createFrameBuffer(FGRenderContext context) {
        FrameBuffer buffer = new FrameBuffer(context.getWidth(), context.getHeight(), 1);
        buffer.setMultiTarget(true);
        return buffer;
    }
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if(material.getMaterialDef().getTechniqueDefs(rm.getForcedTechnique()) == null) {
            return false;
        }
        rm.renderGeometry(geom);
        if (material.getActiveTechnique() != null) {
            if (material.getMaterialDef().getTechniqueDefs(GBUFFER_PASS) != null) {
                LightList lts = geom.getFilterWorldLights();
                for (Light l : lts) {
                    // todo: checking for containment is very slow
                    if (!accumulatedLights.contains(l)) {
                        accumulatedLights.add(l);
                    }
                }
                return true;
            }
        }
        return false;
    }

    public ResourceTicket<Texture2D> getDepth() {
        return depth;
    }
    public ResourceTicket<Texture2D> getDiffuse() {
        return diffuse;
    }
    public ResourceTicket<Texture2D> getSpecular() {
        return specular;
    }
    public ResourceTicket<Texture2D> getEmissive() {
        return emissive;
    }
    public ResourceTicket<Texture2D> getNormal() {
        return normal;
    }
    public ResourceTicket<LightList> getLights() {
        return lights;
    }
    
}
