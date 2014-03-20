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
import com.jme3.texture.Image.Format;

/**
 * This class generates the 'magic' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMagic extends TextureGenerator {
    private static NoiseDepthFunction[] noiseDepthFunctions = new NoiseDepthFunction[10];
    static {
        noiseDepthFunctions[0] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[1] = -(float) Math.cos(xyz[0] - xyz[1] + xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[1] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[0] = (float) Math.cos(xyz[0] - xyz[1] - xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[2] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[2] = (float) Math.sin(-xyz[0] - xyz[1] - xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[3] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[0] = -(float) Math.cos(-xyz[0] + xyz[1] - xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[4] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[1] = -(float) Math.sin(-xyz[0] + xyz[1] + xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[5] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[1] = -(float) Math.cos(-xyz[0] + xyz[1] + xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[6] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[0] = (float) Math.cos(xyz[0] + xyz[1] + xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[7] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[2] = (float) Math.sin(xyz[0] + xyz[1] - xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[8] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[0] = -(float) Math.cos(-xyz[0] - xyz[1] + xyz[2]) * turbulence;
            }
        };
        noiseDepthFunctions[9] = new NoiseDepthFunction() {
            public void compute(float[] xyz, float turbulence) {
                xyz[1] = -(float) Math.sin(xyz[0] - xyz[1] + xyz[2]) * turbulence;
            }
        };
    }

    protected int                       noisedepth;
    protected float                     turbul;
    protected float[]                   xyz                 = new float[3];

    /**
     * Constructor stores the given noise generator.
     * @param noiseGenerator
     *            the noise generator
     */
    public TextureGeneratorMagic(NoiseGenerator noiseGenerator) {
        super(noiseGenerator, Format.RGBA8);
    }

    @Override
    public void readData(Structure tex, BlenderContext blenderContext) {
        super.readData(tex, blenderContext);
        noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
        turbul = ((Number) tex.getFieldValue("turbul")).floatValue() / 5.0f;
    }

    @Override
    public void getPixel(TexturePixel pixel, float x, float y, float z) {
        float turb = turbul;
        xyz[0] = (float) Math.sin((x + y + z) * 5.0f);
        xyz[1] = (float) Math.cos((-x + y - z) * 5.0f);
        xyz[2] = -(float) Math.cos((-x - y + z) * 5.0f);

        if (colorBand != null) {
            pixel.intensity = FastMath.clamp(0.3333f * (xyz[0] + xyz[1] + xyz[2]), 0.0f, 1.0f);
            int colorbandIndex = (int) (pixel.intensity * 1000.0f);
            pixel.red = colorBand[colorbandIndex][0];
            pixel.green = colorBand[colorbandIndex][1];
            pixel.blue = colorBand[colorbandIndex][2];
            pixel.alpha = colorBand[colorbandIndex][3];
        } else {
            if (noisedepth > 0) {
                xyz[0] *= turb;
                xyz[1] *= turb;
                xyz[2] *= turb;
                for (int m = 0; m < noisedepth; ++m) {
                    noiseDepthFunctions[m].compute(xyz, turb);
                }
            }

            if (turb != 0.0f) {
                turb *= 2.0f;
                xyz[0] /= turb;
                xyz[1] /= turb;
                xyz[2] /= turb;
            }
            pixel.red = 0.5f - xyz[0];
            pixel.green = 0.5f - xyz[1];
            pixel.blue = 0.5f - xyz[2];
            pixel.alpha = 1.0f;
        }
        this.applyBrightnessAndContrast(bacd, pixel);
    }

    private static interface NoiseDepthFunction {
        void compute(float[] xyz, float turbulence);
    }
}
