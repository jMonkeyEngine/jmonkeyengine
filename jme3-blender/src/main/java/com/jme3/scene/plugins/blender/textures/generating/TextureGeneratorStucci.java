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
import com.jme3.scene.plugins.blender.textures.TexturePixel;
import com.jme3.scene.plugins.blender.textures.generating.NoiseGenerator.NoiseFunction;
import com.jme3.texture.Image.Format;

/**
 * This class generates the 'stucci' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorStucci extends TextureGenerator {
    protected static final int TEX_NOISESOFT = 0;

    protected float            noisesize;
    protected int              noisebasis;
    protected NoiseFunction    noiseFunction;
    protected int              noisetype;
    protected float            turbul;
    protected boolean          isHard;
    protected int              stype;

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorStucci(NoiseGenerator noiseGenerator) {
        super(noiseGenerator, Format.Luminance8);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();

        noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
        noiseFunction = NoiseGenerator.noiseFunctions.get(noisebasis);
        if (noiseFunction == null) {
            noiseFunction = NoiseGenerator.noiseFunctions.get(0);
            noisebasis = 0;
        }

        noisetype = ((Number) tex.getFieldValue("noisetype")).intValue();
        turbul = ((Number) tex.getFieldValue("turbul")).floatValue();
        isHard = noisetype != TEX_NOISESOFT;
        stype = ((Number) tex.getFieldValue("stype")).intValue();
        if (noisesize <= 0.001f) {// the texture goes black if this value is lower than 0.001f
            noisesize = 0.001f;
        }
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        if (noisebasis == 0) {
            ++x;
            ++y;
            ++z;
        }
        float noiseValue = NoiseGenerator.NoiseFunctions.noise(x, y, z, noisesize, 0, noiseFunction, isHard);
        float ofs = turbul / 200.0f;
        if (stype != 0) {
            ofs *= noiseValue * noiseValue;
        }

        pixel.intensity = NoiseGenerator.NoiseFunctions.noise(x, y, z + ofs, noisesize, 0, noiseFunction, isHard);
        if (colorBand != null) {
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];
            pixel.alpha = colorBand[colorbandIndex][3];
        }

        if (stype == NoiseGenerator.TEX_WALLOUT) {
            pixel.intensity = 1.0f - pixel.intensity;
        }
        if (pixel.intensity < 0.0f) {
            pixel.intensity = 0.0f;
        }
        // no brightness and contrast needed for stucci (it doesn't affect the texture)
    }
}
