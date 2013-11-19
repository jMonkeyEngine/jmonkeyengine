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
import com.jme3.scene.plugins.blender.textures.generating.NoiseGenerator.MusgraveFunction;
import com.jme3.scene.plugins.blender.textures.generating.NoiseGenerator.NoiseFunction;
import com.jme3.texture.Image.Format;

/**
 * This class generates the 'musgrave' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMusgrave extends TextureGenerator {
    protected MusgraveData     musgraveData;
    protected MusgraveFunction musgraveFunction;
    protected int              stype;
    protected float            noisesize;

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorMusgrave(NoiseGenerator noiseGenerator) {
        super(noiseGenerator, Format.Luminance8);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        musgraveData = new MusgraveData(tex);
        stype = ((Number) tex.getFieldValue("stype")).intValue();
        noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
        musgraveFunction = NoiseGenerator.musgraveFunctions.get(Integer.valueOf(musgraveData.stype));
        if (musgraveFunction == null) {
            throw new IllegalStateException("Unknown type of musgrave texture: " + stype);
        }
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        pixel.intensity = musgraveData.outscale * musgraveFunction.execute(musgraveData, x, y, z);
        if (pixel.intensity > 1) {
            pixel.intensity = 1.0f;
        } else if (pixel.intensity < 0) {
            pixel.intensity = 0.0f;
        }

        if (colorBand != null) {
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];

            this.applyBrightnessAndContrast(pixel, bacd.contrast, bacd.brightness);
            pixel.alpha = colorBand[colorbandIndex][3];
        } else {
            this.applyBrightnessAndContrast(bacd, pixel);
        }
    }

    protected static class MusgraveData {
        public final int           stype;
        public final float         outscale;
        public final float         h;
        public final float         lacunarity;
        public final float         octaves;
        public final int           noisebasis;
        public final NoiseFunction noiseFunction;
        public final float         offset;
        public final float         gain;

        public MusgraveData(Structure tex) {
            stype = ((Number) tex.getFieldValue("stype")).intValue();
            outscale = ((Number) tex.getFieldValue("ns_outscale")).floatValue();
            h = ((Number) tex.getFieldValue("mg_H")).floatValue();
            lacunarity = ((Number) tex.getFieldValue("mg_lacunarity")).floatValue();
            octaves = ((Number) tex.getFieldValue("mg_octaves")).floatValue();
            offset = ((Number) tex.getFieldValue("mg_offset")).floatValue();
            gain = ((Number) tex.getFieldValue("mg_gain")).floatValue();

            int noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
            NoiseFunction noiseFunction = NoiseGenerator.noiseFunctions.get(noisebasis);
            if (noiseFunction == null) {
                noiseFunction = NoiseGenerator.noiseFunctions.get(0);
                noisebasis = 0;
            }
            this.noisebasis = noisebasis;
            this.noiseFunction = noiseFunction;
        }
    }
}
