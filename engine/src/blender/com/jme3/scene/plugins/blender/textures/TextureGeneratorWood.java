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
 * This class generates the 'wood' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorWood extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator the noise generator
	 */
	public TextureGeneratorWood(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		// preparing the proper data
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float bright = ((Number) tex.getFieldValue("bright")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		int halfW = width;
		int halfH = height;
		int halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				for(int k = -halfD; k < halfD; ++k) {
					texvec[2] = dDelta * k;
					texres.tin = noiseGenerator.woodInt(tex, texvec[0], texvec[1], texvec[2], dataRepository);
					if (colorBand != null) {
						noiseGenerator.doColorband(colorBand, texres, dataRepository);
						if (texres.nor != null) {// calculate bumpnormal
							texres.nor[0] = noiseGenerator.woodInt(tex, texvec[0] + nabla, texvec[1], texvec[2], dataRepository);
							texres.nor[1] = noiseGenerator.woodInt(tex, texvec[0], texvec[1] + nabla, texvec[2], dataRepository);
							texres.nor[2] = noiseGenerator.woodInt(tex, texvec[0], texvec[1], texvec[2] + nabla, dataRepository);
							noiseGenerator.texNormalDerivate(colorBand, texres, dataRepository);
						}
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
