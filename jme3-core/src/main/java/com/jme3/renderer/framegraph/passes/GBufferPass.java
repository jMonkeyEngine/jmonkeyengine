/*
 * Copyright (c) 2024 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.util.LinkedList;
import java.util.function.Function;

/**
 * Renders diffuse, specular, emissive, normal, and depth information to a set of
 * textures.
 * <p>
 * Lights from rendered geometries are accumulated and exported.
 * 
 * @author codex
 */
public class GBufferPass extends RenderPass implements GeometryRenderHandler {
    
    private final static String GBUFFER_PASS = "GBufferPass";
    
    private ResourceTicket<Texture2D> diffuse, specular, emissive, normal, depth;
    private ResourceTicket<LightList> lights;
    private ResourceTicket<Integer> numRendersTicket;
    private ValueDef<LightList> lightDef;
    private ValueDef<Integer> numRendersDef;
    private final TextureDef<Texture2D>[] texDefs = new TextureDef[5];
    private final LinkedList<Light> accumulatedLights = new LinkedList<>();
    private Texture2D diffuseTex;
    private int numRenders = 0;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        diffuse  = addOutput("Diffuse");
        specular = addOutput("Specular");
        emissive = addOutput("Emissive");
        normal   = addOutput("Normal");
        depth    = addOutput("Depth");
        lights   = addOutput("Lights");
        numRendersTicket = addOutput("NumRenders");
        Function<Image, Texture2D> tex = img -> new Texture2D(img);
        texDefs[0] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[1] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[2] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA16F);
        texDefs[3] = new TextureDef<>(Texture2D.class, tex, Image.Format.RGBA32F);
        texDefs[4] = new TextureDef<>(Texture2D.class, tex, Image.Format.Depth);
        int i = -1;
        for (TextureDef<Texture2D> d : texDefs) {
            i++;
            d.setFormatFlexible(true);
            //d.setUseExisting(false);
            //d.setDisposeOnRelease(true);
            //d.setStaticTimeout(0);
        }
        lightDef = new ValueDef(LightList.class, n -> new LightList(null));
        lightDef.setReviser(list -> list.clear());
        numRendersDef = new ValueDef(Boolean.class, n -> true);
        numRendersDef.setUseExisting(false);
        numRendersDef.setDisposeOnRelease(true);
    }
    @Override
    protected void prepare(FGRenderContext context) {
        int w = context.getWidth();
        int h = context.getHeight();
        if (diffuseTex == null || diffuseTex.getImage().getWidth() != w || diffuseTex.getImage().getHeight() != h) {
            diffuseTex = new Texture2D(w, h, Image.Format.RGBA16F);
        }
        for (TextureDef<Texture2D> d : texDefs) {
            d.setSize(w, h);
        }
        declare(texDefs[0], diffuse);
        declare(texDefs[1], specular);
        declare(texDefs[2], emissive);
        declare(texDefs[3], normal);
        declare(texDefs[4], depth);
        declare(lightDef, lights);
        declare(numRendersDef, numRendersTicket);
        reserve(diffuse, specular, emissive, normal, depth, lights);
        numRenders = 0;
    }
    @Override
    protected void execute(FGRenderContext context) {
        // acquire texture targets
        FrameBuffer fb = getFrameBuffer(context, 1);
        fb.setMultiTarget(true);
        //resources.setDirect(diffuse, diffuseTex);
        resources.acquireColorTargets(fb, diffuse, specular, emissive, normal);
        resources.acquireDepthTarget(fb, depth);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        LightList lightList = resources.acquire(lights);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        context.getRenderManager().setForcedTechnique(GBUFFER_PASS);
        context.getRenderManager().setGeometryRenderHandler(this);
        context.renderViewPortQueue(RenderQueue.Bucket.Opaque, true);
        // add accumulated lights
        while (!accumulatedLights.isEmpty()) {
            lightList.add(accumulatedLights.pollFirst());
        }
        resources.setDirect(numRendersTicket, numRenders);
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public boolean renderGeometry(RenderManager rm, Geometry geom) {
        Material material = geom.getMaterial();
        if(material.getMaterialDef().getTechniqueDefs(rm.getForcedTechnique()) == null) {
            return false;
        }
        rm.renderGeometry(geom);
        numRenders++;
        if (material.getActiveTechnique() != null) {
            LightList lts = geom.getFilterWorldLights();
            for (Light l : lts) {
                // todo: checking for containment is very slow
                if (!accumulatedLights.contains(l)) {
                    accumulatedLights.add(l);
                }
            }
            return true;
        }
        return false;
    }
    
}
