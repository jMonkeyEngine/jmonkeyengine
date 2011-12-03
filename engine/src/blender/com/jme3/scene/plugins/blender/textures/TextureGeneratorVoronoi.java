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
import com.jme3.scene.plugins.blender.textures.NoiseGenerator.NoiseMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * This class generates the 'voronoi' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorVoronoi extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorVoronoi(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, BlenderContext blenderContext) {
		float voronoiWeight1 = ((Number) tex.getFieldValue("vn_w1")).floatValue();
		float voronoiWeight2 = ((Number) tex.getFieldValue("vn_w2")).floatValue();
		float voronoiWeight3 = ((Number) tex.getFieldValue("vn_w3")).floatValue();
		float voronoiWeight4 = ((Number) tex.getFieldValue("vn_w4")).floatValue();
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		float outscale = ((Number) tex.getFieldValue("ns_outscale")).floatValue();
		float mexp = ((Number) tex.getFieldValue("vn_mexp")).floatValue();
		int distm = ((Number) tex.getFieldValue("vn_distm")).intValue();
		int voronoiColorType = ((Number) tex.getFieldValue("vn_coltype")).intValue();

		TexturePixel texres = new TexturePixel();
		float[] texvec = new float[] { 0, 0, 0 };
		int halfW = width >> 1, halfH = height >> 1, halfD = depth >> 1, index = 0;
		float wDelta = 1.0f / halfW, hDelta = 1.0f / halfH, dDelta = 1.0f / halfD;
		
		float[][] colorBand = this.computeColorband(tex, blenderContext);
		Format format = voronoiColorType != 0 || colorBand != null ? Format.RGBA8 : Format.Luminance8;
		int bytesPerPixel = voronoiColorType != 0 || colorBand != null ? 4 : 1;
		BrightnessAndContrastData bacd = new BrightnessAndContrastData(tex);
		
		float[] da = new float[4], pa = new float[12];
		float[] hashPoint = voronoiColorType != 0 ? new float[3] : null;
		float[] voronoiWeights = new float[] {FastMath.abs(voronoiWeight1), FastMath.abs(voronoiWeight2), 
											  FastMath.abs(voronoiWeight3), FastMath.abs(voronoiWeight4)};
		float weight;
		float sc = voronoiWeights[0] + voronoiWeights[1] + voronoiWeights[2] + voronoiWeights[3];
		if (sc != 0.0f) {
			sc = outscale / sc;
		}

		byte[] data = new byte[width * height * depth * bytesPerPixel];
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;
				for (int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					NoiseGenerator.NoiseFunctions.voronoi(texvec[0], texvec[1], texvec[2], da, pa, mexp, distm);
					texres.intensity = sc * FastMath.abs(voronoiWeight1 * da[0] + voronoiWeight2 * da[1] + voronoiWeight3 * da[2] + voronoiWeight4 * da[3]);
					if(texres.intensity>1.0f) {
						texres.intensity = 1.0f;
					} else if(texres.intensity<0.0f) {
						texres.intensity = 0.0f;
					}
					
					if (colorBand != null) {//colorband ALWAYS goes first and covers the color (if set)
						int colorbandIndex = (int) (texres.intensity * 1000.0f);
						texres.red = colorBand[colorbandIndex][0];
						texres.green = colorBand[colorbandIndex][1];
						texres.blue = colorBand[colorbandIndex][2];
						texres.alpha = colorBand[colorbandIndex][3];
					} else if (voronoiColorType != 0) {
						texres.red = texres.green = texres.blue = 0.0f;
						texres.alpha = 1.0f;
						for(int m=0; m<12; m+=3) {
							weight = voronoiWeights[m/3];
							this.cellNoiseV(pa[m], pa[m + 1], pa[m + 2], hashPoint);
							texres.red += weight * hashPoint[0];
							texres.green += weight * hashPoint[1];
							texres.blue += weight * hashPoint[2];
						}
						if (voronoiColorType >= 2) {
							float t1 = (da[1] - da[0]) * 10.0f;
							if (t1 > 1.0f) {
								t1 = 1.0f;
							}
							if (voronoiColorType == 3) {
								t1 *= texres.intensity;
							} else {
								t1 *= sc;
							}
							texres.red *= t1;
							texres.green *= t1;
							texres.blue *= t1;
						} else {
							texres.red *= sc;
							texres.green *= sc;
							texres.blue *= sc;
						}
					}

					if (voronoiColorType != 0 || colorBand != null) {
						this.applyBrightnessAndContrast(bacd, texres);
						data[index++] = (byte) (texres.red * 255.0f);
						data[index++] = (byte) (texres.green * 255.0f);
						data[index++] = (byte) (texres.blue * 255.0f);
						data[index++] = (byte) (texres.alpha * 255.0f);
					} else {
						this.applyBrightnessAndContrast(texres, bacd.contrast, bacd.brightness);
						data[index++] = (byte) (texres.intensity * 255.0f);
					}
				}
			}
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(BufferUtils.createByteBuffer(data));
		return new Texture3D(new Image(format, width, height, depth, dataArray));
	}
	
	/**
     * Returns a vector/point/color in ca, using point hasharray directly
     */
    private void cellNoiseV(float x, float y, float z, float[] hashPoint) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        NoiseMath.hash(xi, yi, zi, hashPoint);
    }
}
