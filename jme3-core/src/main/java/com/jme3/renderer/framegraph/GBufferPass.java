/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.renderer.framegraph;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.GeometryRenderHandler;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import java.util.LinkedList;

/**
 *
 * @author codex
 */
public class GBufferPass extends RenderPass implements GeometryRenderHandler {
    
    private final static String GBUFFER_PASS = "GBufferPass";
    
    private ResourceTicket<Texture2D> depth, diffuse, specular, emissive, normal;
    private ResourceTicket<LightList> lights;
    private final LinkedList<Light> accumulatedLights = new LinkedList<>();
    private final ColorRGBA mask = new ColorRGBA();
    
    @Override
    protected void initialize(FrameGraph frameGraph) {}
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        depth = register(new TextureDef2D(w, h, Image.Format.Depth), depth).setName("depth");
        diffuse = register(new TextureDef2D(w, h, Image.Format.RGBA16F), diffuse).setName("diffuse");
        specular = register(new TextureDef2D(w, h, Image.Format.RGBA16F), specular).setName("specular");
        emissive = register(new TextureDef2D(w, h, Image.Format.RGBA16F), emissive).setName("emissive");
        normal = register(new TextureDef2D(w, h, Image.Format.RGBA32F), normal).setName("normal");
        lights = register(ValueDef.create(n -> new LightList(null)), lights).setName("lights");
    }
    @Override
    protected void execute(FGRenderContext context) {
        // acquire and attach texture targets
        frameBuffer.setDepthTarget(context.createTextureTarget(resources.acquire(depth)));
        resources.acquireColorTargets(frameBuffer, diffuse, specular, emissive, normal);
        LightList lightList = resources.acquire(lights);
        // render to gBuffer
        context.setFrameBuffer(frameBuffer, true, true, true);
        context.getRenderer().setBackgroundColor(mask.set(context.getViewPort().getBackgroundColor()).setAlpha(0));
        context.getRenderManager().setForcedTechnique(GBUFFER_PASS);
        context.getRenderManager().setGeometryRenderHandler(this);
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

    public void setDepth(ResourceTicket<Texture2D> depth) {
        this.depth = depth.copyIndexTo(this.depth);
    }
    
}
