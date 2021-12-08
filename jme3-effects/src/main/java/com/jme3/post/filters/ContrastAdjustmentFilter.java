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
    /**
     * Power-law exponent for the red channel.
     */
    private float redExponent = 1f;
    /**
     * Power-law exponent for the green channel.
     */
    private float greenExponent = 1f;
    /**
     * Power-law exponent for the blue channel.
     */
    private float blueExponent = 1f;
    /**
     * Lower limit of the input range for all color channels:
     * the highest level that the filter normalizes to 0.
     */
    private float lowerLimit = 0f;
    /**
     * Upper limit of the input range for all color channels:
     * the level that the filter normalizes to 1 (before output scaling).
     */
    private float upperLimit = 1f;
    /**
     * Output scale factor for the red channel.
     */
    private float redScale = 1f;
    /**
     * Output scale factor for the green channel.
     */
    private float greenScale = 1f;
    /**
     * Output scale factor for the blue channel.
     */
    private float blueScale = 1f;

    /**
     * Instantiates a contrast-adjustment filter with the default parameters:
     *
     * <p>input range from 0 to 1
     * <p>power-law exponent=1 for all color channels
     * <p>output scale factor=1 for all color channels
     *
     * Such a filter has no effect.
     */
    public ContrastAdjustmentFilter() {
        super("Contrast Adjustment");
    }

    /**
     * Instantiates a contrast-adjustment filter with the default input range and output scaling.
     *
     * @param exponent the desired power-law exponent for all color channels
     */
    public ContrastAdjustmentFilter(float exponent) {
        this();
        this.redExponent = exponent;
        this.greenExponent = exponent;
        this.blueExponent = exponent;
    }

    /**
     * Sets the power-law exponent for each color channel.
     * The default value for each exponent is 1, which produces a linear filter.
     *
     * @param redExponent the desired red-channel exponent
     * @param greenExponent the desired green-channel exponent
     * @param blueExponent the desired blue-channel exponent
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setExponents(float redExponent, float greenExponent, float blueExponent) {
        setRedExponent(redExponent);
        setGreenExponent(greenExponent);
        setBlueExponent(blueExponent);

        return this;
    }

    /**
     * Sets the power-law exponent for the red channel.
     *
     * @param exponent the desired exponent (default=1 for linear)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setRedExponent(float exponent) {
        this.redExponent = exponent;
        if (material != null) {
            material.setFloat("redChannelExponent", redExponent);
        }
        return this;
    }

    /**
     * Sets the power-law exponent for the green channel.
     *
     * @param exponent the desired exponent (default=1 for linear)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setGreenExponent(float exponent) {
        this.greenExponent = exponent;
        if (material != null) {
            material.setFloat("greenChannelExponent", greenExponent);
        }
        return this;
    }

    /**
     * Sets the power-law exponent for the blue channel.
     *
     * @param exponent the desired exponent (default=1 for linear)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setBlueExponent(float exponent) {
        this.blueExponent = exponent;
        if (material != null) {
            material.setFloat("blueChannelExponent", blueExponent);
        }
        return this;
    }

    /**
     * Retrieves the red-channel exponent.
     *
     * @return the power-law exponent (default=1 for linear)
     */
    public float getRedExponent() {
        return redExponent;
    }

    /**
     * Retrieves the green-channel exponent.
     *
     * @return the power-law exponent (default=1 for linear)
     */
    public float getGreenExponent() {
        return greenExponent;
    }

    /**
     * Retrieves the blue-channel exponent.
     *
     * @return the power-law exponent (default=1 for linear)
     */
    public float getBlueExponent() {
        return blueExponent;
    }

    /**
     * Sets the input range for each color channel.
     *
     * <p>Before applying the power law, the input levels get normalized:
     * lowerLimit and below normalize to 0 and upperLimit normalizes to 1.
     *
     * @param lowerLimit the desired lower limit (default=0)
     * @param upperLimit the desired upper limit (default=1)
     * @return this filter instance, for chaining
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
     * @param level the highest input level that should be normalized to 0 (default=0)
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
     *
     * @return the input level (default=0)
     */
    public float getLowerLimit() {
        return lowerLimit;
    }

    /**
     * Returns the upper limit of the input range.
     *
     * @return the input level (default=1)
     */
    public float getUpperLimit() {
        return upperLimit;
    }

    /**
     * Adjusts the output scaling for each color channel.
     * The default for each scale factor is 1, which has no effect.
     *
     * @param redScale the red-channel scale factor
     * @param greenScale the green-channel scale factor
     * @param blueScale the blue-channel scale factor
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setScales(float redScale, float greenScale, float blueScale) {
        setRedScale(redScale);
        setGreenScale(greenScale);
        setBlueScale(blueScale);

        return this;
    }

    /**
     * Sets the output scale factor for the red channel.
     *
     * @param factor the desired scale factor (default=1)
     * @return this filter instance, for chaining
     */
    public ContrastAdjustmentFilter setRedScale(float factor) {
        this.redScale = factor;
        if (material != null) {
            material.setFloat("redChannelScale", redScale);
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
        this.greenScale = factor;
        if (material != null) {
            material.setFloat("greenChannelScale", greenScale);
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
        this.blueScale = factor;
        if (material != null) {
            material.setFloat("blueChannelScale", blueScale);
        }
        return this;
    }

    /**
     * Retrieves the output scale factor for the red channel.
     *
     * @return the scale factor (default=1 for no effect)
     */
    public float getRedScale() {
        return redScale;
    }

    /**
     * Retrieves the output scale factor for the green channel.
     *
     * @return the scale factor (default=1 for no effect)
     */
    public float getGreenScale() {
        return greenScale;
    }

    /**
     * Retrieves the output scale factor for the blue channel.
     *
     * @return the scale factor (default=1 for no effect)
     */
    public float getBlueScale() {
        return blueScale;
    }

    /**
     * Initializes the Filter when it is added to a FilterPostProcessor.
     *
     * @param assetManager for loading assets (not null)
     * @param renderManager unused
     * @param viewPort unused
     * @param width unused
     * @param height unused
     */
    @Override
    protected void initFilter(AssetManager assetManager, RenderManager renderManager,
            ViewPort viewPort, int width, int height) {
        material = new Material(assetManager, "Common/MatDefs/Post/ContrastAdjustment.j3md");

        //different channels exp for different transfer functions
        setExponents(redExponent, greenExponent, blueExponent);

        //input range
        setInputRange(lowerLimit, upperLimit);

        //final pass scales
        setScales(redScale, greenScale, blueScale);
    }

    /**
     * Returns the Material used in this Filter. This method is invoked on every frame.
     *
     * @return the pre-existing instance, or null if the Filter hasn't been initialized
     */
    @Override
    protected Material getMaterial() {
        return material;
    }

    /**
     * De-serializes this filter, for example when loading from a J3O file.
     *
     * @param im the importer to use (not null)
     * @throws IOException from the importer
     */
    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        final InputCapsule inputCapsule = im.getCapsule(this);
        redExponent = inputCapsule.readFloat("redExponent", 1f);
        greenExponent = inputCapsule.readFloat("greenExponent", 1f);
        blueExponent = inputCapsule.readFloat("blueExponent", 1f);
        lowerLimit = inputCapsule.readFloat("lowerLimit", 0f);
        upperLimit = inputCapsule.readFloat("upperLimit", 1f);
        redScale = inputCapsule.readFloat("redScale", 1f);
        greenScale = inputCapsule.readFloat("greenScale", 1f);
        blueScale = inputCapsule.readFloat("blueScale", 1f);
    }

    /**
     * Serializes this filter, for example when saving to a J3O file.
     *
     * @param ex the exporter to use (not null)
     * @throws IOException from the exporter
     */
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        final OutputCapsule outputCapsule = ex.getCapsule(this);
        outputCapsule.write(redExponent, "redExponent", 1f);
        outputCapsule.write(greenExponent, "greenExponent", 1f);
        outputCapsule.write(blueExponent, "blueExponent", 1f);
        outputCapsule.write(lowerLimit, "lowerLimit", 0f);
        outputCapsule.write(upperLimit, "upperLimit", 1f);
        outputCapsule.write(redScale, "redScale", 1f);
        outputCapsule.write(greenScale, "greenScale", 1f);
        outputCapsule.write(blueScale, "blueScale", 1f);
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
                redExponent, greenExponent, blueExponent,
                redScale, greenScale, blueScale);
        return result;
    }
}
