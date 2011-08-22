/*
 *
 * $Id: noise.c 14611 2008-04-29 08:24:33Z campbellbarton $
 *
 * ***** BEGIN GPL LICENSE BLOCK *****
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * The Original Code is Copyright (C) 2001-2002 by NaN Holding BV.
 * All rights reserved.
 *
 * The Original Code is: all of this file.
 *
 * Contributor(s): none yet.
 *
 * ***** END GPL LICENSE BLOCK *****
 *
 */
package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureHelper.ColorBand;
import com.jme3.scene.plugins.blender.textures.TextureHelper.TexResult;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture3D;
import com.jme3.util.BufferUtils;

/**
 * This class generates the 'distorted noise' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorDistnoise extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorDistnoise(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float distAmount = ((Number) tex.getFieldValue("dist_amount")).floatValue();
		int noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
		int noisebasis2 = ((Number) tex.getFieldValue("noisebasis2")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();

		TexResult texres = new TexResult();
		float[] texvec = new float[] { 0, 0, 0 };
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth;
		int halfW = width, halfH = height, halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;
				for (int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;// z
					texres.tin = noiseGenerator.mgVLNoise(texvec[0], texvec[1], texvec[2], distAmount, noisebasis, noisebasis2);
					if (colorBand != null) {
						noiseGenerator.doColorband(colorBand, texres, dataRepository);
						if (texres.nor != null) {
							float offs = nabla / noisesize; // also scaling of texvec
							/* calculate bumpnormal */
							texres.nor[0] = noiseGenerator.mgVLNoise(texvec[0] + offs, texvec[1], texvec[2], distAmount, noisebasis, noisebasis2);
							texres.nor[1] = noiseGenerator.mgVLNoise(texvec[0], texvec[1] + offs, texvec[2], distAmount, noisebasis, noisebasis2);
							texres.nor[2] = noiseGenerator.mgVLNoise(texvec[0], texvec[1], texvec[2] + offs, distAmount, noisebasis, noisebasis2);
							noiseGenerator.texNormalDerivate(colorBand, texres, dataRepository);
						}

						noiseGenerator.brightnesAndContrastRGB(tex, texres);
						data.put((byte) (texres.tr * 255.0f));
						data.put((byte) (texres.tg * 255.0f));
						data.put((byte) (texres.tb * 255.0f));
					} else {
						noiseGenerator.brightnesAndContrast(texres, contrast, brightness);
						data.put((byte) (texres.tin * 255.0f));
					}
				}
			}
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(data);
		return new Texture3D(new Image(format, width, height, depth, dataArray));
	}
}
