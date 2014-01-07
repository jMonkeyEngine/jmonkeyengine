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
 * This class generates the 'distorted noise' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorDistnoise extends TextureGenerator {
    protected float noisesize;
    protected float distAmount;
    protected int   noisebasis;
    protected int   noisebasis2;

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorDistnoise(NoiseGenerator noiseGenerator) {
        super(noiseGenerator, Format.Luminance8);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
        distAmount = ((Number) tex.getFieldValue("dist_amount")).floatValue();
        noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
        noisebasis2 = ((Number) tex.getFieldValue("noisebasis2")).intValue();
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        pixel.intensity = this.musgraveVariableLunacrityNoise(x * 4, y * 4, z * 4, distAmount, noisebasis, noisebasis2);
        pixel.intensity = FastMath.clamp(pixel.intensity, 0.0f, 1.0f);
        if (colorBand != null) {
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];

            this.applyBrightnessAndContrast(bacd, pixel);
        } else {
            this.applyBrightnessAndContrast(pixel, bacd.contrast, bacd.brightness);
        }
    }

    /**
     * "Variable Lacunarity Noise" A distorted variety of Perlin noise. This method is used to calculate distorted noise
     * texture.
     * @param x
     * @param y
     * @param z
     * @param distortion
     * @param nbas1
     * @param nbas2
     * @return
     */
    private float musgraveVariableLunacrityNoise(float x, float y, float z, float distortion, int nbas1, int nbas2) {
        NoiseFunction abstractNoiseFunc1 = NoiseGenerator.noiseFunctions.get(Integer.valueOf(nbas1));
        if (abstractNoiseFunc1 == null) {
            abstractNoiseFunc1 = NoiseGenerator.noiseFunctions.get(Integer.valueOf(0));
        }
        NoiseFunction abstractNoiseFunc2 = NoiseGenerator.noiseFunctions.get(Integer.valueOf(nbas2));
        if (abstractNoiseFunc2 == null) {
            abstractNoiseFunc2 = NoiseGenerator.noiseFunctions.get(Integer.valueOf(0));
        }
        // get a random vector and scale the randomization
        float rx = abstractNoiseFunc1.execute(x + 13.5f, y + 13.5f, z + 13.5f) * distortion;
        float ry = abstractNoiseFunc1.execute(x, y, z) * distortion;
        float rz = abstractNoiseFunc1.execute(x - 13.5f, y - 13.5f, z - 13.5f) * distortion;
        return abstractNoiseFunc2.executeSigned(x + rx, y + ry, z + rz); // distorted-domain noise
    }
}
