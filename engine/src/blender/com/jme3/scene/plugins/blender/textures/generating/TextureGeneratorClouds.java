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

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.scene.plugins.blender.textures.generating.NoiseGenerator.NoiseFunction;
import com.jme3.texture.Image.Format;

/**
 * This class generates the 'clouds' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorClouds extends TextureGenerator {
    // noiseType
    protected static final int TEX_NOISESOFT = 0;
    protected static final int TEX_NOISEPERL = 1;

    // sType
    protected static final int TEX_DEFAULT   = 0;
    protected static final int TEX_COLOR     = 1;

    protected float            noisesize;
    protected int              noiseDepth;
    protected int              noiseBasis;
    protected NoiseFunction    noiseFunction;
    protected int              noiseType;
    protected boolean          isHard;
    protected int              sType;

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorClouds(NoiseGenerator noiseGenerator) {
        super(noiseGenerator, Format.Luminance8);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
        noiseDepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
        noiseBasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
        noiseType = ((Number) tex.getFieldValue("noisetype")).intValue();
        isHard = noiseType != TEX_NOISESOFT;
        sType = ((Number) tex.getFieldValue("stype")).intValue();
        if (sType == TEX_COLOR) {
            imageFormat = Format.RGBA8;
        }

        noiseFunction = NoiseGenerator.noiseFunctions.get(noiseBasis);
        if (noiseFunction == null) {
            noiseFunction = NoiseGenerator.noiseFunctions.get(0);
            noiseBasis = 0;
        }
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        if (noiseBasis == 0) {
            ++x;
            ++y;
            ++z;
        }
        pixel.intensity = NoiseGenerator.NoiseFunctions.turbulence(x, y, z, noisesize, noiseDepth, noiseFunction, isHard);
        if (colorBand != null) {
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];
            pixel.alpha = colorBand[colorbandIndex][3];

            this.applyBrightnessAndContrast(bacd, pixel);
        } else if (sType == TEX_COLOR) {
            pixel.red = pixel.intensity;
            pixel.green = NoiseGenerator.NoiseFunctions.turbulence(y, x, z, noisesize, noiseDepth, noiseFunction, isHard);
            pixel.blue = NoiseGenerator.NoiseFunctions.turbulence(y, z, x, noisesize, noiseDepth, noiseFunction, isHard);

            pixel.green = FastMath.clamp(pixel.green, 0.0f, 1.0f);
            pixel.blue = FastMath.clamp(pixel.blue, 0.0f, 1.0f);
            pixel.alpha = 1.0f;

            this.applyBrightnessAndContrast(bacd, pixel);
        } else {
            this.applyBrightnessAndContrast(pixel, bacd.contrast, bacd.brightness);
        }
    }
}
