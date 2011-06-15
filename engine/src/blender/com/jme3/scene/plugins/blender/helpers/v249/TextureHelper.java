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
package com.jme3.scene.plugins.blender.helpers.v249;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.TextureKey;
import com.jme3.math.FastMath;
import com.jme3.scene.plugins.blender.data.FileBlockHeader;
import com.jme3.scene.plugins.blender.data.Structure;
import com.jme3.scene.plugins.blender.exception.BlenderFileException;
import com.jme3.scene.plugins.blender.helpers.NoiseHelper;
import com.jme3.scene.plugins.blender.utils.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.utils.BlenderInputStream;
import com.jme3.scene.plugins.blender.utils.DataRepository;
import com.jme3.scene.plugins.blender.utils.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.utils.DynamicArray;
import com.jme3.scene.plugins.blender.utils.Pointer;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import com.jme3.texture.plugins.DDSLoader;
import com.jme3.texture.plugins.TGALoader;
import com.jme3.util.BufferUtils;

/**
 * A class that is used in texture calculations.
 * 
 * @author Marcin Roguski
 */
public class TextureHelper extends AbstractBlenderHelper {
	private static final Logger	LOGGER				= Logger.getLogger(TextureHelper.class.getName());

	// texture types
	public static final int		TEX_NONE			= 0;
	public static final int		TEX_CLOUDS			= 1;
	public static final int		TEX_WOOD			= 2;
	public static final int		TEX_MARBLE			= 3;
	public static final int		TEX_MAGIC			= 4;
	public static final int		TEX_BLEND			= 5;
	public static final int		TEX_STUCCI			= 6;
	public static final int		TEX_NOISE			= 7;
	public static final int		TEX_IMAGE			= 8;
	public static final int		TEX_PLUGIN			= 9;
	public static final int		TEX_ENVMAP			= 10;
	public static final int		TEX_MUSGRAVE		= 11;
	public static final int		TEX_VORONOI			= 12;
	public static final int		TEX_DISTNOISE		= 13;

	// mapto
	public static final int		MAP_COL				= 1;
	public static final int		MAP_NORM			= 2;
	public static final int		MAP_COLSPEC			= 4;
	public static final int		MAP_COLMIR			= 8;
	public static final int		MAP_VARS			= 0xFFF0;
	public static final int		MAP_REF				= 16;
	public static final int		MAP_SPEC			= 32;
	public static final int		MAP_EMIT			= 64;
	public static final int		MAP_ALPHA			= 128;
	public static final int		MAP_HAR				= 256;
	public static final int		MAP_RAYMIRR			= 512;
	public static final int		MAP_TRANSLU			= 1024;
	public static final int		MAP_AMB				= 2048;
	public static final int		MAP_DISPLACE		= 4096;
	public static final int		MAP_WARP			= 8192;
	public static final int		MAP_LAYER			= 16384;

	// blendtypes
	public static final int		MTEX_BLEND			= 0;
	public static final int		MTEX_MUL			= 1;
	public static final int		MTEX_ADD			= 2;
	public static final int		MTEX_SUB			= 3;
	public static final int		MTEX_DIV			= 4;
	public static final int		MTEX_DARK			= 5;
	public static final int		MTEX_DIFF			= 6;
	public static final int		MTEX_LIGHT			= 7;
	public static final int		MTEX_SCREEN			= 8;
	public static final int		MTEX_OVERLAY		= 9;
	public static final int		MTEX_BLEND_HUE		= 10;
	public static final int		MTEX_BLEND_SAT		= 11;
	public static final int		MTEX_BLEND_VAL		= 12;
	public static final int		MTEX_BLEND_COLOR	= 13;
	public static final int		MTEX_NUM_BLENDTYPES	= 14;

	// variables used in rampBlend method
	public static final int		MA_RAMP_BLEND		= 0;
	public static final int		MA_RAMP_ADD			= 1;
	public static final int		MA_RAMP_MULT		= 2;
	public static final int		MA_RAMP_SUB			= 3;
	public static final int		MA_RAMP_SCREEN		= 4;
	public static final int		MA_RAMP_DIV			= 5;
	public static final int		MA_RAMP_DIFF		= 6;
	public static final int		MA_RAMP_DARK		= 7;
	public static final int		MA_RAMP_LIGHT		= 8;
	public static final int		MA_RAMP_OVERLAY		= 9;
	public static final int		MA_RAMP_DODGE		= 10;
	public static final int		MA_RAMP_BURN		= 11;
	public static final int		MA_RAMP_HUE			= 12;
	public static final int		MA_RAMP_SAT			= 13;
	public static final int		MA_RAMP_VAL			= 14;
	public static final int		MA_RAMP_COLOR		= 15;

	/**
	 * This constructor parses the given blender version and stores the result. Some functionalities may differ in different blender
	 * versions.
	 * 
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public TextureHelper(String blenderVersion) {
		super(blenderVersion);
	}

	/**
	 * This class returns a texture read from the file or from packed blender data. The returned texture has the name set to the value of
	 * its blender type.
	 * 
	 * @param tex
	 *        texture structure filled with data
	 * @param dataRepository
	 *        the data repository
	 * @return the texture that can be used by JME engine
	 * @throws BlenderFileException
	 *         this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	public Texture getTexture(Structure tex, DataRepository dataRepository) throws BlenderFileException {
		Texture result = (Texture) dataRepository.getLoadedFeature(tex.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
		if (result != null) {
			return result;
		}
		int type = ((Number) tex.getFieldValue("type")).intValue();
		int width = dataRepository.getBlenderKey().getGeneratedTextureWidth();
		int height = dataRepository.getBlenderKey().getGeneratedTextureHeight();

		switch (type) {
			case TEX_NONE:// No texture, do nothing
				break;
			case TEX_IMAGE:// (it is first because probably this will be most commonly used)
				Pointer pImage = (Pointer) tex.getFieldValue("ima");
				Structure image = pImage.fetchData(dataRepository.getInputStream()).get(0);
				result = this.getTextureFromImage(image, dataRepository);
				break;
			case TEX_CLOUDS:
				result = this.clouds(tex, width, height, dataRepository);
				break;
			case TEX_WOOD:
				result = this.wood(tex, width, height, dataRepository);
				break;
			case TEX_MARBLE:
				result = this.marble(tex, width, height, dataRepository);
				break;
			case TEX_MAGIC:
				result = this.magic(tex, width, height, dataRepository);
				break;
			case TEX_BLEND:
				result = this.blend(tex, width, height, dataRepository);
				break;
			case TEX_STUCCI:
				result = this.stucci(tex, width, height, dataRepository);
				break;
			case TEX_NOISE:
				result = this.texnoise(tex, width, height, dataRepository);
				break;
			case TEX_MUSGRAVE:
				result = this.musgrave(tex, width, height, dataRepository);
				break;
			case TEX_VORONOI:
				result = this.voronoi(tex, width, height, dataRepository);
				break;
			case TEX_DISTNOISE:
				result = this.distnoise(tex, width, height, dataRepository);
				break;
			case TEX_PLUGIN:
			case TEX_ENVMAP:// TODO: implement envmap texture
				LOGGER.log(Level.WARNING, "Unsupported texture type: " + type + " for texture: " + tex.getName());
				break;
			default:
				throw new BlenderFileException("Unknown texture type: " + type + " for texture: " + tex.getName());
		}
		if (result != null) {
			result.setName(tex.getName());
			result.setWrap(WrapMode.Repeat);
		}
		return result;
	}

	/**
	 * This method generates the clouds texture. The result is one pixel.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of texture (in pixels)
	 * @param height
	 *        the height of texture (in pixels)
	 * @param dataRepository
	 *        the data repository
	 * @return generated texture
	 */
	protected Texture clouds(Structure tex, int width, int height, DataRepository dataRepository) {
		// preparing the proper data
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();

		// reading the data from the texture structure
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		int noiseDepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		int noiseBasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
		int noiseType = ((Number) tex.getFieldValue("noisetype")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float bright = ((Number) tex.getFieldValue("bright")).floatValue();
		boolean isHard = noiseType != com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_NOISESOFT;
		int sType = ((Number) tex.getFieldValue("stype")).intValue();
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = sType == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_COLOR || colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = sType == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_COLOR || colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;// x
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;// y (z is always = 0)

				texres.tin = noiseHelper.bliGTurbulence(noisesize, texvec[0], texvec[1], texvec[2], noiseDepth, isHard, noiseBasis);
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {
						float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
						// calculate bumpnormal
						texres.nor[0] = noiseHelper.bliGTurbulence(noisesize, texvec[0] + nabla, texvec[1], texvec[2], noiseDepth, isHard, noiseBasis);
						texres.nor[1] = noiseHelper.bliGTurbulence(noisesize, texvec[0], texvec[1] + nabla, texvec[2], noiseDepth, isHard, noiseBasis);
						texres.nor[2] = noiseHelper.bliGTurbulence(noisesize, texvec[0], texvec[1], texvec[2] + nabla, noiseDepth, isHard, noiseBasis);
						noiseHelper.texNormalDerivate(colorBand, texres, dataRepository);
					}
					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else if (sType == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_COLOR) {
					// in this case, int. value should really be computed from color,
					// and bumpnormal from that, would be too slow, looks ok as is
					texres.tr = texres.tin;
					texres.tg = noiseHelper.bliGTurbulence(noisesize, texvec[1], texvec[0], texvec[2], noiseDepth, isHard, noiseBasis);
					texres.tb = noiseHelper.bliGTurbulence(noisesize, texvec[1], texvec[2], texvec[0], noiseDepth, isHard, noiseBasis);
					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, bright);
					data.put((byte) (texres.tin * 255));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the wood texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture wood(Structure tex, int width, int height, DataRepository dataRepository) {
		// preparing the proper data
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float bright = ((Number) tex.getFieldValue("bright")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		int halfW = width;
		int halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				texres.tin = noiseHelper.woodInt(tex, texvec[0], texvec[1], texvec[2], dataRepository);
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {// calculate bumpnormal
						texres.nor[0] = noiseHelper.woodInt(tex, texvec[0] + nabla, texvec[1], texvec[2], dataRepository);
						texres.nor[1] = noiseHelper.woodInt(tex, texvec[0], texvec[1] + nabla, texvec[2], dataRepository);
						texres.nor[2] = noiseHelper.woodInt(tex, texvec[0], texvec[1], texvec[2] + nabla, dataRepository);
						noiseHelper.texNormalDerivate(colorBand, texres, dataRepository);
					}
					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, bright);
					data.put((byte) (texres.tin * 255));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the marble texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture marble(Structure tex, int width, int height, DataRepository dataRepository) {
		// preparing the proper data
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float bright = ((Number) tex.getFieldValue("bright")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				texres.tin = noiseHelper.marbleInt(tex, texvec[0], texvec[1], texvec[2], dataRepository);
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {// calculate bumpnormal
						texres.nor[0] = noiseHelper.marbleInt(tex, texvec[0] + nabla, texvec[1], texvec[2], dataRepository);
						texres.nor[1] = noiseHelper.marbleInt(tex, texvec[0], texvec[1] + nabla, texvec[2], dataRepository);
						texres.nor[2] = noiseHelper.marbleInt(tex, texvec[0], texvec[1], texvec[2] + nabla, dataRepository);
						noiseHelper.texNormalDerivate(colorBand, texres, dataRepository);
					}

					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, bright);
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the magic texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture magic(Structure tex, int width, int height, DataRepository dataRepository) {
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float x, y, z, turb;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		float turbul = ((Number) tex.getFieldValue("turbul")).floatValue() / 5.0f;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				turb = turbul;
				texvec[1] = hDelta * j;
				x = (float) Math.sin((texvec[0] + texvec[1]) * 5.0f);// in blender: Math.sin((texvec[0] + texvec[1] + texvec[2]) * 5.0f);
				y = (float) Math.cos((-texvec[0] + texvec[1]) * 5.0f);// in blender: Math.cos((-texvec[0] + texvec[1] - texvec[2]) * 5.0f);
				z = -(float) Math.cos((-texvec[0] - texvec[1]) * 5.0f);// in blender: Math.cos((-texvec[0] - texvec[1] + texvec[2]) * 5.0f);

				if (colorBand != null) {
					texres.tin = 0.3333f * (x + y + z);
					noiseHelper.doColorband(colorBand, texres, dataRepository);
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
				noiseHelper.brightnesAndContrastRGB(tex, texres);
				data.put((byte) (texres.tin * 255));
				data.put((byte) (texres.tb * 255));
				data.put((byte) (texres.tg * 255));
				data.put((byte) (texres.tr * 255));
			}
		}
		return new Texture2D(new Image(Format.ABGR8, width, height, data));
	}

	/**
	 * This method generates the blend texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture blend(Structure tex, int width, int height, DataRepository dataRepository) {
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		int flag = ((Number) tex.getFieldValue("flag")).intValue();
		int stype = ((Number) tex.getFieldValue("stype")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();
		float wDelta = 1.0f / width, hDelta = 1.0f / height, x, y, t;
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;
				if ((flag & com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_FLIPBLEND) != 0) {
					x = texvec[1];
					y = texvec[0];
				} else {
					x = texvec[0];
					y = texvec[1];
				}

				if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_LIN) { /* lin */
					texres.tin = (1.0f + x) / 2.0f;
				} else if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_QUAD) { /* quad */
					texres.tin = (1.0f + x) / 2.0f;
					if (texres.tin < 0.0f) {
						texres.tin = 0.0f;
					} else {
						texres.tin *= texres.tin;
					}
				} else if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_EASE) { /* ease */
					texres.tin = (1.0f + x) / 2.0f;
					if (texres.tin <= 0.0f) {
						texres.tin = 0.0f;
					} else if (texres.tin >= 1.0f) {
						texres.tin = 1.0f;
					} else {
						t = texres.tin * texres.tin;
						texres.tin = 3.0f * t - 2.0f * t * texres.tin;
					}
				} else if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_DIAG) { /* diag */
					texres.tin = (2.0f + x + y) / 4.0f;
				} else if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_RAD) { /* radial */
					texres.tin = (float) Math.atan2(y, x) / FastMath.TWO_PI + 0.5f;
				} else { /* sphere TEX_SPHERE */
					texres.tin = 1.0f - (float) Math.sqrt(x * x + y * y + texvec[2] * texvec[2]);
					if (texres.tin < 0.0f) {
						texres.tin = 0.0f;
					}
					if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_HALO) {
						texres.tin *= texres.tin;
					} /* halo */
				}
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, brightness);
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the stucci texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture stucci(Structure tex, int width, int height, DataRepository dataRepository) {
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		int noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
		int noisetype = ((Number) tex.getFieldValue("noisetype")).intValue();
		float turbul = ((Number) tex.getFieldValue("turbul")).floatValue();
		boolean isHard = noisetype != com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_NOISESOFT;
		int stype = ((Number) tex.getFieldValue("stype")).intValue();

		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float[] texvec = new float[] { 0, 0, 0 };
		TexResult texres = new TexResult();
		float wDelta = 1.0f / width, hDelta = 1.0f / height, b2, ofs;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i;// x
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j;// y (z is always = 0)
				b2 = noiseHelper.bliGNoise(noisesize, texvec[0], texvec[1], texvec[2], isHard, noisebasis);

				ofs = turbul / 200.0f;

				if (stype != 0) {
					ofs *= b2 * b2;
				}

				texres.tin = noiseHelper.bliGNoise(noisesize, texvec[0], texvec[1], texvec[2] + ofs, isHard, noisebasis);// ==nor[2]
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {
						texres.nor[0] = noiseHelper.bliGNoise(noisesize, texvec[0] + ofs, texvec[1], texvec[2], isHard, noisebasis);
						texres.nor[1] = noiseHelper.bliGNoise(noisesize, texvec[0], texvec[1] + ofs, texvec[2], isHard, noisebasis);
						texres.nor[2] = texres.tin;
						noiseHelper.texNormalDerivate(colorBand, texres, dataRepository);

						if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_WALLOUT) {
							texres.nor[0] = -texres.nor[0];
							texres.nor[1] = -texres.nor[1];
							texres.nor[2] = -texres.nor[2];
						}
					}
				}

				if (stype == com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_WALLOUT) {
					texres.tin = 1.0f - texres.tin;
				}
				if (texres.tin < 0.0f) {
					texres.tin = 0.0f;
				}
				if (colorBand != null) {
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the noise texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	// TODO: correct this one, so it looks more like the texture generated by blender
	protected Texture texnoise(Structure tex, int width, int height, DataRepository dataRepository) {
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float div = 3.0f;
		int val, ran, loop;
		int noisedepth = ((Number) tex.getFieldValue("noisedepth")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();
		TexResult texres = new TexResult();
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			for (int j = -halfH; j < halfH; ++j) {
				ran = FastMath.rand.nextInt();// BLI_rand();
				val = ran & 3;

				loop = noisedepth;
				while (loop-- != 0) {
					ran = ran >> 2;
					val *= ran & 3;
					div *= 3.0f;
				}
				texres.tin = val;// / div;
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, brightness);
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the musgrave texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture musgrave(Structure tex, int width, int height, DataRepository dataRepository) {
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		int stype = ((Number) tex.getFieldValue("stype")).intValue();
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		TexResult texres = new TexResult();
		float[] texvec = new float[] { 0, 0, 0 };
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;
				switch (stype) {
					case com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_MFRACTAL:
					case com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_FBM:
						noiseHelper.mgMFractalOrfBmTex(tex, texvec, colorBand, texres, dataRepository);
						break;
					case com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_RIDGEDMF:
					case com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_HYBRIDMF:
						noiseHelper.mgRidgedOrHybridMFTex(tex, texvec, colorBand, texres, dataRepository);
						break;
					case com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_HTERRAIN:
						noiseHelper.mgHTerrainTex(tex, texvec, colorBand, texres, dataRepository);
						break;
					default:
						throw new IllegalStateException("Unknown type of musgrave texture: " + stype);
				}
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the voronoi texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture voronoi(Structure tex, int width, int height, DataRepository dataRepository) {
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float vn_w1 = ((Number) tex.getFieldValue("vn_w1")).floatValue();
		float vn_w2 = ((Number) tex.getFieldValue("vn_w2")).floatValue();
		float vn_w3 = ((Number) tex.getFieldValue("vn_w3")).floatValue();
		float vn_w4 = ((Number) tex.getFieldValue("vn_w4")).floatValue();
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float ns_outscale = ((Number) tex.getFieldValue("ns_outscale")).floatValue();
		float vn_mexp = ((Number) tex.getFieldValue("vn_mexp")).floatValue();
		int vn_distm = ((Number) tex.getFieldValue("vn_distm")).intValue();
		int vn_coltype = ((Number) tex.getFieldValue("vn_coltype")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();

		TexResult texres = new TexResult();
		float[] texvec = new float[] { 0, 0, 0 };
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = vn_coltype != 0 || colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = vn_coltype != 0 || colorBand != null ? 3 : 1;

		float[] da = new float[4], pa = new float[12]; /* distance and point coordinate arrays of 4 nearest neighbours */
		float[] ca = vn_coltype != 0 ? new float[3] : null; // cell color
		float aw1 = FastMath.abs(vn_w1);
		float aw2 = FastMath.abs(vn_w2);
		float aw3 = FastMath.abs(vn_w3);
		float aw4 = FastMath.abs(vn_w4);
		float sc = aw1 + aw2 + aw3 + aw4;
		if (sc != 0.f) {
			sc = ns_outscale / sc;
		}

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;

				noiseHelper.voronoi(texvec[0], texvec[1], texvec[2], da, pa, vn_mexp, vn_distm);
				texres.tin = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
				if (vn_coltype != 0) {
					noiseHelper.cellNoiseV(pa[0], pa[1], pa[2], ca);
					texres.tr = aw1 * ca[0];
					texres.tg = aw1 * ca[1];
					texres.tb = aw1 * ca[2];
					noiseHelper.cellNoiseV(pa[3], pa[4], pa[5], ca);
					texres.tr += aw2 * ca[0];
					texres.tg += aw2 * ca[1];
					texres.tb += aw2 * ca[2];
					noiseHelper.cellNoiseV(pa[6], pa[7], pa[8], ca);
					texres.tr += aw3 * ca[0];
					texres.tg += aw3 * ca[1];
					texres.tb += aw3 * ca[2];
					noiseHelper.cellNoiseV(pa[9], pa[10], pa[11], ca);
					texres.tr += aw4 * ca[0];
					texres.tg += aw4 * ca[1];
					texres.tb += aw4 * ca[2];
					if (vn_coltype >= 2) {
						float t1 = (da[1] - da[0]) * 10.0f;
						if (t1 > 1) {
							t1 = 1.0f;
						}
						if (vn_coltype == 3) {
							t1 *= texres.tin;
						} else {
							t1 *= sc;
						}
						texres.tr *= t1;
						texres.tg *= t1;
						texres.tb *= t1;
					} else {
						texres.tr *= sc;
						texres.tg *= sc;
						texres.tb *= sc;
					}
				}
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {
						float offs = nabla / noisesize; // also scaling of texvec
						// calculate bumpnormal
						noiseHelper.voronoi(texvec[0] + offs, texvec[1], texvec[2], da, pa, vn_mexp, vn_distm);
						texres.nor[0] = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
						noiseHelper.voronoi(texvec[0], texvec[1] + offs, texvec[2], da, pa, vn_mexp, vn_distm);
						texres.nor[1] = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
						noiseHelper.voronoi(texvec[0], texvec[1], texvec[2] + offs, da, pa, vn_mexp, vn_distm);
						texres.nor[2] = sc * FastMath.abs(vn_w1 * da[0] + vn_w2 * da[1] + vn_w3 * da[2] + vn_w4 * da[3]);
						noiseHelper.texNormalDerivate(colorBand, texres, dataRepository);
					}
				}

				if (vn_coltype != 0 || colorBand != null) {
					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));// tin or tr??
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, brightness);
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method generates the distorted noise texture.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param width
	 *        the width of the texture
	 * @param height
	 *        the height of the texture
	 * @param dataRepository
	 *        the data repository
	 * @return the generated texture
	 */
	protected Texture distnoise(Structure tex, int width, int height, DataRepository dataRepository) {
		NoiseHelper noiseHelper = dataRepository.getHelper(NoiseHelper.class);
		float noisesize = ((Number) tex.getFieldValue("noisesize")).floatValue();
		float nabla = ((Number) tex.getFieldValue("nabla")).floatValue();
		float distAmount = ((Number) tex.getFieldValue("dist_amount")).floatValue();
		int noisebasis = ((Number) tex.getFieldValue("noisebasis")).intValue();
		int noisebasis2 = ((Number) tex.getFieldValue("noisebasis2")).intValue();
		float contrast = ((Number) tex.getFieldValue("contrast")).floatValue();
		float brightness = ((Number) tex.getFieldValue("bright")).floatValue();

		TexResult texres = new TexResult();
		float[] texvec = new float[] { 0, 0, 0 };
		float wDelta = 1.0f / width, hDelta = 1.0f / height;
		int halfW = width, halfH = height;
		width <<= 1;
		height <<= 1;
		ColorBand colorBand = this.readColorband(tex, dataRepository);
		Format format = colorBand != null ? Format.RGB8 : Format.Luminance8;
		int bytesPerPixel = colorBand != null ? 3 : 1;

		ByteBuffer data = BufferUtils.createByteBuffer(width * height * bytesPerPixel);
		for (int i = -halfW; i < halfW; ++i) {
			texvec[0] = wDelta * i / noisesize;
			for (int j = -halfH; j < halfH; ++j) {
				texvec[1] = hDelta * j / noisesize;

				texres.tin = noiseHelper.mgVLNoise(texvec[0], texvec[1], texvec[2], distAmount, noisebasis, noisebasis2);
				if (colorBand != null) {
					noiseHelper.doColorband(colorBand, texres, dataRepository);
					if (texres.nor != null) {
						float offs = nabla / noisesize; // also scaling of texvec
						/* calculate bumpnormal */
						texres.nor[0] = noiseHelper.mgVLNoise(texvec[0] + offs, texvec[1], texvec[2], distAmount, noisebasis, noisebasis2);
						texres.nor[1] = noiseHelper.mgVLNoise(texvec[0], texvec[1] + offs, texvec[2], distAmount, noisebasis, noisebasis2);
						texres.nor[2] = noiseHelper.mgVLNoise(texvec[0], texvec[1], texvec[2] + offs, distAmount, noisebasis, noisebasis2);
						noiseHelper.texNormalDerivate(colorBand, texres, dataRepository);
					}

					noiseHelper.brightnesAndContrastRGB(tex, texres);
					data.put((byte) (texres.tr * 255.0f));
					data.put((byte) (texres.tg * 255.0f));
					data.put((byte) (texres.tb * 255.0f));
				} else {
					noiseHelper.brightnesAndContrast(texres, contrast, brightness);
					data.put((byte) (texres.tin * 255.0f));
				}
			}
		}
		return new Texture2D(new Image(format, width, height, data));
	}

	/**
	 * This method reads the colorband data from the given texture structure.
	 * 
	 * @param tex
	 *        the texture structure
	 * @param dataRepository
	 *        the data repository
	 * @return read colorband or null if not present
	 */
	protected ColorBand readColorband(Structure tex, DataRepository dataRepository) {
		ColorBand result = null;
		int flag = ((Number) tex.getFieldValue("flag")).intValue();
		if ((flag & com.jme3.scene.plugins.blender.helpers.v249.NoiseHelper.TEX_COLORBAND) != 0) {
			Pointer pColorband = (Pointer) tex.getFieldValue("coba");
			Structure colorbandStructure;
			try {
				colorbandStructure = pColorband.fetchData(dataRepository.getInputStream()).get(0);
				result = new ColorBand(colorbandStructure);
			} catch (BlenderFileException e) {
				LOGGER.warning("Cannot fetch the colorband structure. The reason: " + e.getLocalizedMessage());
				// TODO: throw an exception here ???
			}
		}
		return result;
	}

	/**
	 * This method blends the given texture with material color and the defined color in 'map to' panel. As a result of this method a new
	 * texture is created. The input texture is NOT.
	 * 
	 * @param materialColor
	 *        the material diffuse color
	 * @param texture
	 *        the texture we use in blending
	 * @param color
	 *        the color defined for the texture
	 * @param affectFactor
	 *        the factor that the color affects the texture (value form 0.0 to 1.0)
	 * @param blendType
	 *        the blending type
	 * @param dataRepository
	 *        the data repository
	 * @return new texture that was created after the blending
	 */
	public Texture blendTexture(float[] materialColor, Texture texture, float[] color, float affectFactor, int blendType, boolean neg, DataRepository dataRepository) {
		float[] materialColorClone = materialColor.clone();//this array may change, so we copy it
		Format format = texture.getImage().getFormat();
		ByteBuffer data = texture.getImage().getData(0);
		data.rewind();
		int width = texture.getImage().getWidth();
		int height = texture.getImage().getHeight();
		ByteBuffer newData = BufferUtils.createByteBuffer(width * height * 3);

		float[] resultPixel = new float[3];
		int dataIndex = 0;
		while (data.hasRemaining()) {
			float tin = this.setupMaterialColor(data, format, neg, materialColorClone);
			this.blendPixel(resultPixel, materialColorClone, color, tin, affectFactor, blendType, dataRepository);
			newData.put(dataIndex++, (byte) (resultPixel[0] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[1] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[2] * 255.0f));
		}
		return new Texture2D(new Image(Format.RGB8, width, height, newData));
	}

	/**
	 * This method alters the material color in a way dependent on the type of the image.
	 * For example the color remains untouched if the texture is of Luminance type.
	 * The luminance defines the interaction between the material color and color defined
	 * for texture blending.
	 * If the type has 3 or more color channels then the material color is replaces with the texture's
	 * color and later blended with the defined blend color.
	 * All alpha values (if present) are ignored and not used during blending.
	 * @param data
	 *        the image data
	 * @param imageFormat
	 *        the format of the image
	 * @param neg
	 *        defines it the result color should be nagated
	 * @param materialColor
	 *        the material's color (value may be changed)
	 * @return texture intensity for the current pixel
	 */
	protected float setupMaterialColor(ByteBuffer data, Format imageFormat, boolean neg, float[] materialColor) {
		// at least one byte is always taken :)
		float tin = 0.0f;
		byte pixelValue = data.get();
		float firstPixelValue = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
		switch (imageFormat) {
			case ABGR8:
				pixelValue = data.get();
				materialColor[2] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				pixelValue = data.get();
				materialColor[0] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				break;
			case BGR8:
				materialColor[2] = firstPixelValue;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				pixelValue = data.get();
				materialColor[0] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				break;
			case RGB8:
				materialColor[0] = firstPixelValue;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				pixelValue = data.get();
				materialColor[2] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				break;
			case RGBA8:
				materialColor[0] = firstPixelValue;
				pixelValue = data.get();
				materialColor[1] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				pixelValue = data.get();
				materialColor[2] = pixelValue >= 0 ? 1.0f - pixelValue / 255.0f : (~pixelValue + 1) / 255.0f;
				data.get(); // ignore alpha
				break;
			case Luminance8:
				tin = neg ? 1.0f - firstPixelValue : firstPixelValue;
				neg = false;//do not negate the materialColor, it must be unchanged
				break;
			case Luminance8Alpha8:
				tin = neg ? 1.0f - firstPixelValue : firstPixelValue;
				neg = false;//do not negate the materialColor, it must be unchanged
				data.get(); // ignore alpha
				break;
			case Luminance16:
			case Luminance16Alpha16:
			case Alpha16:
			case Alpha8:
			case ARGB4444:
			case Depth:
			case Depth16:
			case Depth24:
			case Depth32:
			case Depth32F:
			case DXT1:
			case DXT1A:
			case DXT3:
			case DXT5:
			case Intensity16:
			case Intensity8:
			case LATC:
			case LTC:
			case Luminance16F:
			case Luminance16FAlpha16F:
			case Luminance32F:
			case RGB10:
			case RGB111110F:
			case RGB16:
			case RGB16F:
			case RGB16F_to_RGB111110F:
			case RGB16F_to_RGB9E5:
			case RGB32F:
			case RGB565:
			case RGB5A1:
			case RGB9E5:
			case RGBA16:
			case RGBA16F:
			case RGBA32F:
				LOGGER.warning("Image type not yet supported for blending: " + imageFormat);
				break;
			default:
				throw new IllegalStateException("Unknown image format type: " + imageFormat);
		}
		if (neg) {
			materialColor[0] = 1.0f - materialColor[0];
			materialColor[1] = 1.0f - materialColor[1];
			materialColor[2] = 1.0f - materialColor[2];
		}
		return tin;
	}

	/**
	 * This method blends the texture with an appropriate color.
	 * 
	 * @param result
	 *        the result color (variable 'in' in blender source code)
	 * @param materialColor
	 *        the texture color (variable 'out' in blender source coude)
	 * @param color
	 *        the previous color (variable 'tex' in blender source code)
	 * @param textureIntensity
	 *        texture intensity (variable 'fact' in blender source code)
	 * @param textureFactor
	 *        texture affection factor (variable 'facg' in blender source code)
	 * @param blendtype
	 *        the blend type
	 * @param dataRepository
	 *        the data repository
	 */
	public void blendPixel(float[] result, float[] materialColor, float[] color, float textureIntensity, float textureFactor, int blendtype, DataRepository dataRepository) {
		float facm, col;

		switch (blendtype) {
			case MTEX_BLEND:
				textureIntensity *= textureFactor;
				facm = 1.0f - textureIntensity;
				result[0] = textureIntensity * color[0] + facm * materialColor[0];
				result[1] = textureIntensity * color[1] + facm * materialColor[1];
				result[2] = textureIntensity * color[2] + facm * materialColor[2];
				break;
			case MTEX_MUL:
				textureIntensity *= textureFactor;
				facm = 1.0f - textureFactor;
				result[0] = (facm + textureIntensity * materialColor[0]) * color[0];
				result[1] = (facm + textureIntensity * materialColor[1]) * color[1];
				result[2] = (facm + textureIntensity * materialColor[2]) * color[2];
				break;
			case MTEX_DIV:
				textureIntensity *= textureFactor;
				facm = 1.0f - textureIntensity;
				if (color[0] != 0.0) {
					result[0] = (facm * materialColor[0] + textureIntensity * materialColor[0] / color[0]) * 0.5f;
				}
				if (color[1] != 0.0) {
					result[1] = (facm * materialColor[1] + textureIntensity * materialColor[1] / color[1]) * 0.5f;
				}
				if (color[2] != 0.0) {
					result[2] = (facm * materialColor[2] + textureIntensity * materialColor[2] / color[2]) * 0.5f;
				}
				break;
			case MTEX_SCREEN:
				textureIntensity *= textureFactor;
				facm = 1.0f - textureFactor;
				result[0] = 1.0f - (facm + textureIntensity * (1.0f - materialColor[0])) * (1.0f - color[0]);
				result[1] = 1.0f - (facm + textureIntensity * (1.0f - materialColor[1])) * (1.0f - color[1]);
				result[2] = 1.0f - (facm + textureIntensity * (1.0f - materialColor[2])) * (1.0f - color[2]);
				break;
			case MTEX_OVERLAY:
				textureIntensity *= textureFactor;
				facm = 1.0f - textureFactor;
				if (materialColor[0] < 0.5f) {
					result[0] = color[0] * (facm + 2.0f * textureIntensity * materialColor[0]);
				} else {
					result[0] = 1.0f - (facm + 2.0f * textureIntensity * (1.0f - materialColor[0])) * (1.0f - color[0]);
				}
				if (materialColor[1] < 0.5f) {
					result[1] = color[1] * (facm + 2.0f * textureIntensity * materialColor[1]);
				} else {
					result[1] = 1.0f - (facm + 2.0f * textureIntensity * (1.0f - materialColor[1])) * (1.0f - color[1]);
				}
				if (materialColor[2] < 0.5f) {
					result[2] = color[2] * (facm + 2.0f * textureIntensity * materialColor[2]);
				} else {
					result[2] = 1.0f - (facm + 2.0f * textureIntensity * (1.0f - materialColor[2])) * (1.0f - color[2]);
				}
				break;
			case MTEX_SUB:
				textureIntensity *= textureFactor;
				result[0] = materialColor[0] - textureIntensity * color[0];
				result[1] = materialColor[1] - textureIntensity * color[1];
				result[2] = materialColor[2] - textureIntensity * color[2];
				result[0] = FastMath.clamp(result[0], 0.0f, 1.0f);
				result[1] = FastMath.clamp(result[1], 0.0f, 1.0f);
				result[2] = FastMath.clamp(result[2], 0.0f, 1.0f);
				break;
			case MTEX_ADD:
				textureIntensity *= textureFactor;
				result[0] = (textureIntensity * color[0] + materialColor[0]) * 0.5f;
				result[1] = (textureIntensity * color[1] + materialColor[1]) * 0.5f;
				result[2] = (textureIntensity * color[2] + materialColor[2]) * 0.5f;
				break;
			case MTEX_DIFF:
				textureIntensity *= textureFactor;
				facm = 1.0f - textureIntensity;
				result[0] = facm * color[0] + textureIntensity * Math.abs(materialColor[0] - color[0]);
				result[1] = facm * color[1] + textureIntensity * Math.abs(materialColor[1] - color[1]);
				result[2] = facm * color[2] + textureIntensity * Math.abs(materialColor[2] - color[2]);
				break;
			case MTEX_DARK:
				textureIntensity *= textureFactor;
				col = textureIntensity * color[0];
				result[0] = col < materialColor[0] ? col : materialColor[0];
				col = textureIntensity * color[1];
				result[1] = col < materialColor[1] ? col : materialColor[1];
				col = textureIntensity * color[2];
				result[2] = col < materialColor[2] ? col : materialColor[2];
				break;
			case MTEX_LIGHT:
				textureIntensity *= textureFactor;
				col = textureIntensity * color[0];
				result[0] = col > materialColor[0] ? col : materialColor[0];
				col = textureIntensity * color[1];
				result[1] = col > materialColor[1] ? col : materialColor[1];
				col = textureIntensity * color[2];
				result[2] = col > materialColor[2] ? col : materialColor[2];
				break;
			case MTEX_BLEND_HUE:
				textureIntensity *= textureFactor;
				System.arraycopy(materialColor, 0, result, 0, 3);
				this.rampBlend(MA_RAMP_HUE, result, textureIntensity, color, dataRepository);
				break;
			case MTEX_BLEND_SAT:
				textureIntensity *= textureFactor;
				System.arraycopy(materialColor, 0, result, 0, 3);
				this.rampBlend(MA_RAMP_SAT, result, textureIntensity, color, dataRepository);
				break;
			case MTEX_BLEND_VAL:
				textureIntensity *= textureFactor;
				System.arraycopy(materialColor, 0, result, 0, 3);
				this.rampBlend(MA_RAMP_VAL, result, textureIntensity, color, dataRepository);
				break;
			case MTEX_BLEND_COLOR:
				textureIntensity *= textureFactor;
				System.arraycopy(materialColor, 0, result, 0, 3);
				this.rampBlend(MA_RAMP_COLOR, result, textureIntensity, color, dataRepository);
				break;
			default:
				throw new IllegalStateException("Unknown blend type: " + blendtype);
		}
	}

	/**
	 * The method that performs the ramp blending (whatever it is :P - copied from blender sources).
	 * 
	 * @param type
	 *        the ramp type
	 * @param rgb
	 *        the rgb value where the result is stored
	 * @param fac
	 *        color affection factor
	 * @param col
	 *        the texture color
	 * @param dataRepository
	 *        the data repository
	 */
	public void rampBlend(int type, float[] rgb, float fac, float[] col, DataRepository dataRepository) {
		float tmp, facm = 1.0f - fac;
		MaterialHelper materialHelper = dataRepository.getHelper(MaterialHelper.class);

		switch (type) {
			case MA_RAMP_HUE:
				if (rgb.length == 3) {
					float[] colorTransformResult = new float[3];
					materialHelper.rgbToHsv(col[0], col[1], col[2], colorTransformResult);
					if (colorTransformResult[1] != 0.0f) {
						float colH = colorTransformResult[0];
						materialHelper.rgbToHsv(rgb[0], rgb[1], rgb[2], colorTransformResult);
						materialHelper.hsvToRgb(colH, colorTransformResult[1], colorTransformResult[2], colorTransformResult);
						rgb[0] = facm * rgb[0] + fac * colorTransformResult[0];
						rgb[1] = facm * rgb[1] + fac * colorTransformResult[1];
						rgb[2] = facm * rgb[2] + fac * colorTransformResult[2];
					}
				}
				break;
			case MA_RAMP_SAT:
				if (rgb.length == 3) {
					float[] colorTransformResult = new float[3];
					materialHelper.rgbToHsv(rgb[0], rgb[1], rgb[2], colorTransformResult);
					float rH = colorTransformResult[0];
					float rS = colorTransformResult[1];
					float rV = colorTransformResult[2];
					if (rS != 0) {
						materialHelper.rgbToHsv(col[0], col[1], col[2], colorTransformResult);
						materialHelper.hsvToRgb(rH, (facm * rS + fac * colorTransformResult[1]), rV, rgb);
					}
				}
				break;
			case MA_RAMP_VAL:
				if (rgb.length == 3) {
					float[] rgbToHsv = new float[3];
					float[] colToHsv = new float[3];
					materialHelper.rgbToHsv(rgb[0], rgb[1], rgb[2], rgbToHsv);
					materialHelper.rgbToHsv(col[0], col[1], col[2], colToHsv);
					materialHelper.hsvToRgb(rgbToHsv[0], rgbToHsv[1], (facm * rgbToHsv[2] + fac * colToHsv[2]), rgb);
				}
				break;
			case MA_RAMP_COLOR:
				if (rgb.length == 3) {
					float[] rgbToHsv = new float[3];
					float[] colToHsv = new float[3];
					materialHelper.rgbToHsv(col[0], col[1], col[2], colToHsv);
					if (colToHsv[2] != 0) {
						materialHelper.rgbToHsv(rgb[0], rgb[1], rgb[2], rgbToHsv);
						materialHelper.hsvToRgb(colToHsv[0], colToHsv[1], rgbToHsv[2], rgbToHsv);
						rgb[0] = facm * rgb[0] + fac * rgbToHsv[0];
						rgb[1] = facm * rgb[1] + fac * rgbToHsv[1];
						rgb[2] = facm * rgb[2] + fac * rgbToHsv[2];
					}
				}
				break;
			case MA_RAMP_BLEND:
				rgb[0] = facm * rgb[0] + fac * col[0];
				if (rgb.length == 3) {
					rgb[1] = facm * rgb[1] + fac * col[1];
					rgb[2] = facm * rgb[2] + fac * col[2];
				}
				break;
			case MA_RAMP_ADD:
				rgb[0] += fac * col[0];
				if (rgb.length == 3) {
					rgb[1] += fac * col[1];
					rgb[2] += fac * col[2];
				}
				break;
			case MA_RAMP_MULT:
				rgb[0] *= facm + fac * col[0];
				if (rgb.length == 3) {
					rgb[1] *= facm + fac * col[1];
					rgb[2] *= facm + fac * col[2];
				}
				break;
			case MA_RAMP_SCREEN:
				rgb[0] = 1.0f - (facm + fac * (1.0f - col[0])) * (1.0f - rgb[0]);
				if (rgb.length == 3) {
					rgb[1] = 1.0f - (facm + fac * (1.0f - col[1])) * (1.0f - rgb[1]);
					rgb[2] = 1.0f - (facm + fac * (1.0f - col[2])) * (1.0f - rgb[2]);
				}
				break;
			case MA_RAMP_OVERLAY:
				if (rgb[0] < 0.5f) {
					rgb[0] *= facm + 2.0f * fac * col[0];
				} else {
					rgb[0] = 1.0f - (facm + 2.0f * fac * (1.0f - col[0])) * (1.0f - rgb[0]);
				}
				if (rgb.length == 3) {
					if (rgb[1] < 0.5f) {
						rgb[1] *= facm + 2.0f * fac * col[1];
					} else {
						rgb[1] = 1.0f - (facm + 2.0f * fac * (1.0f - col[1])) * (1.0f - rgb[1]);
					}
					if (rgb[2] < 0.5f) {
						rgb[2] *= facm + 2.0f * fac * col[2];
					} else {
						rgb[2] = 1.0f - (facm + 2.0f * fac * (1.0f - col[2])) * (1.0f - rgb[2]);
					}
				}
				break;
			case MA_RAMP_SUB:
				rgb[0] -= fac * col[0];
				if (rgb.length == 3) {
					rgb[1] -= fac * col[1];
					rgb[2] -= fac * col[2];
				}
				break;
			case MA_RAMP_DIV:
				if (col[0] != 0.0) {
					rgb[0] = facm * rgb[0] + fac * rgb[0] / col[0];
				}
				if (rgb.length == 3) {
					if (col[1] != 0.0) {
						rgb[1] = facm * rgb[1] + fac * rgb[1] / col[1];
					}
					if (col[2] != 0.0) {
						rgb[2] = facm * rgb[2] + fac * rgb[2] / col[2];
					}
				}
				break;
			case MA_RAMP_DIFF:
				rgb[0] = facm * rgb[0] + fac * Math.abs(rgb[0] - col[0]);
				if (rgb.length == 3) {
					rgb[1] = facm * rgb[1] + fac * Math.abs(rgb[1] - col[1]);
					rgb[2] = facm * rgb[2] + fac * Math.abs(rgb[2] - col[2]);
				}
				break;
			case MA_RAMP_DARK:
				tmp = fac * col[0];
				if (tmp < rgb[0]) {
					rgb[0] = tmp;
				}
				if (rgb.length == 3) {
					tmp = fac * col[1];
					if (tmp < rgb[1]) {
						rgb[1] = tmp;
					}
					tmp = fac * col[2];
					if (tmp < rgb[2]) {
						rgb[2] = tmp;
					}
				}
				break;
			case MA_RAMP_LIGHT:
				tmp = fac * col[0];
				if (tmp > rgb[0]) {
					rgb[0] = tmp;
				}
				if (rgb.length == 3) {
					tmp = fac * col[1];
					if (tmp > rgb[1]) {
						rgb[1] = tmp;
					}
					tmp = fac * col[2];
					if (tmp > rgb[2]) {
						rgb[2] = tmp;
					}
				}
				break;
			case MA_RAMP_DODGE:
				if (rgb[0] != 0.0) {
					tmp = 1.0f - fac * col[0];
					if (tmp <= 0.0) {
						rgb[0] = 1.0f;
					} else if ((tmp = rgb[0] / tmp) > 1.0) {
						rgb[0] = 1.0f;
					} else {
						rgb[0] = tmp;
					}
				}
				if (rgb.length == 3) {
					if (rgb[1] != 0.0) {
						tmp = 1.0f - fac * col[1];
						if (tmp <= 0.0) {
							rgb[1] = 1.0f;
						} else if ((tmp = rgb[1] / tmp) > 1.0) {
							rgb[1] = 1.0f;
						} else {
							rgb[1] = tmp;
						}
					}
					if (rgb[2] != 0.0) {
						tmp = 1.0f - fac * col[2];
						if (tmp <= 0.0) {
							rgb[2] = 1.0f;
						} else if ((tmp = rgb[2] / tmp) > 1.0) {
							rgb[2] = 1.0f;
						} else {
							rgb[2] = tmp;
						}
					}

				}
				break;
			case MA_RAMP_BURN:
				tmp = facm + fac * col[0];
				if (tmp <= 0.0) {
					rgb[0] = 0.0f;
				} else if ((tmp = 1.0f - (1.0f - rgb[0]) / tmp) < 0.0) {
					rgb[0] = 0.0f;
				} else if (tmp > 1.0) {
					rgb[0] = 1.0f;
				} else {
					rgb[0] = tmp;
				}

				if (rgb.length == 3) {
					tmp = facm + fac * col[1];
					if (tmp <= 0.0) {
						rgb[1] = 0.0f;
					} else if ((tmp = 1.0f - (1.0f - rgb[1]) / tmp) < 0.0) {
						rgb[1] = 0.0f;
					} else if (tmp > 1.0) {
						rgb[1] = 1.0f;
					} else {
						rgb[1] = tmp;
					}

					tmp = facm + fac * col[2];
					if (tmp <= 0.0) {
						rgb[2] = 0.0f;
					} else if ((tmp = 1.0f - (1.0f - rgb[2]) / tmp) < 0.0) {
						rgb[2] = 0.0f;
					} else if (tmp > 1.0) {
						rgb[2] = 1.0f;
					} else {
						rgb[2] = tmp;
					}
				}
				break;
			default:
				throw new IllegalStateException("Unknown ramp type: " + type);
		}
	}

	/**
	 * This class returns a texture read from the file or from packed blender data.
	 * 
	 * @param image
	 *        image structure filled with data
	 * @param dataRepository
	 *        the data repository
	 * @return the texture that can be used by JME engine
	 * @throws BlenderFileException
	 *         this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	public Texture getTextureFromImage(Structure image, DataRepository dataRepository) throws BlenderFileException {
		Texture result = (Texture) dataRepository.getLoadedFeature(image.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
		if (result == null) {
			Pointer pPackedFile = (Pointer) image.getFieldValue("packedfile");
			if (pPackedFile.isNull()) {
				LOGGER.info("Reading texture from file!");
				String imagePath = image.getFieldValue("name").toString();
				result = this.loadTextureFromFile(imagePath, dataRepository);
			} else {
				LOGGER.info("Packed texture. Reading directly from the blend file!");
				Structure packedFile = pPackedFile.fetchData(dataRepository.getInputStream()).get(0);
				Pointer pData = (Pointer) packedFile.getFieldValue("data");
				FileBlockHeader dataFileBlock = dataRepository.getFileBlock(pData.getOldMemoryAddress());
				dataRepository.getInputStream().setPosition(dataFileBlock.getBlockPosition());
				ImageLoader imageLoader = new ImageLoader();

				// Should the texture be flipped? It works for sinbad ..
				Image im = imageLoader.loadImage(dataRepository.getInputStream(), dataFileBlock.getBlockPosition(), true);
				if (im != null) {
					result = new Texture2D(im);
				}
			}
			if (result != null) {
				result.setWrap(Texture.WrapMode.Repeat);
				dataRepository.addLoadedFeatures(image.getOldMemoryAddress(), image.getName(), image, result);
			}
		}
		return result;
	}

	/**
	 * This method loads the textre from outside the blend file.
	 * 
	 * @param name
	 *        the path to the image
	 * @param dataRepository
	 *        the data repository
	 * @return the loaded image or null if the image cannot be found
	 */
	protected Texture loadTextureFromFile(String name, DataRepository dataRepository) {
		Image image = null;
		ImageLoader imageLoader = new ImageLoader();
		FileInputStream fis = null;
		ImageType[] imageTypes = ImageType.values();
		// TODO: would be nice to have the model asset key here to getthe models older in the assetmanager

		if (name.startsWith("//")) {
			File modelFolder = new File(dataRepository.getBlenderKey().getName());
			File textureFolder = modelFolder.getParentFile();

			if (textureFolder != null) {
				name = textureFolder.getPath() + "/." + name.substring(1); // replace the // that means "relative" for blender (hopefully)
																			// with
			} else {
				name = name.substring(1);
			}

			TextureKey texKey = new TextureKey(name, false);
			Texture tex = dataRepository.getAssetManager().loadTexture(texKey);
			image = tex.getImage();
		}

		// 2. Try using the direct path from the blender file
		if (image == null) {
			File textureFile = new File(name);
			if (textureFile.exists() && textureFile.isFile()) {
				LOGGER.log(Level.INFO, "Trying with: {0}", name);
				try {
					for (int i = 0; i < imageTypes.length && image == null; ++i) {
						fis = new FileInputStream(textureFile);
						image = imageLoader.loadImage(fis, imageTypes[i], false);
						this.closeStream(fis);
					}
				} catch (FileNotFoundException e) {
					assert false : e;// this should NEVER happen
				} finally {
					this.closeStream(fis);
				}
			}
		}

		// 3. if 2 failed we start including the parent folder(s) to see if the texture
		// can be found
		if (image == null) {
			String baseName = File.separatorChar != '/' ? name.replace(File.separatorChar, '/') : name;
			int idx = baseName.lastIndexOf('/');
			while (idx != -1 && image == null) {
				String texName = baseName.substring(idx + 1);
				File textureFile = new File(texName);
				if (textureFile.exists() && textureFile.isFile()) {
					LOGGER.info("Trying with: " + texName);
					try {
						for (int i = 0; i < imageTypes.length && image == null; ++i) {
							fis = new FileInputStream(textureFile);
							image = imageLoader.loadImage(fis, imageTypes[i], false);
						}
					} catch (FileNotFoundException e) {
						assert false : e;// this should NEVER happen
					} finally {
						this.closeStream(fis);
					}
				}
				if (idx > 1) {
					idx = baseName.lastIndexOf('/', idx - 1);
				} else {
					idx = -1;
				}
			}
		}

		return image == null ? null : new Texture2D(image);
	}

	/**
	 * This method closes the given stream.
	 * 
	 * @param is
	 *        the input stream that is to be closed
	 */
	protected void closeStream(InputStream is) {
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				LOGGER.log(Level.SEVERE, e.getLocalizedMessage(), e);
			}
		}
	}

	/**
	 * An image loader class. It uses three loaders (AWTLoader, TGALoader and DDSLoader) in an attempt to load the image from the given
	 * input stream.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class ImageLoader extends AWTLoader {
		private static final Logger	LOGGER		= Logger.getLogger(ImageLoader.class.getName());

		protected DDSLoader			ddsLoader	= new DDSLoader();									// DirectX image loader

		/**
		 * This method loads the image from the blender file itself. It tries each loader to load the image.
		 * 
		 * @param inputStream
		 *        blender input stream
		 * @param startPosition
		 *        position in the stream where the image data starts
		 * @param flipY
		 *        if the image should be flipped (does not work with DirectX image)
		 * @return loaded image or null if it could not be loaded
		 */
		public Image loadImage(BlenderInputStream inputStream, int startPosition, boolean flipY) {
			// loading using AWT loader
			inputStream.setPosition(startPosition);
			Image result = this.loadImage(inputStream, ImageType.AWT, flipY);
			// loading using TGA loader
			if (result == null) {
				inputStream.setPosition(startPosition);
				result = this.loadImage(inputStream, ImageType.TGA, flipY);
			}
			// loading using DDS loader
			if (result == null) {
				inputStream.setPosition(startPosition);
				result = this.loadImage(inputStream, ImageType.DDS, flipY);
			}

			if (result == null) {
				LOGGER.warning("Image could not be loaded by none of available loaders!");
			}

			return result;
		}

		/**
		 * This method loads an image of a specified type from the given input stream.
		 * 
		 * @param inputStream
		 *        the input stream we read the image from
		 * @param imageType
		 *        the type of the image {@link ImageType}
		 * @param flipY
		 *        if the image should be flipped (does not work with DirectX image)
		 * @return loaded image or null if it could not be loaded
		 */
		public Image loadImage(InputStream inputStream, ImageType imageType, boolean flipY) {
			Image result = null;
			switch (imageType) {
				case AWT:
					try {
						result = this.load(inputStream, flipY);
					} catch (Exception e) {
						LOGGER.info("Unable to load image using AWT loader!");
					}
					break;
				case DDS:
					try {
						result = ddsLoader.load(inputStream);
					} catch (Exception e) {
						LOGGER.info("Unable to load image using DDS loader!");
					}
					break;
				case TGA:
					try {
						result = TGALoader.load(inputStream, flipY);
					} catch (Exception e) {
						LOGGER.info("Unable to load image using TGA loader!");
					}
					break;
				default:
					throw new IllegalStateException("Unknown image type: " + imageType);
			}
			return result;
		}
	}

	/**
	 * Image types that can be loaded. AWT: png, jpg, jped or bmp TGA: tga DDS: DirectX image files
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	public static enum ImageType {
		AWT, TGA, DDS;
	}

	/**
	 * The result pixel of generated texture computations;
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class TexResult implements Cloneable {
		public float	tin, tr, tg, tb, ta;
		public int		talpha;
		public float[]	nor;

		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	/**
	 * A class constaining the colorband data.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class ColorBand {
		public int		flag, tot, cur, ipotype;
		public CBData[]	data	= new CBData[32];

		/**
		 * Constructor. Loads the data from the given structure.
		 * 
		 * @param cbdataStructure
		 *        the colorband structure
		 */
		@SuppressWarnings("unchecked")
		public ColorBand(Structure colorbandStructure) {
			this.flag = ((Number) colorbandStructure.getFieldValue("flag")).intValue();
			this.tot = ((Number) colorbandStructure.getFieldValue("tot")).intValue();
			this.cur = ((Number) colorbandStructure.getFieldValue("cur")).intValue();
			this.ipotype = ((Number) colorbandStructure.getFieldValue("ipotype")).intValue();
			DynamicArray<Structure> data = (DynamicArray<Structure>) colorbandStructure.getFieldValue("data");
			for (int i = 0; i < data.getTotalSize(); ++i) {
				this.data[i] = new CBData(data.get(i));
			}
		}
	}

	/**
	 * Class to store the single colorband unit data.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	protected static class CBData implements Cloneable {
		public float	r, g, b, a, pos;
		public int		cur;

		/**
		 * Constructor. Loads the data from the given structure.
		 * 
		 * @param cbdataStructure
		 *        the structure containing the CBData object
		 */
		public CBData(Structure cbdataStructure) {
			this.r = ((Number) cbdataStructure.getFieldValue("r")).floatValue();
			this.g = ((Number) cbdataStructure.getFieldValue("g")).floatValue();
			this.b = ((Number) cbdataStructure.getFieldValue("b")).floatValue();
			this.a = ((Number) cbdataStructure.getFieldValue("a")).floatValue();
			this.pos = ((Number) cbdataStructure.getFieldValue("pos")).floatValue();
			this.cur = ((Number) cbdataStructure.getFieldValue("cur")).intValue();
		}

		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public static class GeneratedTextureData {
		public ByteBuffer	luminanceData;
		public ByteBuffer	rgbData;
		public Format		rgbFormat;
		public int			width;
		public int			height;

		public GeneratedTextureData(ByteBuffer luminanceData, ByteBuffer rgbData, Format rgbFormat, int width, int height) {
			this.luminanceData = luminanceData;
			this.rgbData = rgbData;
			this.rgbFormat = rgbFormat;
			this.width = width;
			this.height = height;
		}
	}
}
