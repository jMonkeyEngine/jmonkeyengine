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
 * This class generates the 'magic' texture.
 * @author Marcin Roguski (Kaelthas)
 */
public class TextureGeneratorMagic extends TextureGenerator {

	/**
	 * Constructor stores the given noise generator.
	 * @param noiseGenerator
	 *        the noise generator
	 */
	public TextureGeneratorMagic(NoiseGenerator noiseGenerator) {
		super(noiseGenerator);
	}

	@Override
	protected Texture generate(Structure tex, int width, int height, int depth, DataRepository dataRepository) {
		float x, y, z, turb;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		float turbul = ((Number) tex.getFieldValue("turbul")).floatValue() / 5.0f;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		float wDelta = 1.0f / width, hDelta = 1.0f / height, dDelta = 1.0f / depth;
		int halfW = width, halfH = height, halfD = depth;
		width <<= 1;
		height <<= 1;
		depth <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * depth * 4);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				for (int k = -halfD; k < halfD; ++k) {
					turb = turbul;
					texvec[2] = dDelta * k;// z
					x = (float) Math.sin((texvec[0] + texvec[1] + texvec[2]) * 5.0f);
					y = (float) Math.cos((-texvec[0] + texvec[1] - texvec[2]) * 5.0f);
					z = -(float) Math.cos((-texvec[0] - texvec[1] + texvec[2]) * 5.0f);

					if (colorBand != null) {
						texres.tin = 0.3333f * (x + y + z);
						noiseGenerator.doColorband(colorBand, texres, dataRepository);
					} else {
						if (noisedepth > 0) {
							x *= turb;
							y *= turb;
							z *= turb;
							y = -(float) Math.cos(x - y + z) * turb;
							if (noisedepth > 1) {
								x = (float) Math.cos(x - y - z) * turb;
								if (noisedepth > 2) {
									z = (float) Math.sin(-x - y - z) * turb;
									if (noisedepth > 3) {
										x = -(float) Math.cos(-x + y - z) * turb;
										if (noisedepth > 4) {
											y = -(float) Math.sin(-x + y + z) * turb;
											if (noisedepth > 5) {
												y = -(float) Math.cos(-x + y + z) * turb;
												if (noisedepth > 6) {
													x = (float) Math.cos(x + y + z) * turb;
													if (noisedepth > 7) {
														z = (float) Math.sin(x + y - z) * turb;
														if (noisedepth > 8) {
															x = -(float) Math.cos(-x - y + z) * turb;
															if (noisedepth > 9) {
																y = -(float) Math.sin(x - y + z) * turb;
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}

						if (turb != 0.0f) {
							turb *= 2.0f;
							x /= turb;
							y /= turb;
							z /= turb;
						}
						texres.tr = 0.5f - x;
						texres.tg = 0.5f - y;
						texres.tb = 0.5f - z;
					}
					noiseGenerator.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tin * 255));
					data.put((byte) (texres.tb * 255));
					data.put((byte) (texres.tg * 255));
					data.put((byte) (texres.tr * 255));
				}
			}
		}
		ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(1);
		dataArray.add(data);
		return new Texture3D(new Image(Format.ABGR8, width, height, depth, dataArray));
	}
}
