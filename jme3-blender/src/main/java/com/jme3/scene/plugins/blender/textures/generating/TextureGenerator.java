/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.scene.plugins.blender.textures.generating;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.ColorBand;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.texture.Image.Format;

/**
 * This class is a base class for texture generators.
 * @author Marcin Roguski (Kaelthas)
 */
public abstract class TextureGenerator {
    protected NoiseGenerator            noiseGenerator;
    protected int                       flag;
    protected float[][]                 colorBand;
    protected BrightnessAndContrastData bacd;
    protected Format                    imageFormat;

    public TextureGenerator(NoiseGenerator noiseGenerator, Format imageFormat) {
        this.noiseGenerator = noiseGenerator;
        this.imageFormat = imageFormat;
    }

    public Format getImageFormat() {
        return imageFormat;
    }

    public void readData(Structure tex, BlenderContext blenderContext) {
        flag = ((Number) tex.getFieldValue("flag")).intValue();
        colorBand = new ColorBand(tex, blenderContext).computeValues();
        bacd = new BrightnessAndContrastData(tex);
        if (colorBand != null) {
            imageFormat = Format.RGBA8;
        }
    }

    public abstract void getPixel(TexturePixel pixel, float x, float y, float z);

    /**
     * This method applies brightness and contrast for RGB textures.
     */
    protected void applyBrightnessAndContrast(BrightnessAndContrastData bacd, TexturePixel texres) {
        texres.red = (texres.red - 0.5f) * bacd.contrast + bacd.brightness;
        if (texres.red < 0.0f) {
            texres.red = 0.0f;
        }
        texres.green = (texres.green - 0.5f) * bacd.contrast + bacd.brightness;
        if (texres.green < 0.0f) {
            texres.green = 0.0f;
        }
        texres.blue = (texres.blue - 0.5f) * bacd.contrast + bacd.brightness;
        if (texres.blue < 0.0f) {
            texres.blue = 0.0f;
        }
    }

    /**
     * This method applies brightness and contrast for Luminance textures.
     * @param texres
     * @param contrast
     * @param brightness
     */
    protected void applyBrightnessAndContrast(TexturePixel texres, float contrast, float brightness) {
        texres.intensity = (texres.intensity - 0.5f) * contrast + brightness;
        if (texres.intensity < 0.0f) {
            texres.intensity = 0.0f;
        } else if (texres.intensity > 1.0f) {
            texres.intensity = 1.0f;
        }
    }

    /**
     * This class contains brightness and contrast data.
     * @author Marcin Roguski (Kaelthas)
     */
    protected static class BrightnessAndContrastData {
        public final float contrast;
        public final float brightness;
        public final float rFactor;
        public final float gFactor;
        public final float bFactor;

        /**
         * Constructor reads the required data from the given structure.
         * @param tex
         *            texture structure
         */
        public BrightnessAndContrastData(Structure tex) {
            contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
            brightness = ((Number) tex.getFieldValue("bright")).floatValue() - 0.5f;
            rFactor = ((Number) tex.getFieldValue("rfac")).floatValue();
            gFactor = ((Number) tex.getFieldValue("gfac")).floatValue();
            bFactor = ((Number) tex.getFieldValue("bfac")).floatValue();
        }
    }
}
