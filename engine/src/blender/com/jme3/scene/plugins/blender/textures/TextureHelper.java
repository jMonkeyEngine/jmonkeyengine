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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3tools.converters.ImageToAwt;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.BlenderKey;
import com.jme3.asset.BlenderKey.FeaturesToLoad;
import com.jme3.asset.GeneratedTextureKey;
import com.jme3.asset.TextureKey;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.AbstractBlenderHelper;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.FileBlockHeader;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.texture.Texture3D;
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

	protected NoiseGenerator noiseGenerator;
	private Map<Integer, TextureGenerator> textureGenerators = new HashMap<Integer, TextureGenerator>();

	/**
	 * This constructor parses the given blender version and stores the result.
	 * It creates noise generator and texture generators.
	 * 
	 * @param blenderVersion
	 *        the version read from the blend file
	 * @param fixUpAxis
     *        a variable that indicates if the Y asxis is the UP axis or not
	 */
	public TextureHelper(String blenderVersion, boolean fixUpAxis) {
		super(blenderVersion, false);
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
	 * @param blenderContext
	 *        the blender context
	 * @return the texture that can be used by JME engine
	 * @throws BlenderFileException
	 *         this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	public Texture getTexture(Structure tex, BlenderContext blenderContext) throws BlenderFileException {
		Texture result = (Texture) blenderContext.getLoadedFeature(tex.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
		if (result != null) {
			return result;
		}
		int type = ((Number) tex.getFieldValue("type")).intValue();
		int width = blenderContext.getBlenderKey().getGeneratedTextureWidth();
		int height = blenderContext.getBlenderKey().getGeneratedTextureHeight();
		int depth = blenderContext.getBlenderKey().getGeneratedTextureDepth();

		switch (type) {
		case TEX_IMAGE:// (it is first because probably this will be most commonly used)
			Pointer pImage = (Pointer) tex.getFieldValue("ima");
			if (pImage.isNotNull()){
				Structure image = pImage.fetchData(blenderContext.getInputStream()).get(0);
				result = this.getTextureFromImage(image, blenderContext);
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
			result = textureGenerator.generate(tex, width, height, depth, blenderContext);
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
			// NOTE: Enable mipmaps FOR ALL TEXTURES EVER
			result.setMinFilter(MinFilter.Trilinear);
			if(type != TEX_IMAGE) {//only generated textures should have this key
				result.setKey(new GeneratedTextureKey(tex.getName()));
			}
		}
		return result;
	}

	/**
	 * This method merges the given textures. The result texture has no alpha
	 * factor (is always opaque).
	 * 
	 * @param sources
	 *            the textures to be merged
	 * @param materialContext
	 *            the context of the material
	 * @return merged textures
	 */
	public Texture mergeTextures(List<Texture> sources, MaterialContext materialContext) {
		Texture result = null;
		if(sources!=null && sources.size()>0) {
			if(sources.size() == 1) {
				return sources.get(0);//just return the texture
			}
			//checking the sizes of the textures (tehy should perfectly match)
			int lastTextureWithoutAlphaIndex = 0;
			int width = sources.get(0).getImage().getWidth();
			int height = sources.get(0).getImage().getHeight();
			int depth = sources.get(0).getImage().getDepth();
			
			for(Texture source : sources) {
				if(source.getImage().getWidth() != width) {
					throw new IllegalArgumentException("The texture " + source.getName() + " has invalid width! It should be: " + width + '!');
				}
				if(source.getImage().getHeight() != height) {
					throw new IllegalArgumentException("The texture " + source.getName() + " has invalid height! It should be: " + height + '!');
				}
				if(source.getImage().getDepth() != depth) {
					throw new IllegalArgumentException("The texture " + source.getName() + " has invalid depth! It should be: " + depth + '!');
				}
				//support for more formats is not necessary at the moment
				if(source.getImage().getFormat()!=Format.RGB8 && source.getImage().getFormat()!=Format.BGR8) {
					++lastTextureWithoutAlphaIndex;
				}
			}
			if(depth==0) {
				depth = 1;
			}
			
			//remove textures before the one without alpha (they will be covered anyway)
			if(lastTextureWithoutAlphaIndex > 0 && lastTextureWithoutAlphaIndex<sources.size()-1) {
				sources = sources.subList(lastTextureWithoutAlphaIndex, sources.size()-1);
			}
			int pixelsAmount = width * height * depth;
			
			ByteBuffer data = BufferUtils.createByteBuffer(pixelsAmount * 3);
			TexturePixel resultPixel = new TexturePixel();
			TexturePixel sourcePixel = new TexturePixel();
			ColorRGBA diffuseColor = materialContext.getDiffuseColor();
			for (int i = 0; i < pixelsAmount; ++i) {
				for (int j = 0; j < sources.size(); ++j) {
					Image image = sources.get(j).getImage();
					ByteBuffer sourceData = image.getData(0);
					if(j==0) {
						resultPixel.fromColor(diffuseColor);
						sourcePixel.fromImage(image.getFormat(), sourceData, i);
						resultPixel.merge(sourcePixel);
					} else {
						sourcePixel.fromImage(image.getFormat(), sourceData, i);
						resultPixel.merge(sourcePixel);
					}
				}
				data.put((byte)(255 * resultPixel.red));
				data.put((byte)(255 * resultPixel.green));
				data.put((byte)(255 * resultPixel.blue));
				resultPixel.clear();
			}
			
			if(depth==1) {
				result = new Texture2D(new Image(Format.RGB8, width, height, data));
			} else {
				ArrayList<ByteBuffer> arrayData = new ArrayList<ByteBuffer>(1);
				arrayData.add(data);
				result = new Texture3D(new Image(Format.RGB8, width, height, depth, arrayData));
			}
		}
		return result;
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
	 * @param blenderContext
	 *        the blender context
	 * @return the texture that can be used by JME engine
	 * @throws BlenderFileException
	 *         this exception is thrown when the blend file structure is somehow invalid or corrupted
	 */
	public Texture getTextureFromImage(Structure image, BlenderContext blenderContext) throws BlenderFileException {
		LOGGER.log(Level.FINE, "Fetching texture with OMA = {0}", image.getOldMemoryAddress());
		Texture result = (Texture) blenderContext.getLoadedFeature(image.getOldMemoryAddress(), LoadedFeatureDataType.LOADED_FEATURE);
		if (result == null) {
			String texturePath = image.getFieldValue("name").toString();
			Pointer pPackedFile = (Pointer) image.getFieldValue("packedfile");
			if (pPackedFile.isNull()) {
				LOGGER.log(Level.INFO, "Reading texture from file: {0}", texturePath);
				result = this.loadTextureFromFile(texturePath, blenderContext);
			} else {
				LOGGER.info("Packed texture. Reading directly from the blend file!");
				Structure packedFile = pPackedFile.fetchData(blenderContext.getInputStream()).get(0);
				Pointer pData = (Pointer) packedFile.getFieldValue("data");
				FileBlockHeader dataFileBlock = blenderContext.getFileBlock(pData.getOldMemoryAddress());
				blenderContext.getInputStream().setPosition(dataFileBlock.getBlockPosition());
				ImageLoader imageLoader = new ImageLoader();

				// Should the texture be flipped? It works for sinbad ..
				Image im = imageLoader.loadImage(blenderContext.getInputStream(), dataFileBlock.getBlockPosition(), true);
				if (im != null) {
					result = new Texture2D(im);
				}
			}
			if (result != null) {
				result.setName(texturePath);
				result.setWrap(Texture.WrapMode.Repeat);
				if(LOGGER.isLoggable(Level.FINE)) {
					LOGGER.log(Level.FINE, "Adding texture {0} to the loaded features with OMA = {1}", new Object[] {texturePath, image.getOldMemoryAddress()});
				}
				blenderContext.addLoadedFeatures(image.getOldMemoryAddress(), image.getName(), image, result);
			}
		}
		return result;
	}

	/**
	 * This method loads the textre from outside the blend file.
	 * 
	 * @param name
	 *        the path to the image
	 * @param blenderContext
	 *        the blender context
	 * @return the loaded image or null if the image cannot be found
	 */
	protected Texture loadTextureFromFile(String name, BlenderContext blenderContext) {
                if (!name.contains(".")){
                    return null; // no extension means not a valid image
                }
                
		AssetManager assetManager = blenderContext.getAssetManager();
		name = name.replaceAll("\\\\", "\\/");
		Texture result = null;

		List<String> assetNames = new ArrayList<String>();
		if (name.startsWith("//")) {
			String relativePath = name.substring(2);
			//augument the path with blender key path
			BlenderKey blenderKey = blenderContext.getBlenderKey();
            int idx = blenderKey.getName().lastIndexOf('/');
			String blenderAssetFolder = blenderKey.getName().substring(0, idx != -1 ? idx : 0);
			assetNames.add(blenderAssetFolder+'/'+relativePath);
		} else {//use every path from the asset name to the root (absolute path)
			String[] paths = name.split("\\/");
			StringBuilder sb = new StringBuilder(paths[paths.length-1]);//the asset name
			assetNames.add(paths[paths.length-1]);

			for(int i=paths.length-2;i>=0;--i) {
				sb.insert(0, '/');
				sb.insert(0, paths[i]);
				assetNames.add(0, sb.toString());
			}
		}

		//now try to locate the asset
		for(String assetName : assetNames) {
			try {
                TextureKey key = new TextureKey(assetName);
                key.setGenerateMips(true);
                key.setAsCube(false);
				result = assetManager.loadTexture(key);
				break;//if no exception is thrown then accept the located asset and break the loop
			} catch(AssetNotFoundException e) {
				LOGGER.fine(e.getLocalizedMessage());
			}
		}
		return result;
	}

	@Override
	public boolean shouldBeLoaded(Structure structure, BlenderContext blenderContext) {
		return (blenderContext.getBlenderKey().getFeaturesToLoad() & FeaturesToLoad.TEXTURES) != 0;
	}
}