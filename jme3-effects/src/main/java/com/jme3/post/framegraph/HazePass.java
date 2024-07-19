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

import com.jme3.export.InputCapsule;
import com.jme3.export.OutputCapsule;
import com.jme3.export.Savable;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.client.GraphSource;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * Fog that only affects objects within a specified depth range.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): scene color texture.</li>
 *   <li>Depth ({@link Texture2D}): scene depth texture.</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Result ({@link Texture2D}): resulting color texture.</li>
 * </ul>
 * 
 * @author codex
 */
public class HazePass extends RenderPass {
    
    private static final Vector2f DEF_RANGE = new Vector2f(0.5f, 1.0f);
    
    private ResourceTicket<Texture2D> inColor, inDepth, result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material material;
    private GraphSource<ColorRGBA> color;
    private GraphSource<Vector2f> range;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inColor = addInput("Color");
        inDepth = addInput("Depth");
        result = addOutput("Result");
        texDef.setMagFilter(Texture.MagFilter.Bilinear);
        texDef.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        material = new Material(frameGraph.getAssetManager(), "Common/MatDefs/Post/Haze.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        reserve(result);
        reference(inColor, inDepth);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        Texture2D colorTex = resources.acquire(inColor);
        Texture2D depthTex = resources.acquire(inDepth);
        material.setTexture("ColorMap", colorTex);
        material.setTexture("DepthMap", depthTex);
        material.setColor("HazeColor", GraphSource.get(color, ColorRGBA.White, frameGraph, context.getViewPort()));
        material.setVector2("Range", GraphSource.get(range, DEF_RANGE, frameGraph, context.getViewPort()));
        
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
    @Override
    protected void write(OutputCapsule out) throws IOException {
        if (color != null && color instanceof Savable) {
            out.write((Savable)color, "color", null);
        }
    }
    @Override
    protected void read(InputCapsule in) throws IOException {
        color = (GraphSource<ColorRGBA>)in.readSavable("color", null);
    }
    
    /**
     * Sets the color of the haze.
     * <p>
     * The alpha value determines the strength of the haze effect. One being
     * total haze at the maximum range, and zero being no haze at the maximum range.
     * <p>
     * default={@link ColorRGBA#White}
     * 
     * @param color color source (or null to use default)
     */
    public void setColor(GraphSource<ColorRGBA> color) {
        this.color = color;
    }
    /**
     * Sets the depth range in which haze is increasing (exclusive).
     * <p>
     * The x value determines the range's lower boundary, and the y value
     * determines the range's upper boundary. Pixels outside the range
     * are not affected by haze.
     * <p>
     * default={@code (0.5, 1.0)}
     * 
     * @param range range source (or null to use default)
     */
    public void setRange(GraphSource<Vector2f> range) {
        this.range = range;
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
    public GraphSource<Vector2f> getRange() {
        return range;
    }
    
}
