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
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import java.io.IOException;

/**
 * A filter to adjust the colors of a rendered scene by normalizing each color channel to a specified range,
 * applying a power law, and scaling the output. The alpha channel is unaffected.
 *
 * @author pavl_g.
 */
public class ContrastAdjustmentFilter extends Filter {

    protected float redChannelExponent = 1f;
    protected float greenChannelExponent = 1f;
    protected float blueChannelExponent = 1f;
    /**
     * Lower limit of the input range for all color channels: a level that the filter normalizes to 0.
     */
    protected float lowerLimit = 0f;
    /**
     * Upper limit of the input range for all color channels: the level that the filter normalizes to 1
     * (before output scaling).
     */
    protected float upperLimit = 1f;
    //the final pass scale factor
    protected float redChannelScale = 1f;
    protected float greenChannelScale = 1f;
    protected float blueChannelScale = 1f;

    /**
     * Instantiates a default color contrast filter, default input range and default scale.
     * Default values :
     * - Exponents = 1.0f on all channels.
     * - Input Range Lower Limit = 0f.
     * - Input Range Upper Limit = 1f.
     * - Scale = 1.0f on all channels.
     */
    public ContrastAdjustmentFilter() {
        super("Contrast Adjustment");
    }

    /**
     * Instantiates a color contrast filter with a specific exponent, default scale and default input range.
     *
     * @param exponent an exponent to apply on all channels.
     */
    public ContrastAdjustmentFilter(float exponent) {
        this();
        this.redChannelExponent = exponent;
        this.greenChannelExponent = exponent;
        this.blueChannelExponent = exponent;
    }


    /**
     * Sets the exponents used to adjust the contrast of the color channels.
     * Default values are 1f.
     *
     * @param redChannelExponent   the red channel exponent.
     * @param greenChannelExponent the green channel exponent.
     * @param blueChannelExponent  the blue channel exponent.
     * @return this filter instance for a chain call.
     */
    public ContrastAdjustmentFilter setExponents(float redChannelExponent, float greenChannelExponent,
            float blueChannelExponent) {
        setRedExponent(redChannelExponent);
        setGreenExponent(greenChannelExponent);
        setBlueExponent(blueChannelExponent);

        return this;
    }

    /**
     * Sets the power-law exponent for the red channel.
     *
     * @param exponent the desired exponent (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setRedExponent(float exponent) {
        this.redChannelExponent = exponent;
        if (material != null) {
            material.setFloat("redChannelExponent", redChannelExponent);
        }
        return this;
    }

    /**
     * Sets the power-law exponent for the green channel.
     *
     * @param exponent the desired exponent (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setGreenExponent(float exponent) {
        this.greenChannelExponent = exponent;
        if (material != null) {
            material.setFloat("greenChannelExponent", greenChannelExponent);
        }
        return this;
    }

    /**
     * Sets the power-law exponent for the blue channel.
     *
     * @param exponent the desired exponent (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setBlueExponent(float exponent) {
        this.blueChannelExponent = exponent;
        if (material != null) {
            material.setFloat("blueChannelExponent", blueChannelExponent);
        }
        return this;
    }

    /**
     * Retrieves the red channel exponent.
     * Default value = 1.0f
     *
     * @return the red channel exponent.
     */
    public float getRedChannelExponent() {
        return redChannelExponent;
    }

    /**
     * Retrieves the green channel exponent.
     * Default value = 1.0f.
     *
     * @return the green channel exponent.
     */
    public float getGreenChannelExponent() {
        return greenChannelExponent;
    }

    /**
     * Retrieves the blue channel exponent.
     * Default value = 1.0f
     *
     * @return the blue channel exponent.
     */
    public float getBlueChannelExponent() {
        return blueChannelExponent;
    }

    /**
     * Sets the input range for all color channels. Before applying the power law, the input levels get
     * normalized so that lowerLimit becomes 0 and upperLimit becomes 1.
     *
     * @param lowerLimit the desired lower limit (default=0)
     * @param upperLimit the desired upper limit (default=1)
     * @return this filter instance for a chain call.
     */
    public ContrastAdjustmentFilter setInputRange(float lowerLimit, float upperLimit) {
        setLowerLimit(lowerLimit);
        setUpperLimit(upperLimit);
        return this;
    }

    /**
     * Sets the upper limit of the input range.
     *
     * @param level the input level that should be normalized to 1 (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setUpperLimit(float level) {
        this.upperLimit = level;
        if (material != null) {
            material.setFloat("upperLimit", upperLimit);
        }
        return this;
    }

    /**
     * Sets the lower limit of the input range.
     *
     * @param level the input level that should be normalized to 0 (default=0)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setLowerLimit(float level) {
        this.lowerLimit = level;
        if (material != null) {
            material.setFloat("lowerLimit", lowerLimit);
        }
        return this;
    }

    /**
     * Returns the lower limit of the input range.
     * Default value = 0.0.
     *
     * @return the lower limit
     */
    public float getInputRangeLowerLimit() {
        return lowerLimit;
    }

    /**
     * Returns the upper limit of the input range.
     * Default value = 1.0.
     *
     * @return the upper limit
     */
    public float getInputRangeUpperLimit() {
        return upperLimit;
    }

    /**
     * Adjusts the scales of different channels.
     * Default values = 1.0.
     *
     * @param redChannelScale   the red channel scale.
     * @param greenChannelScale the green channel scale.
     * @param blueChannelScale  the blue channel scale.
     * @return this filter instance for a chain call.
     */
    public ContrastAdjustmentFilter setScales(float redChannelScale, float greenChannelScale,
            float blueChannelScale) {
        setRedScale(redChannelScale);
        setGreenScale(greenChannelScale);
        setBlueScale(blueChannelScale);

        return this;
    }

    /**
     * Sets the output scale factor for the red channel.
     *
     * @param factor the desired scale factor (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setRedScale(float factor) {
        this.redChannelScale = factor;
        if (material != null) {
            material.setFloat("redChannelScale", redChannelScale);
        }
        return this;
    }

    /**
     * Sets the output scale factor for the green channel.
     *
     * @param factor the desired scale factor (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setGreenScale(float factor) {
        this.greenChannelScale = factor;
        if (material != null) {
            material.setFloat("greenChannelScale", greenChannelScale);
        }
        return this;
    }

    /**
     * Sets the output scale factor for the blue channel.
     *
     * @param factor the desired scale factor (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setBlueScale(float factor) {
        this.blueChannelScale = factor;
        if (material != null) {
            material.setFloat("blueChannelScale", blueChannelScale);
        }
        return this;
    }

    /**
     * Retrieves the value of the red channel scale that's applied on the final pass.
     * Default value = 1.0.
     *
     * @return the scale of the red channel.
     */
    public float getRedChannelScale() {
        return redChannelScale;
    }

    /**
     * Retrieves the value of the green channel scale that's applied on the final pass.
     * Default value = 1.0.
     *
     * @return the scale of the green channel.
     */
    public float getGreenChannelScale() {
        return greenChannelScale;
    }

    /**
     * Retrieves the value of the blue channel scale that's applied on the final pass.
     * Default value = 1.0.
     *
     * @return the scale of the blue channel.
     */
    public float getBlueChannelScale() {
        return blueChannelScale;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        //validate app
        if (manager == null || renderManager == null || vp == null || w == 0 || h == 0) {
            return;
        }
        material = new Material(manager, "Common/MatDefs/Post/ColorContrast.j3md");

        //different channels exp for different transfer functions
        setExponents(redChannelExponent, greenChannelExponent, blueChannelExponent);

        //input range
        setInputRange(lowerLimit, upperLimit);

        //final pass scales
        setScales(redChannelScale, greenChannelScale, blueChannelScale);
    }

    @Override
    protected Material getMaterial() {
        return material;
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        final InputCapsule inputCapsule = im.getCapsule(this);
        redChannelExponent = inputCapsule.readFloat("redChannelExponent", 1f);
        greenChannelExponent = inputCapsule.readFloat("greenChannelExponent", 1f);
        blueChannelExponent = inputCapsule.readFloat("blueChannelExponent", 1f);
        lowerLimit = inputCapsule.readFloat("lowerLimit", 0f);
        upperLimit = inputCapsule.readFloat("upperLimit", 1f);
        redChannelScale = inputCapsule.readFloat("redChannelScale", 1f);
        greenChannelScale = inputCapsule.readFloat("greenChannelScale", 1f);
        blueChannelScale = inputCapsule.readFloat("blueChannelScale", 1f);
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        final OutputCapsule outputCapsule = ex.getCapsule(this);
        outputCapsule.write(redChannelExponent, "redChannelExponent", 1f);
        outputCapsule.write(greenChannelExponent, "greenChannelExponent", 1f);
        outputCapsule.write(blueChannelExponent, "blueChannelExponent", 1f);
        outputCapsule.write(lowerLimit, "lowerLimit", 0f);
        outputCapsule.write(upperLimit, "upperLimit", 1f);
        outputCapsule.write(redChannelScale, "redChannelScale", 1f);
        outputCapsule.write(greenChannelScale, "greenChannelScale", 1f);
        outputCapsule.write(blueChannelScale, "blueChannelScale", 1f);
    }

    /**
     * Represent this Filter as a String.
     *
     * @return a descriptive string of text (not null)
     */
    @Override
    public String toString() {
        String result = String.format(
                "input(%.3f, %.3f) exp(%.3f, %.3f, %.3f) scale(%.3f, %.3f, %.3f)",
                lowerLimit, upperLimit,
                redChannelExponent, greenChannelExponent, blueChannelExponent,
                redChannelScale, greenChannelScale, blueChannelScale);
        return result;
    }
}
