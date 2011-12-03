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
 * This class generates the 'noise' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorNoise extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorNoise(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, BlenderContext blenderContext) {
		int val, random, loop;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		TexturePixel texres = new TexturePixel();
		int halfW = width >> 1, halfH = height >> 1, halfD = depth >> 1, index = 0;
		float[][] colorBand = this.computeColorband(tex, blenderContext);
		Format format = colorBand != null ? Format.RGBA8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 4 : 1;
		BrightnessAndContrastData bacd = new BrightnessAndContrastData(tex);
		
		byte[] data = new byte[width * height * depth * bytesPerPixel];
		for (int i = -halfW; i < halfW; ++i) {
			for (int j = -halfH; j < halfH; ++j) {
				for (int k = -halfD; k < halfD; ++k) {
					random = FastMath.rand.nextInt();
					val = random & 3;

					loop = noisedepth;
					while (loop-- != 0) {
						random >>= 2;
						val *= random & 3;
					}
					texres.intensity = FastMath.clamp(val, 0.0f, 1.0f);
					if (colorBand != null) {
						int colorbandIndex = (int) (texres.intensity * 1000.0f);
						texres.red = colorBand[colorbandIndex][0];
						texres.green = colorBand[colorbandIndex][1];
						texres.blue = colorBand[colorbandIndex][2];
						
						this.applyBrightnessAndContrast(bacd, texres);
						data[index++] = (byte) (texres.red * 255.0f);
						data[index++] = (byte) (texres.green * 255.0f);
						data[index++] = (byte) (texres.blue * 255.0f);
						data[index++] = (byte) (colorBand[colorbandIndex][3] * 255.0f);
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
}
