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
 * This class generates the 'clouds' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorClouds extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorClouds(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		// preparing the proper data
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();

		// reading the data from the texture structure
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		int noiseDepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		int noiseBasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
		int noiseType = ((Number) tex.getFieldValue("noisetype")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float bright = ((Number) tex.getFieldValue("bright")).floatValue();
		boolean isHard = noiseType != NoiseGenerator.TEX_NOISESOFT;
		int sType = ((Number) tex.getFieldValue("stype")).intValue();
		int halfW = width, halfH = height, halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = sType == NoiseGenerator.TEX_COLOR || colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = sType == NoiseGenerator.TEX_COLOR || colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				for (int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					texres.tin = noiseGenerator.bliGTurbulence(noisesize, texvec[0], texvec[1], texvec[2], noiseDepth, isHard, noiseBasis);
					if (colorBand != null) {
						noiseGenerator.doColorband(colorBand, texres, dataRepository);
						if (texres.nor != null) {
							float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
							// calculate bumpnormal
							texres.nor[0] = noiseGenerator.bliGTurbulence(noisesize, texvec[0] + nabla, texvec[1], texvec[2], noiseDepth, isHard, noiseBasis);
							texres.nor[1] = noiseGenerator.bliGTurbulence(noisesize, texvec[0], texvec[1] + nabla, texvec[2], noiseDepth, isHard, noiseBasis);
							texres.nor[2] = noiseGenerator.bliGTurbulence(noisesize, texvec[0], texvec[1], texvec[2] + nabla, noiseDepth, isHard, noiseBasis);
							noiseGenerator.texNormalDerivate(colorBand, texres, dataRepository);
						}
						noiseGenerator.brightnesAndContrastRGB(tex, texres);
						data.put((byte) (texres.tr * 255.0f));
						data.put((byte) (texres.tg * 255.0f));
						data.put((byte) (texres.tb * 255.0f));
					} else if (sType == NoiseGenerator.TEX_COLOR) {
						// in this case, int. value should really be computed from color,
						// and bumpnormal from that, would be too slow, looks ok as is
						texres.tr = texres.tin;
						texres.tg = noiseGenerator.bliGTurbulence(noisesize, texvec[1], texvec[0], texvec[2], noiseDepth, isHard, noiseBasis);
						texres.tb = noiseGenerator.bliGTurbulence(noisesize, texvec[1], texvec[2], texvec[0], noiseDepth, isHard, noiseBasis);
						noiseGenerator.brightnesAndContrastRGB(tex, texres);
						data.put((byte) (texres.tr * 255.0f));
						data.put((byte) (texres.tg * 255.0f));
						data.put((byte) (texres.tb * 255.0f));
					} else {
						noiseGenerator.brightnesAndContrast(texres, contrast, bright);
						data.put((byte) (texres.tin * 255));
					}
				}
			}
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(data);
		return new Texture3D(new Image(format, width, height, depth, dataArray));
	}
}
