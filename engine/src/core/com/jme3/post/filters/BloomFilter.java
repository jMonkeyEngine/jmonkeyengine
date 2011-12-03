/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
import com.jme3.math.ColorRGBA;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Image.Format;
import java.io.IOException;
import java.util.ArrayList;

/**
 * BloomFilter is used to make objects in the scene have a glow effect.<br>
 * There are 2 mode : Scene and Objects.<br>
 * Scene mode extracts the bright parts of the scene to make them glow<br>
 * Object mode make objects glow according to their material's glowMap or their GlowColor<br>
 * @see <a href="http://jmonkeyengine.org/wiki/doku.php/jme3:advanced:bloom_and_glow">advanced:bloom_and_glow</a> for more details
 * 
 * @author Rémy Bouquet aka Nehon
 */
public class BloomFilter extends Filter {

    /**
     * GlowMode specifies if the glow will be applied to the whole scene,or to objects that have aglow color or a glow map
     */
    public enum GlowMode {

        /**
         * Apply bloom filter to bright areas in the scene.
         */
        Scene,
        /**
         * Apply bloom only to objects that have a glow map or a glow color.
         */
        Objects,
        /**
         * Apply bloom to both bright parts of the scene and objects with glow map.
         */
        SceneAndObjects;
    }

    private GlowMode glowMode = GlowMode.Scene;
    //Bloom parameters
    private float blurScale = 1.5f;
    private float exposurePower = 5.0f;
    private float exposureCutOff = 0.0f;
    private float bloomIntensity = 2.0f;
    private float downSamplingFactor = 1;
    private Pass preGlowPass;
    private Pass extractPass;
    private Pass horizontalBlur = new Pass();
    private Pass verticalalBlur = new Pass();
    private Material extractMat;
    private Material vBlurMat;
    private Material hBlurMat;
    private int screenWidth;
    private int screenHeight;    

    /**
     * Creates a Bloom filter
     */
    public BloomFilter() {
        super("BloomFilter");
    }

    /**
     * Creates the bloom filter with the specific glow mode
     * @param glowMode
     */
    public BloomFilter(GlowMode glowMode) {
        this();
        this.glowMode = glowMode;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        screenWidth = (int) Math.max(1, (w / downSamplingFactor));
        screenHeight = (int) Math.max(1, (h / downSamplingFactor));
        //    System.out.println(screenWidth + " " + screenHeight);
        if (glowMode != GlowMode.Scene) {
            preGlowPass = new Pass();
            preGlowPass.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth);
        }

        postRenderPasses = new ArrayList<Pass>();
        //configuring extractPass
        extractMat = new Material(manager, "Common/MatDefs/Post/BloomExtract.j3md");
        extractPass = new Pass() {

            @Override
            public boolean requiresSceneAsTexture() {
                return true;
            }

            @Override
            public void beforeRender() {
                extractMat.setFloat("ExposurePow", exposurePower);
                extractMat.setFloat("ExposureCutoff", exposureCutOff);
                if (glowMode != GlowMode.Scene) {
                    extractMat.setTexture("GlowMap", preGlowPass.getRenderedTexture());
                }
                extractMat.setBoolean("Extract", glowMode != GlowMode.Objects);
            }
        };

        extractPass.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth, 1, extractMat);
        postRenderPasses.add(extractPass);

        //configuring horizontal blur pass
        hBlurMat = new Material(manager, "Common/MatDefs/Blur/HGaussianBlur.j3md");
        horizontalBlur = new Pass() {

            @Override
            public void beforeRender() {
                hBlurMat.setTexture("Texture", extractPass.getRenderedTexture());
                hBlurMat.setFloat("Size", screenWidth);
                hBlurMat.setFloat("Scale", blurScale);
            }
        };

        horizontalBlur.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth, 1, hBlurMat);
        postRenderPasses.add(horizontalBlur);

        //configuring vertical blur pass
        vBlurMat = new Material(manager, "Common/MatDefs/Blur/VGaussianBlur.j3md");
        verticalalBlur = new Pass() {

            @Override
            public void beforeRender() {
                vBlurMat.setTexture("Texture", horizontalBlur.getRenderedTexture());
                vBlurMat.setFloat("Size", screenHeight);
                vBlurMat.setFloat("Scale", blurScale);
            }
        };

        verticalalBlur.init(renderManager.getRenderer(), screenWidth, screenHeight, Format.RGBA8, Format.Depth, 1, vBlurMat);
        postRenderPasses.add(verticalalBlur);


        //final material
        material = new Material(manager, "Common/MatDefs/Post/BloomFinal.j3md");
        material.setTexture("BloomTex", verticalalBlur.getRenderedTexture());
    }


    @Override
    protected Material getMaterial() {
        material.setFloat("BloomIntensity", bloomIntensity);
        return material;
    }

    @Override
    protected void postQueue(RenderManager renderManager, ViewPort viewPort) {
        if (glowMode != GlowMode.Scene) {           
            renderManager.getRenderer().setBackgroundColor(ColorRGBA.BlackNoAlpha);            
            renderManager.getRenderer().setFrameBuffer(preGlowPass.getRenderFrameBuffer());
            renderManager.getRenderer().clearBuffers(true, true, true);
            renderManager.setForcedTechnique("Glow");
            renderManager.renderViewPortQueues(viewPort, false);         
            renderManager.setForcedTechnique(null);
            renderManager.getRenderer().setFrameBuffer(viewPort.getOutputFrameBuffer());
        }
    }

    /**
     * returns the bloom intensity
     * @return 
     */
    public float getBloomIntensity() {
        return bloomIntensity;
    }

    /**
     * intensity of the bloom effect default is 2.0
     * @param bloomIntensity
     */
    public void setBloomIntensity(float bloomIntensity) {
        this.bloomIntensity = bloomIntensity;
    }

    /**
     * returns the blur scale
     * @return 
     */
    public float getBlurScale() {
        return blurScale;
    }

    /**
     * sets The spread of the bloom default is 1.5f
     * @param blurScale
     */
    public void setBlurScale(float blurScale) {
        this.blurScale = blurScale;
    }

    /**
     * returns the exposure cutoff<br>
     * for more details see {@link setExposureCutOff(float exposureCutOff)}
     * @return 
     */    
    public float getExposureCutOff() {
        return exposureCutOff;
    }

    /**
     * Define the color threshold on which the bloom will be applied (0.0 to 1.0)
     * @param exposureCutOff
     */
    public void setExposureCutOff(float exposureCutOff) {
        this.exposureCutOff = exposureCutOff;
    }

    /**
     * returns the exposure power<br>
     * form more details see {@link setExposurePower(float exposurePower)}
     * @return 
     */
    public float getExposurePower() {
        return exposurePower;
    }

    /**
     * defines how many time the bloom extracted color will be multiplied by itself. default id 5.0<br>
     * a high value will reduce rough edges in the bloom and somhow the range of the bloom area     * 
     * @param exposurePower
     */
    public void setExposurePower(float exposurePower) {
        this.exposurePower = exposurePower;
    }

    /**
     * returns the downSampling factor<br>
     * form more details see {@link setDownSamplingFactor(float downSamplingFactor)}
     * @return
     */
    public float getDownSamplingFactor() {
        return downSamplingFactor;
    }

    /**
     * Sets the downSampling factor : the size of the computed texture will be divided by this factor. default is 1 for no downsampling
     * A 2 value is a good way of widening the blur
     * @param downSamplingFactor
     */
    public void setDownSamplingFactor(float downSamplingFactor) {
        this.downSamplingFactor = downSamplingFactor;
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(glowMode, "glowMode", GlowMode.Scene);
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
        glowMode = ic.readEnum("glowMode", GlowMode.class, GlowMode.Scene);
        blurScale = ic.readFloat("blurScale", 1.5f);
        exposurePower = ic.readFloat("exposurePower", 5.0f);
        exposureCutOff = ic.readFloat("exposureCutOff", 0.0f);
        bloomIntensity = ic.readFloat("bloomIntensity", 2.0f);
        downSamplingFactor = ic.readFloat("downSamplingFactor", 1);
    }
}
