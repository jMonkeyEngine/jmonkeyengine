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
import com.jme3.scene.plugins.blender.textures.generating.NoiseGenerator.DistanceFunction;
import com.jme3.scene.plugins.blender.textures.generating.NoiseGenerator.NoiseMath;
import com.jme3.texture.Image.Format;

/**
 * This class generates the 'voronoi' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorVoronoi extends TextureGenerator {
    protected float            noisesize;
    protected float            outscale;
    protected float            mexp;
    protected DistanceFunction distanceFunction;
    protected int              voronoiColorType;
    protected float[]          da = new float[4], pa = new float[12];
    protected float[]          hashPoint;
    protected float[]          voronoiWeights;
    protected float            weightSum;

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorVoronoi(NoiseGenerator noiseGenerator) {
        super(noiseGenerator, Format.Luminance8);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        voronoiWeights = new float[4];
        voronoiWeights[0] = ((Number) tex.getFieldValue("vn_w1")).floatValue();
        voronoiWeights[1] = ((Number) tex.getFieldValue("vn_w2")).floatValue();
        voronoiWeights[2] = ((Number) tex.getFieldValue("vn_w3")).floatValue();
        voronoiWeights[3] = ((Number) tex.getFieldValue("vn_w4")).floatValue();
        noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
        outscale = ((Number) tex.getFieldValue("ns_outscale")).floatValue();
        mexp = ((Number) tex.getFieldValue("vn_mexp")).floatValue();
        int distanceType = ((Number) tex.getFieldValue("vn_distm")).intValue();
        distanceFunction = NoiseGenerator.distanceFunctions.get(distanceType);
        voronoiColorType = ((Number) tex.getFieldValue("vn_coltype")).intValue();
        hashPoint = voronoiColorType != 0 ? new float[3] : null;
        weightSum = voronoiWeights[0] + voronoiWeights[1] + voronoiWeights[2] + voronoiWeights[3];
        if (weightSum != 0.0f) {
            weightSum = outscale / weightSum;
        }
        if (voronoiColorType != 0 || colorBand != null) {
            imageFormat = Format.RGBA8;
        }
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        // for voronoi we need to widen the range a little
        NoiseGenerator.NoiseFunctions.voronoi(x * 4, y * 4, z * 4, da, pa, mexp, distanceFunction);
        pixel.intensity = weightSum * FastMath.abs(voronoiWeights[0] * da[0] + voronoiWeights[1] * da[1] + voronoiWeights[2] * da[2] + voronoiWeights[3] * da[3]);
        if (pixel.intensity > 1.0f) {
            pixel.intensity = 1.0f;
        } else if (pixel.intensity < 0.0f) {
            pixel.intensity = 0.0f;
        }

        if (colorBand != null) {// colorband ALWAYS goes first and covers the color (if set)
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];
            pixel.alpha = colorBand[colorbandIndex][3];
        } else if (voronoiColorType != 0) {
            pixel.red = pixel.green = pixel.blue = 0.0f;
            pixel.alpha = 1.0f;
            for (int m = 0; m < 12; m += 3) {
                float weight = voronoiWeights[m / 3];
                NoiseMath.hash((int) pa[m], (int) pa[m + 1], (int) pa[m + 2], hashPoint);
                pixel.red += weight * hashPoint[0];
                pixel.green += weight * hashPoint[1];
                pixel.blue += weight * hashPoint[2];
            }
            if (voronoiColorType >= 2) {
                float t1 = (da[1] - da[0]) * 10.0f;
                if (t1 > 1.0f) {
                    t1 = 1.0f;
                }
                if (voronoiColorType == 3) {
                    t1 *= pixel.intensity;
                } else {
                    t1 *= weightSum;
                }
                pixel.red *= t1;
                pixel.green *= t1;
                pixel.blue *= t1;
            } else {
                pixel.red *= weightSum;
                pixel.green *= weightSum;
                pixel.blue *= weightSum;
            }
        }

        if (voronoiColorType != 0 || colorBand != null) {
            this.applyBrightnessAndContrast(bacd, pixel);
        } else {
            this.applyBrightnessAndContrast(pixel, bacd.contrast, bacd.brightness);
        }
    }
}
