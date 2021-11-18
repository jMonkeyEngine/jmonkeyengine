/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;

/**
 * A color filter used to change the contrast for each color channel independently and the brightness of colors
 * of the scene textures based on a simple transfer function.
 *
 * The transfer function involves setting the brightness of colors from 0 to 1 based on a maxValue and a minValue,
 * then setting the contrast of each color channel independently using a power law function before the textures' colors are processed by the
 * rasterizer.
 *
 * @author pavl_g.
 */
public class ContrastAdjustmentFilter extends Filter {
    //the different channels exponents
    protected float exp_r = 1f;
    protected float exp_g = 1f;
    protected float exp_b = 1f;
    //the minimum value and the maximum value of the brightness parameter
    protected float minBrightness = 0f;
    protected float maxBrightness = 1f;
    protected Material material;

    /**
     * Instantiates a default color contrast filter and default brightness.
     */
    public ContrastAdjustmentFilter() {
        super(ContrastAdjustmentFilter.class.getName());
    }

    /**
     * Instantiates a color contrast filter with a default brightness.
     * @param exp_r the red color exponent.
     * @param exp_b the blue color exponent.
     * @param exp_g the green color exponent.
     */
    public ContrastAdjustmentFilter(float exp_r, float exp_b, float exp_g){
        super(ContrastAdjustmentFilter.class.getName());
        this.exp_r = exp_r;
        this.exp_g = exp_g;
        this.exp_b = exp_b;
    }
    /**
     * Instantiates a color contrast filter by adjusting different parameters.
     * @param exponents the exponents applied to the colors in order, where x = r, y = g, z = b.
     * @param brightness the brightness applied to the textures in order, where x = minBrightness, y = maxBrightness.
     */
    public ContrastAdjustmentFilter(final Vector3f exponents, final Vector2f brightness){
        super(ContrastAdjustmentFilter.class.getName());
        this.exp_r = exponents.x;
        this.exp_b = exponents.y;
        this.exp_g = exponents.z;
        this.minBrightness = brightness.x;
        this.maxBrightness = brightness.y;
    }

    /**
     * Sets the exponents used to adjust the contrast of the color channels.
     * Default values are 1.0f.
     * @param exp_r the red channel exponent.
     * @param exp_b the blue channel exponent.
     * @param exp_g the green channel exponent.
     */
    public void setExponents(float exp_r, float exp_b, float exp_g){
        this.exp_r = exp_r;
        this.exp_b = exp_b;
        this.exp_g = exp_g;

        if(material == null){
            return;
        }
        //different channels exp for different transfer functions
        material.setFloat("exp_r", exp_r);
        material.setFloat("exp_g", exp_g);
        material.setFloat("exp_b", exp_b);
    }

    /**
     * Retrieves the blue channel exponent.
     * @return the blue channel exponent.
     */
    public float getExp_b() {
        return exp_b;
    }

    /**
     * Retrieves the green channel exponent.
     * @return the green channel exponent.
     */
    public float getExp_g() {
        return exp_g;
    }

    /**
     * Retrieves the red channel exponent.
     * @return the red channel exponent.
     */
    public float getExp_r() {
        return exp_r;
    }

    /**
     * Sets the color channels brightness using a maxValue and a minValue based on this equation :
     * color.rgb = (color.rgb - minBrightness) / (maxBrightness - minBrightness)
     * where, increasing the minBrightness value and increasing the difference between maxBrightness and minBrightness would decrease the brightness.
     * decreasing the minBrightness value and increasing the difference between maxBrightness and minBrightness would increase the brightness.
     * @param minValue the minimum brightness value to use, default is 0f.
     * @param maxValue the maximum brightness value to use, default is 1f.
     */
    public void setBrightness(float minValue, float maxValue) {
        this.minBrightness = minValue;
        this.maxBrightness = maxValue;

        if(material == null){
            return;
        }
        //brightness minValue and maxValue
        material.setFloat("minBrightness", minBrightness);
        material.setFloat("maxBrightness", maxBrightness);
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        //validate app
        if(manager == null || renderManager == null || vp == null || w == 0 || h == 0){
            return;
        }
        material = new Material(manager, "Common/MatDefs/Post/ColorContrast.j3md");
        //different channels exp for different transfer functions
        material.setFloat("exp_r", exp_r);
        material.setFloat("exp_g", exp_g);
        material.setFloat("exp_b", exp_b);

        //brightness minValue and maxValue
        material.setFloat("minBrightness", minBrightness);
        material.setFloat("maxBrightness", maxBrightness);
    }

    @Override
    protected Material getMaterial() {
        if(material == null){
            throw new IllegalStateException("Cannot create a color filter from a null reference !");
        }
        return material;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        final InputCapsule inputCapsule = im.getCapsule(this);
        exp_r = inputCapsule.readFloat("exp_r", 1f);
        exp_g = inputCapsule.readFloat("exp_g", 1f);
        exp_b = inputCapsule.readFloat("exp_b", 1f);
        minBrightness = inputCapsule.readFloat("minBrightness", 0f);
        maxBrightness = inputCapsule.readFloat("maxBrightness", 1f);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        final OutputCapsule outputCapsule = ex.getCapsule(this);
        outputCapsule.write(exp_r, "exp_r", 1f);
        outputCapsule.write(exp_g, "exp_g", 1f);
        outputCapsule.write(exp_b, "exp_b", 1f);
        outputCapsule.write(minBrightness, "minBrightness", 0f);
        outputCapsule.write(maxBrightness, "maxBrightness", 1f);
    }
}