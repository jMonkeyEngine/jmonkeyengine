/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * This class generates the 'magic' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMagic extends TextureGenerator {
	private static NoiseDepthFunction[] noiseDepthFunctions = new NoiseDepthFunction[10];
	static {
		noiseDepthFunctions[0] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[1] = -(float) Math.cos(xyz[0] - xyz[1] + xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[1] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[0] = (float) Math.cos(xyz[0] - xyz[1] - xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[2] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[2] = (float) Math.sin(-xyz[0] - xyz[1] - xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[3] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[0] = -(float) Math.cos(-xyz[0] + xyz[1] - xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[4] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[1] = -(float) Math.sin(-xyz[0] + xyz[1] + xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[5] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[1] = -(float) Math.cos(-xyz[0] + xyz[1] + xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[6] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[0] = (float) Math.cos(xyz[0] + xyz[1] + xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[7] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[2] = (float) Math.sin(xyz[0] + xyz[1] - xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[8] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[0] = -(float) Math.cos(-xyz[0] - xyz[1] + xyz[2]) * turbulence;
			}
		};
		noiseDepthFunctions[9] = new NoiseDepthFunction() {
			@Override
			public void compute(float[] xyz, float turbulence) {
				xyz[1] = -(float) Math.sin(xyz[0] - xyz[1] + xyz[2]) * turbulence;
			}
		};
	}
	
	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorMagic(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, BlenderContext blenderContext) {
		float xyz[] = new float[3], turb;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		float turbul = ((Number) tex.getFieldValue("turbul")).floatValue() / 5.0f;
		float[] texvec = new float[] { 0, 0, 0 };
		TexturePixel texres = new TexturePixel();
		int halfW = width >> 1, halfH = height >> 1, halfD = depth >> 1, index = 0;
		float wDelta = 1.0f / halfW, hDelta = 1.0f / halfH, dDelta = 1.0f / halfD;
		float[][] colorBand = this.computeColorband(tex, blenderContext);
		BrightnessAndContrastData bacd = new BrightnessAndContrastData(tex);
		
		byte[] data = new byte[width * height * depth * 4];
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				for (int k = -halfD; k < halfD; ++k) {
					turb = turbul;
					texvec[2] = dDelta * k;
					xyz[0] = (float) Math.sin((texvec[0] + texvec[1] + texvec[2]) * 5.0f);
					xyz[1] = (float) Math.cos((-texvec[0] + texvec[1] - texvec[2]) * 5.0f);
					xyz[2] = -(float) Math.cos((-texvec[0] - texvec[1] + texvec[2]) * 5.0f);

					if (colorBand != null) {
						texres.intensity = FastMath.clamp(0.3333f * (xyz[0] + xyz[1] + xyz[2]), 0.0f, 1.0f);
						int colorbandIndex = (int) (texres.intensity * 1000.0f);
						texres.red = colorBand[colorbandIndex][0];
						texres.green = colorBand[colorbandIndex][1];
						texres.blue = colorBand[colorbandIndex][2];
						texres.alpha = colorBand[colorbandIndex][3];
					} else {
						if (noisedepth > 0) {
							xyz[0] *= turb;
							xyz[1] *= turb;
							xyz[2] *= turb;
							for (int m=0;m<noisedepth;++m) {
								noiseDepthFunctions[m].compute(xyz, turb);
							}
						}

						if (turb != 0.0f) {
							turb *= 2.0f;
							xyz[0] /= turb;
							xyz[1] /= turb;
							xyz[2] /= turb;
						}
						texres.red = 0.5f - xyz[0];
						texres.green = 0.5f - xyz[1];
						texres.blue = 0.5f - xyz[2];
						texres.alpha = 1.0f;
					}
					this.applyBrightnessAndContrast(bacd, texres);
					data[index++] = (byte) (texres.red * 255.0f);
					data[index++] = (byte) (texres.green * 255.0f);
					data[index++] = (byte) (texres.blue * 255.0f);
					data[index++] = (byte) (texres.alpha * 255.0f);
				}
			}
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(BufferUtils.createByteBuffer(data));
		return new Texture3D(new Image(Format.RGBA8, width, height, depth, dataArray));
	}
	
	private static interface NoiseDepthFunction {
		void compute(float[] xyz, float turbulence);
	}
}
