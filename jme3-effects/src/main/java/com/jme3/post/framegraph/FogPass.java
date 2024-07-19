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
package com.jme3.post.framegraph;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.client.GraphSource;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

/**
 * Port of {@link com.jme3.post.filters.FogFilter} to a RenderPass.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): scene color texture.</li>
 *   <li>Depth ({@link Texture2D}): scene depth texture.</li>
 *   <li>Fog ({@link Texture2D}): screenspace fog texture to fade into (optional).</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Result ({@link Texture2D}): resulting color texture.</li>
 * </ul>
 * If "Fog" is defined, the fog texture will be used to determine the fog color in screenspace.
 * Otherwise, a solid color from a {@link GraphSource} will be used.
 * 
 * @author codex
 */
public class FogPass extends RenderPass {
    
    private ResourceTicket<Texture2D> colorMap, depthMap, fogMap, result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private GraphSource<ColorRGBA> color;
    private GraphSource<Float> density, distance;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        colorMap = addInput("Color");
        depthMap = addInput("Depth");
        fogMap = addInput("Fog");
        result = addOutput("Result");
        texDef.setMagFilter(Texture.MagFilter.Bilinear);
        texDef.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/SkyFog.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        reference(colorMap, depthMap);
        referenceOptional(fogMap);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        ViewPort vp = context.getViewPort();
        
        Texture2D colorTex = resources.acquire(colorMap);
        Texture2D fogTex = resources.acquireOrElse(fogMap, null);
        material.setTexture("ColorMap", colorTex);
        material.setTexture("DepthMap", resources.acquire(depthMap));
        if (fogTex != null) {
            material.setTexture("FogMap", fogTex);
        } else {
            material.clearParam("FogMap");
            material.setColor("FogColor", GraphSource.get(color, ColorRGBA.White, frameGraph, vp));
        }
        material.setFloat("Density", GraphSource.get(density, .7f, frameGraph, vp));
        material.setFloat("Distance", GraphSource.get(distance, 1000f, frameGraph, vp));
        
        int w = colorTex.getImage().getWidth();
        int h = colorTex.getImage().getHeight();
        texDef.setSize(w, h);
        texDef.setFormat(colorTex.getImage().getFormat());
        
        FrameBuffer fb = getFrameBuffer(w, h, 1);
        resources.acquireColorTarget(fb, result);
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.resizeCamera(w, h, false, false, false);
        
        context.getScreen().render(context.getRenderManager(), material);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    
    /**
     * Sets the fog color used if no fog map is specified.
     * <p>
     * default={@link ColorRGBA#White}
     * 
     * @param color 
     */
    public void setColor(GraphSource<ColorRGBA> color) {
        this.color = color;
    }
    /**
     * Sets the fog density.
     * <p>
     * default={@code 0.7f}
     * 
     * @param density 
     */
    public void setDensity(GraphSource<Float> density) {
        this.density = density;
    }
    /**
     * Sets the distance the fog begins at.
     * <p>
     * default={@code 1000f}
     * 
     * @param distance 
     */
    public void setDistance(GraphSource<Float> distance) {
        this.distance = distance;
    }

    /**
     * 
     * @return 
     */
    public GraphSource<ColorRGBA> getColor() {
        return color;
    }
    /**
     * 
     * @return 
     */
    public GraphSource<Float> getDensity() {
        return density;
    }
    /**
     * 
     * @return 
     */
    public GraphSource<Float> getDistance() {
        return distance;
    }
    
}
