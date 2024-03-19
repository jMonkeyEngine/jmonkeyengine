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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.post.Filter;
import com.jme3.post.filters.BloomFilter.GlowMode;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Image;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Adds a glow effect to the scene.
 * <p>
 * Compared to {@link BloomFilter}, this filter produces much higher quality
 * results that feel much more natural.
 * <p>
 * This implementation, unlike BloomFilter, has no brightness threshold,
 * meaning all aspects of the scene glow, although only very bright areas will
 * noticably produce glow. For this reason, this filter should <em>only</em> be used
 * if HDR is also being utilized, otherwise BloomFilter should be preferred.
 * <p>
 * This filter uses the PBR bloom algorithm presented in
 * <a href="https://learnopengl.com/Guest-Articles/2022/Phys.-Based-Bloom">this article</a>.
 * 
 * @author codex
 */
public class PBRBloomFilter extends Filter {
    
    private AssetManager assetManager;
    private RenderManager renderManager;
    private ViewPort viewPort;
    private int width, height;
    private Pass[] downsamplingPasses;
    private Pass[] upsamplingPasses;
    private final Image.Format format = Image.Format.RGBA16F;
    private boolean initialized = false;
    private int numSamplingPasses = 5;
    private float glowFactor = 0.05f;
    
    /**
     * Creates filter with default settings.
     */
    public PBRBloomFilter() {
        super("PBRBloomFilter");
    }
    
    @Override
    protected void initFilter(AssetManager am, RenderManager rm, ViewPort vp, int width, int height) {
        
        assetManager = am;
        renderManager = rm;
        viewPort = vp;
        postRenderPasses = new LinkedList<>();
        Renderer renderer = renderManager.getRenderer();
        int w = this.width = width;
        int h = this.height = height;
        
        downsamplingPasses = new Pass[numSamplingPasses];
        upsamplingPasses = new Pass[numSamplingPasses];
        
        // downsampling passes
        Material downsampleMat = new Material(assetManager, "Common/MatDefs/Post/Downsample.j3md");
        Vector2f initTexelSize = new Vector2f(1f/w, 1f/h);
        w /= 2; h /= 2;
        Pass initialPass = new Pass() {
            @Override
            public boolean requiresSceneAsTexture() {
                return true;
            }
            @Override
            public void beforeRender() {
                downsampleMat.setVector2("TexelSize", initTexelSize);
            }
        };
        initialPass.init(renderer, w, h, format, Image.Format.Depth, 1, downsampleMat);
        postRenderPasses.add(initialPass);
        downsamplingPasses[0] = initialPass;
        for (int i = 1; i < downsamplingPasses.length; i++) {
            Vector2f texelSize = new Vector2f(1f/w, 1f/h);
            w /= 2; h /= 2;
            Pass prev = downsamplingPasses[i-1];
            Pass pass = new Pass() {
                @Override
                public void beforeRender() {
                    downsampleMat.setTexture("Texture", prev.getRenderedTexture());
                    downsampleMat.setVector2("TexelSize", texelSize);
                }
            };
            pass.init(renderer, w, h, format, Image.Format.Depth, 1, downsampleMat);
            postRenderPasses.add(pass);
            downsamplingPasses[i] = pass;
        }
        
        // upsampling passes
        Material upsampleMat = new Material(assetManager, "Common/MatDefs/Post/Upsample.j3md");
        for (int i = 0; i < upsamplingPasses.length; i++) {
            Vector2f texelSize = new Vector2f(1f/w, 1f/h);
            w *= 2; h *= 2;
            Pass prev;
            if (i == 0) {
                prev = downsamplingPasses[downsamplingPasses.length-1];
            } else {
                prev = upsamplingPasses[i-1];
            }
            Pass pass = new Pass() {
                @Override
                public void beforeRender() {
                    upsampleMat.setTexture("Texture", prev.getRenderedTexture());
                    upsampleMat.setVector2("TexelSize", texelSize);
                }
            };
            pass.init(renderer, w, h, format, Image.Format.Depth, 1, upsampleMat);
            postRenderPasses.add(pass);
            upsamplingPasses[i] = pass;
        }
        
        material = new Material(assetManager, "Common/MatDefs/Post/PBRBloomFinal.j3md");
        material.setTexture("GlowMap", upsamplingPasses[upsamplingPasses.length-1].getRenderedTexture());
        material.setFloat("GlowFactor", glowFactor);
        
        initialized = true;
        
    }
    
    @Override
    protected Material getMaterial() {
        return material;
    }
    
    /**
     * Sets the number of sampling passes in each step.
     * <p>
     * Higher values produce more glow with higher resolution, at the cost
     * of more passes. Lower values produce less glow with lower resolution.
     * <p>
     * The total number of passes is {@code 2n+1}: n passes for downsampling
     * (13 texture reads per pass per fragment), n passes for upsampling and blur
     * (9 texture reads per pass per fragment), and 1 pass for blending (2 texture reads
     * per fragment). Though, it should be noted that for each downsampling pass the
     * number of fragments decreases by 75%, and for each upsampling pass, the number
     * of fragments quadruples (which restores the number of fragments to the original
     * resolution).
     * <p>
     * Settings this after the filter has been initialized forces reinitialization.
     * <p>
     * default=5
     * 
     * @param numSamplingPasses number of passes per donwsampling/upsampling step
     */
    public void setNumSamplingPasses(int numSamplingPasses) {
        if (numSamplingPasses <= 0) {
            throw new IllegalArgumentException("Expected number of sampling passes to be greater than zero (found: " + numSamplingPasses + ").");
        }
        this.numSamplingPasses = numSamplingPasses;
        if (initialized) {
            initFilter(assetManager, renderManager, viewPort, width, height);
        }
    }
    
    /**
     * Sets the factor at which the glow result texture is merged with
     * the scene texture.
     * <p>
     * Low values favor the scene texture more, while high values make
     * glow more noticable. This value is clamped between 0 and 1.
     * <p>
     * default=0.05f
     * 
     * @param factor 
     */
    public void setGlowFactor(float factor) {
        this.glowFactor = FastMath.clamp(factor, 0, 1);
        if (material != null) {
            material.setFloat("GlowFactor", glowFactor);
        }
    }
    
    /**
     * Gets the number of downsampling/upsampling passes per step.
     * 
     * @return number of downsampling/upsampling passes
     * @see #setNumSamplingPasses(int)
     */
    public int getNumSamplingPasses() {
        return numSamplingPasses;
    }
    
    /**
     * Gets the glow factor.
     * 
     * @return glow factor
     * @see #setGlowFactor(float)
     */
    public float getGlowFactor() {
        return glowFactor;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(numSamplingPasses, "numSamplingPasses", 5);
        oc.write(glowFactor, "glowFactor", 0.05f);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        numSamplingPasses = ic.readInt("numSamplingPasses", numSamplingPasses);
        glowFactor = ic.readFloat("glowFactor", glowFactor);
    }
    
}
