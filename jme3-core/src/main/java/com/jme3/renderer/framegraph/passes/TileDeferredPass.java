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
import com.jme3.texture.Texture2D;

/**
 * Renders GBuffer information using tiled deferred lighting to a color texture.
 * 
 * @author codex
 */
public class TileDeferredPass extends RenderPass {
    
    private ResourceTicket<Texture2D> diffuse, specular, emissive, normal, depth, outColor;
    private ResourceTicket<LightList> lights;
    private ResourceTicket<TiledRenderGrid> tiles;
    private TextureDef<Texture2D> colorDef;
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
        lights = addInput("Lights");
        tiles = addInput("TileInfo");
        colorDef = new TextureDef<>(Texture2D.class, img -> new Texture2D(img));
        colorDef.setFormatFlexible(true);
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/ShadingCommon/TileBasedDeferredShading.j3md");
        material = material.clone();
        for (TechniqueDef t : material.getMaterialDef().getTechniqueDefs("TileBasedDeferredPass")) {
            t.setLogic(new TileBasedDeferredSinglePassLightingLogic(t, tileInfo));
        }
    }
    @Override
    protected void prepare(FGRenderContext context) {
        colorDef.setSize(context.getWidth(), context.getHeight());
        declare(colorDef, outColor);
        reserve(outColor);
        reference(diffuse, specular, emissive, normal, depth);
        referenceOptional(lights, tiles);
    }
    @Override
    protected void execute(FGRenderContext context) {
        FrameBuffer fb = getFrameBuffer(context, 1);
        resources.acquireColorTargets(fb, outColor);
        context.getRenderer().setFrameBuffer(fb);
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
    
}
