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

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.framegraph.FGRenderContext;
import com.jme3.renderer.framegraph.FrameGraph;
import com.jme3.renderer.framegraph.GeometryQueue;
import com.jme3.renderer.framegraph.ResourceTicket;
import com.jme3.renderer.framegraph.definitions.TextureDef;
import com.jme3.renderer.framegraph.passes.RenderPass;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * Port of {@link com.jme3.post.filters.BloomFilter} as a RenderPass.
 * <p>
 * Inputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): color texture to apply bloom effect to (optional).</li>
 *   <li>Objects ({@link GeometryQueue}): specific geometries to apply bloom effect to (optional).</li>
 * </ul>
 * Outputs:
 * <ul>
 *   <li>Color ({@link Texture2D}): resulting color texture.</li>
 * </ul>
 * If "Objects" is undefined, the texture from "Color" (input) will be used. Otherwise, the
 * queue from "Objects" will be rendered with the Glow technique and the result used.
 * 
 * @author codex
 */
public class BloomPass extends RenderPass {
    
    private ResourceTicket<Texture2D> inColor;
    private ResourceTicket<GeometryQueue> objects;
    private ResourceTicket<Texture2D> midTex;
    private ResourceTicket<Texture2D> result;
    private final TextureDef<Texture2D> texDef = TextureDef.texture2D();
    private Material extractMat, hBlurMat, vBlurMat, outMat;
    private float blurScale = 1.5f;
    private float exposurePower = 5.0f;
    private float exposureCutOff = 0.0f;
    private float bloomIntensity = 2.0f;
    private float downSamplingFactor = 1;
    
    @Override
    protected void initialize(FrameGraph frameGraph) {
        inColor = addInput("Color");
        objects = addInput("Objects");
        result = addOutput("Color");
        AssetManager assets = frameGraph.getAssetManager();
        extractMat = new Material(assets, "Common/MatDefs/Post/BloomExtract.j3md");
        hBlurMat = new Material(assets, "Common/MatDefs/Blur/HGaussianBlur.j3md");
        vBlurMat = new Material(assets, "Common/MatDefs/Blur/VGaussianBlur.j3md");
        outMat = new Material(assets, "Common/MatDefs/Post/BloomFinal.j3md");
    }
    @Override
    protected void prepare(FGRenderContext context) {
        declare(texDef, result);
        declare(texDef, midTex);
        reserve(result, midTex);
        referenceOptional(inColor, objects);
    }
    @Override
    protected void execute(FGRenderContext context) {
        
        // configure definitions
        int w = (int)Math.max(1f, context.getWidth()/downSamplingFactor);
        int h = (int)Math.max(1f, context.getHeight()/downSamplingFactor);
        texDef.setSize(w, h);
        
        // get framebuffers and resources
        FrameBuffer outFb = getFrameBuffer(w, h, 1);
        Texture2D outTarget = resources.acquireColorTarget(outFb, result);
        FrameBuffer midFb = getFrameBuffer("mid", w, h, 1);
        Texture2D midTarget = resources.acquireColorTarget(midFb, midTex);
        GeometryQueue geometry = resources.acquireOrElse(objects, null);
        Texture2D scene = resources.acquireOrElse(inColor, null);
        context.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);
        
        // geometry render
        if (geometry != null) {
            context.getRenderer().setFrameBuffer(outFb);
            context.getRenderer().clearBuffers(true, true, true);
            context.getRenderManager().setForcedTechnique("Glow");
            context.renderGeometry(geometry, null, null);
            context.getRenderManager().setForcedTechnique(null);
            extractMat.setTexture("GlowMap", outTarget);
        } else {
            extractMat.clearParam("GlowMap");
        }
        
        // extraction
        extractMat.setFloat("ExposurePow", exposurePower);
        extractMat.setFloat("ExposureCutoff", exposureCutOff);
        extractMat.setBoolean("Extract", scene != null);
        if (scene != null) {
            extractMat.setTexture("Texture", scene);
        } else {
            extractMat.clearParam("Texture");
        }
        render(context, midFb, extractMat);
        
        // horizontal blur
        hBlurMat.setTexture("Texture", midTarget);
        hBlurMat.setFloat("Size", w);
        hBlurMat.setFloat("Scale", blurScale);
        render(context, outFb, hBlurMat);
        
        // vertical blur
        vBlurMat.setTexture("Texture", outTarget);
        vBlurMat.setFloat("Size", h);
        vBlurMat.setFloat("Scale", blurScale);
        render(context, midFb, vBlurMat);
        
        // final output
        outMat.setTexture("Texture", scene);
        outMat.setTexture("BloomTex", midTarget);
        render(context, outFb, outMat);
        
        // manual release required for unregistered tickets
        resources.release(midTex);
        
    }
    @Override
    protected void reset(FGRenderContext context) {}
    @Override
    protected void cleanup(FrameGraph frameGraph) {}
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(blurScale, "blurScale", 1.5f);
        oc.write(exposurePower, "exposurePower", 5.0f);
        oc.write(exposureCutOff, "exposureCutOff", 0.0f);
        oc.write(bloomIntensity, "bloomIntensity", 2.0f);
        oc.write(downSamplingFactor, "downSamplingFactor", 1);
    }
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        blurScale = ic.readFloat("blurScale", 1.5f);
        exposurePower = ic.readFloat("exposurePower", 5.0f);
        exposureCutOff = ic.readFloat("exposureCutOff", 0.0f);
        bloomIntensity = ic.readFloat("bloomIntensity", 2.0f);
        downSamplingFactor = ic.readFloat("downSamplingFactor", 1);
    }
    
    private void render(FGRenderContext context, FrameBuffer fb, Material mat) {
        context.getRenderer().setFrameBuffer(fb);
        context.getRenderer().clearBuffers(true, true, true);
        context.renderFullscreen(mat);
    }
    
    /**
     * returns the bloom intensity
     * @return the intensity value
     */
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /**
     * intensity of the bloom effect default is 2.0
     *
     * @param bloomIntensity the desired intensity (default=2)
     */
    public void setBloomIntensity(float bloomIntensity) {
        this.bloomIntensity = bloomIntensity;
    }

    /**
     * returns the blur scale
     * @return the blur scale
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * sets The spread of the bloom default is 1.5f
     *
     * @param blurScale the desired scale (default=1.5)
     */
    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
    }

    /**
     * returns the exposure cutoff<br>
     * for more details see {@link #setExposureCutOff(float exposureCutOff)}
     * @return the exposure cutoff
     */    
    public float getExposureCutOff() {
        return exposureCutOff;
    }

    /**
     * Define the color threshold on which the bloom will be applied (0.0 to 1.0)
     *
     * @param exposureCutOff the desired threshold (&ge;0, &le;1, default=0)
     */
    public void setExposureCutOff(float exposureCutOff) {
        this.exposureCutOff = exposureCutOff;
    }

    /**
     * returns the exposure power<br>
     * for more details see {@link #setExposurePower(float exposurePower)}
     * @return the exposure power
     */
    public float getExposurePower() {
        return exposurePower;
    }

    /**
     * defines how many times the bloom extracted color will be multiplied by itself. default is 5.0<br>
     * a high value will reduce rough edges in the bloom and somehow the range of the bloom area
     *
     * @param exposurePower the desired exponent (default=5)
     */
    public void setExposurePower(float exposurePower) {
        this.exposurePower = exposurePower;
    }

    /**
     * returns the downSampling factor<br>
     * for more details see {@link #setDownSamplingFactor(float downSamplingFactor)}
     * @return the downsampling factor
     */
    public float getDownSamplingFactor() {
        return downSamplingFactor;
    }

    /**
     * Sets the downSampling factor : the size of the computed texture will be divided by this factor. default is 1 for no downsampling
     * A 2 value is a good way of widening the blur
     *
     * @param downSamplingFactor the desired factor (default=1)
     */
    public void setDownSamplingFactor(float downSamplingFactor) {
        this.downSamplingFactor = downSamplingFactor;
    }
    
}
