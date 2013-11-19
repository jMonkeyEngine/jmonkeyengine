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

/**
 * This class generates the 'marble' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMarble extends TextureGeneratorWood {
    // tex->stype
    protected static final int TEX_SOFT    = 0;
    protected static final int TEX_SHARP   = 1;
    protected static final int TEX_SHARPER = 2;

    protected MarbleData       marbleData;
    protected int              noisebasis;
    protected NoiseFunction    noiseFunction;

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorMarble(NoiseGenerator noiseGenerator) {
        super(noiseGenerator);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        marbleData = new MarbleData(tex);
        noisebasis = marbleData.noisebasis;
        noiseFunction = NoiseGenerator.noiseFunctions.get(noisebasis);
        if (noiseFunction == null) {
            noiseFunction = NoiseGenerator.noiseFunctions.get(0);
            noisebasis = 0;
        }
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        pixel.intensity = this.marbleInt(marbleData, x, y, z);
        if (colorBand != null) {
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];

            this.applyBrightnessAndContrast(bacd, pixel);
            pixel.alpha = colorBand[colorbandIndex][3];
        } else {
            this.applyBrightnessAndContrast(pixel, bacd.contrast, bacd.brightness);
        }
    }

    public float marbleInt(MarbleData marbleData, float x, float y, float z) {
        int waveform;
        if (marbleData.waveform > TEX_TRI || marbleData.waveform < TEX_SIN) {
            waveform = 0;
        } else {
            waveform = marbleData.waveform;
        }

        float n = 5.0f * (x + y + z);
        if (noisebasis == 0) {
            ++x;
            ++y;
            ++z;
        }
        float mi = n + marbleData.turbul * NoiseGenerator.NoiseFunctions.turbulence(x, y, z, marbleData.noisesize, marbleData.noisedepth, noiseFunction, marbleData.isHard);

        if (marbleData.stype >= TEX_SOFT) {
            mi = waveformFunctions[waveform].execute(mi);
            if (marbleData.stype == TEX_SHARP) {
                mi = (float) Math.sqrt(mi);
            } else if (marbleData.stype == TEX_SHARPER) {
                mi = (float) Math.sqrt(Math.sqrt(mi));
            }
        }
        return mi;
    }

    private static class MarbleData {
        public final float   noisesize;
        public final int     noisebasis;
        public final int     noisedepth;
        public final int     stype;
        public final float   turbul;
        public final int     waveform;
        public final boolean isHard;

        public MarbleData(Structure tex) {
            noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
            noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
            noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
            stype = ((Number) tex.getFieldValue("stype")).intValue();
            turbul = ((Number) tex.getFieldValue("turbul")).floatValue();
            int noisetype = ((Number) tex.getFieldValue("noisetype")).intValue();
            waveform = ((Number) tex.getFieldValue("noisebasis2")).intValue();
            isHard = noisetype != TEX_NOISESOFT;
        }
    }
}
