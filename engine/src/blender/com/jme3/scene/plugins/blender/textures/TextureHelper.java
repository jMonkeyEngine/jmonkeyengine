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

import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3tools.converters.ImageToAwt;

import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.asset.GeneratedTextureKey;
import com.jme3.asset.TextureKey;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.DataRepository.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialHelper;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
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
	public static final int 	TEX_POINTDENSITY 	= 14;//v. 25+
    public static final int 	TEX_VOXELDATA 		= 15;//v. 25+
    
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

	protected NoiseGenerator noiseGenerator;
	private Map<Integer, TextureGenerator> textureGenerators = new HashMap<Integer, TextureGenerator>();
	
	/**
	 * This constructor parses the given blender version and stores the result.
	 * It creates noise generator and texture generators.
	 * 
	 * @param blenderVersion
	 *        the version read from the blend file
	 */
	public TextureHelper(String blenderVersion) {
		super(blenderVersion);
		noiseGenerator = new NoiseGenerator(blenderVersion);
		textureGenerators.put(Integer.valueOf(TEX_BLEND), new TextureGeneratorBlend(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_CLOUDS), new TextureGeneratorClouds(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_DISTNOISE), new TextureGeneratorDistnoise(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_MAGIC), new TextureGeneratorMagic(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_MARBLE), new TextureGeneratorMarble(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_MUSGRAVE), new TextureGeneratorMusgrave(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_NOISE), new TextureGeneratorNoise(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_STUCCI), new TextureGeneratorStucci(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_VORONOI), new TextureGeneratorVoronoi(noiseGenerator));
		textureGenerators.put(Integer.valueOf(TEX_WOOD), new TextureGeneratorWood(noiseGenerator));
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
			case TEX_IMAGE:// (it is first because probably this will be most commonly used)
				Pointer pImage = (Pointer) tex.getFieldValue("ima");
                if (pImage.isNotNull()){
                    Structure image = pImage.fetchData(dataRepository.getInputStream()).get(0);
                    result = this.getTextureFromImage(image, dataRepository);
                }
				break;
			case TEX_CLOUDS:
			case TEX_WOOD:
			case TEX_MARBLE:
			case TEX_MAGIC:
			case TEX_BLEND:
			case TEX_STUCCI:
			case TEX_NOISE:
			case TEX_MUSGRAVE:
			case TEX_VORONOI:
			case TEX_DISTNOISE:
				TextureGenerator textureGenerator = textureGenerators.get(Integer.valueOf(type));
				result = textureGenerator.generate(tex, width, height, dataRepository);
				break;
			case TEX_NONE:// No texture, do nothing
				break;
			case TEX_POINTDENSITY:
                LOGGER.warning("Point density texture loading currently not supported!");
                break;
            case TEX_VOXELDATA:
                LOGGER.warning("Voxel data texture loading currently not supported!");
                break;
			case TEX_PLUGIN:
			case TEX_ENVMAP:// TODO: implement envmap texture
				LOGGER.log(Level.WARNING, "Unsupported texture type: {0} for texture: {1}", new Object[]{type, tex.getName()});
				break;
			default:
				throw new BlenderFileException("Unknown texture type: " + type + " for texture: " + tex.getName());
		}
		if (result != null) {
			result.setName(tex.getName());
			result.setWrap(WrapMode.Repeat);
			if(type != TEX_IMAGE) {//only generated textures should have this key
				result.setKey(new GeneratedTextureKey(tex.getName()));
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
		ByteBuffer newData = BufferUtils.createByteBuffer(width * height * 4);

		float[] resultPixel = new float[4];
		int dataIndex = 0;
		while (data.hasRemaining()) {
			float tin = this.setupMaterialColor(data, format, neg, materialColorClone);
			this.blendPixel(resultPixel, materialColorClone, color, tin, affectFactor, blendType, dataRepository);
			newData.put(dataIndex++, (byte) (resultPixel[0] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[1] * 255.0f));
			newData.put(dataIndex++, (byte) (resultPixel[2] * 255.0f));
                        newData.put(dataIndex++, (byte) (1.0 * 255.0f));
		}
		return new Texture2D(new Image(Format.RGBA8, width, height, newData));
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
				LOGGER.log(Level.WARNING, "Image type not yet supported for blending: {0}", imageFormat);
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
	protected void blendPixel(float[] result, float[] materialColor, float[] color, float textureIntensity, float textureFactor, int blendtype, DataRepository dataRepository) {
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
	protected void rampBlend(int type, float[] rgb, float fac, float[] col, DataRepository dataRepository) {
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
	 * This method converts the given texture into normal-map texture.
	 * @param source
	 *        the source texture
	 * @param strengthFactor
	 *        the normal strength factor
	 * @return normal-map texture
	 */
	public Texture convertToNormalMapTexture(Texture source, float strengthFactor) {
		Image image = source.getImage();
		BufferedImage sourceImage = ImageToAwt.convert(image, false, false, 0);
		BufferedImage heightMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		BufferedImage bumpMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
		ColorConvertOp gscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		gscale.filter(sourceImage, heightMap);

		Vector3f S = new Vector3f();
		Vector3f T = new Vector3f();
		Vector3f N = new Vector3f();

		for (int x = 0; x < bumpMap.getWidth(); ++x) {
			for (int y = 0; y < bumpMap.getHeight(); ++y) {
				// generating bump pixel
				S.x = 1;
				S.y = 0;
				S.z = strengthFactor * this.getHeight(heightMap, x + 1, y) - strengthFactor * this.getHeight(heightMap, x - 1, y);
				T.x = 0;
				T.y = 1;
				T.z = strengthFactor * this.getHeight(heightMap, x, y + 1) - strengthFactor * this.getHeight(heightMap, x, y - 1);

				float den = (float) Math.sqrt(S.z * S.z + T.z * T.z + 1);
				N.x = -S.z;
				N.y = -T.z;
				N.z = 1;
				N.divideLocal(den);

				// setting thge pixel in the result image
				bumpMap.setRGB(x, y, this.vectorToColor(N.x, N.y, N.z));
			}
		}
		ByteBuffer byteBuffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 3);
		ImageToAwt.convert(bumpMap, Format.RGB8, byteBuffer);
		return new Texture2D(new Image(Format.RGB8, image.getWidth(), image.getHeight(), byteBuffer));
	}

	/**
	 * This method returns the height represented by the specified pixel in the given texture.
	 * The given texture should be a height-map.
	 * @param image
	 *        the height-map texture
	 * @param x
	 *        pixel's X coordinate
	 * @param y
	 *        pixel's Y coordinate
	 * @return height reprezented by the given texture in the specified location
	 */
	protected int getHeight(BufferedImage image, int x, int y) {
		if (x < 0) {
			x = 0;
		} else if (x >= image.getWidth()) {
			x = image.getWidth() - 1;
		}
		if (y < 0) {
			y = 0;
		} else if (y >= image.getHeight()) {
			y = image.getHeight() - 1;
		}
		return image.getRGB(x, y) & 0xff;
	}
    
	/**
	 * This method transforms given vector's coordinates into ARGB color (A is always = 255).
	 * @param x X factor of the vector
	 * @param y Y factor of the vector
	 * @param z Z factor of the vector
	 * @return color representation of the given vector
	 */
    protected int vectorToColor(float x, float y, float z) {
        int r = Math.round(255 * (x + 1f) / 2f);
        int g = Math.round(255 * (y + 1f) / 2f);
        int b = Math.round(255 * (z + 1f) / 2f);
        return (255 << 24) + (r << 16) + (g << 8) + b;
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
			String texturePath = image.getFieldValue("name").toString();
			Pointer pPackedFile = (Pointer) image.getFieldValue("packedfile");
			if (pPackedFile.isNull()) {
				LOGGER.info("Reading texture from file!");
				result = this.loadTextureFromFile(texturePath, dataRepository);
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
				result.setName(texturePath);
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
		BufferedInputStream bis = null;
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

			TextureKey texKey = new TextureKey(name, true);
			try {
				Texture tex = dataRepository.getAssetManager().loadTexture(texKey);
				image = tex.getImage();
			} catch (AssetNotFoundException e) {
				LOGGER.log(Level.WARNING, "Asset not found: {0}", e.getLocalizedMessage());
			}
		}

		// 2. Try using the direct path from the blender file
		if (image == null) {
			File textureFile = new File(name);
			if (textureFile.exists() && textureFile.isFile()) {
				LOGGER.log(Level.INFO, "Trying with: {0}", name);
				try {
					for (int i = 0; i < imageTypes.length && image == null; ++i) {
						FileInputStream fis = new FileInputStream(textureFile);
						bis = new BufferedInputStream(fis);
						image = imageLoader.loadImage(bis, imageTypes[i], false);
						this.closeStream(fis);
					}
				} catch (FileNotFoundException e) {
					assert false : e;// this should NEVER happen
				} finally {
					this.closeStream(bis);
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
							FileInputStream fis = new FileInputStream(textureFile);
							bis = new BufferedInputStream(fis);
							image = imageLoader.loadImage(bis, imageTypes[i], false);
						}
					} catch (FileNotFoundException e) {
						assert false : e;// this should NEVER happen
					} finally {
						this.closeStream(bis);
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
	
	@Override
	public boolean shouldBeLoaded(Structure structure, DataRepository dataRepository) {
		return (dataRepository.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.TEXTURES) != 0;
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
}