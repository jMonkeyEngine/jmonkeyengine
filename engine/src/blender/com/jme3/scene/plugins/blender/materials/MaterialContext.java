package com.jme3.scene.plugins.blender.materials;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.texture.Texture.Type;

public final class MaterialContext {
	private static final Logger			LOGGER				= Logger.getLogger(MaterialContext.class.getName());

	/* package */final String			name;
	/* package */final List<Structure>	mTexs;
	/* package */final List<Structure>	textures;
	/* package */final int				texturesCount;
	/* package */final Type				textureType;

	/* package */final boolean			shadeless;
	/* package */final boolean			vertexColor;
	/* package */final boolean			transparent;
	/* package */final boolean			vtangent;

	/* package */int					uvCoordinatesType	= -1;
	/* package */int					projectionType;

	@SuppressWarnings("unchecked")
	/* package */MaterialContext(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
		name = structure.getName();

		int mode = ((Number) structure.getFieldValue("mode")).intValue();
		shadeless = (mode & 0x4) != 0;
		vertexColor = (mode & 0x80) != 0;
		transparent = (mode & 0x10000) != 0;
		vtangent = (mode & 0x4000000) != 0; // NOTE: Requires tangents

		mTexs = new ArrayList<Structure>();
		textures = new ArrayList<Structure>();
		DynamicArray<Pointer> mtexsArray = (DynamicArray<Pointer>) structure.getFieldValue("mtex");
		int separatedTextures = ((Number) structure.getFieldValue("septex")).intValue();
		Type firstTextureType = null;
		for (int i = 0; i < mtexsArray.getTotalSize(); ++i) {
			Pointer p = mtexsArray.get(i);
			if (p.isNotNull() && (separatedTextures & 1 << i) == 0) {
				Structure mtex = p.fetchData(blenderContext.getInputStream()).get(0);

				// the first texture determines the texture coordinates type
				if (uvCoordinatesType == -1) {
					uvCoordinatesType = ((Number) mtex.getFieldValue("texco")).intValue();
					projectionType = ((Number) mtex.getFieldValue("mapping")).intValue();
				} else if (uvCoordinatesType != ((Number) mtex.getFieldValue("texco")).intValue()) {
					LOGGER.log(Level.WARNING, "The texture with index: {0} has different UV coordinates type than the first texture! This texture will NOT be loaded!", i + 1);
					continue;
				}

				Pointer pTex = (Pointer) mtex.getFieldValue("tex");
				if (pTex.isNotNull()) {
					Structure tex = pTex.fetchData(blenderContext.getInputStream()).get(0);
					int type = ((Number) tex.getFieldValue("type")).intValue();
					Type textureType = this.getType(type);
					if (textureType != null) {
						if (firstTextureType == null) {
							firstTextureType = textureType;
							mTexs.add(mtex);
							textures.add(tex);
						} else if (firstTextureType == textureType) {
							mTexs.add(mtex);
							textures.add(tex);
						} else {
							LOGGER.log(Level.WARNING, "The texture with index: {0} is of different dimension than the first one! This texture will NOT be loaded!", i + 1);
						}
					}
				}
			}
		}

		this.texturesCount = mTexs.size();
		this.textureType = firstTextureType;
	}

	/**
	 * This method returns the current material's texture UV coordinates type.
	 * @return uv coordinates type
	 */
	public int getUvCoordinatesType() {
		return uvCoordinatesType;
	}

	/**
	 * This method returns the proper projection type for the material's texture.
	 * This applies only to 2D textures.
	 * @return texture's projection type
	 */
	public int getProjectionType() {
		return projectionType;
	}

	/**
	 * This method returns current material's texture dimension.
	 * @return the material's texture dimension
	 */
	public int getTextureDimension() {
		return this.textureType == Type.TwoDimensional ? 2 : 3;
	}

	/**
	 * This method returns the amount of textures applied for the current
	 * material.
	 * 
	 * @return the amount of textures applied for the current material
	 */
	public int getTexturesCount() {
		return textures == null ? 0 : textures.size();
	}

	/**
	 * This method returns the projection array that indicates where the current coordinate factor X, Y or Z (represented
	 * by the index in the array) will be used where (indicated by the value in the array).
	 * For example the configuration: [1,2,3] means that X - coordinate will be used as X, Y as Y and Z as Z.
	 * The configuration [2,1,0] means that Z will be used instead of X coordinate, Y will be used as Y and
	 * Z will not be used at all (0 will be in its place).
	 * @param textureIndex
	 *        the index of the texture
	 * @return texture projection array
	 */
	public int[] getProjection(int textureIndex) {
		Structure mtex = mTexs.get(textureIndex);
		return new int[] { ((Number) mtex.getFieldValue("projx")).intValue(), ((Number) mtex.getFieldValue("projy")).intValue(), ((Number) mtex.getFieldValue("projz")).intValue() };
	}

	/**
	 * This method determines the type of the texture.
	 * @param texType
	 *        texture type (from blender)
	 * @return texture type (used by jme)
	 */
	private Type getType(int texType) {
		switch (texType) {
			case TextureHelper.TEX_IMAGE:// (it is first because probably this will be most commonly used)
				return Type.TwoDimensional;
			case TextureHelper.TEX_CLOUDS:
			case TextureHelper.TEX_WOOD:
			case TextureHelper.TEX_MARBLE:
			case TextureHelper.TEX_MAGIC:
			case TextureHelper.TEX_BLEND:
			case TextureHelper.TEX_STUCCI:
			case TextureHelper.TEX_NOISE:
			case TextureHelper.TEX_MUSGRAVE:
			case TextureHelper.TEX_VORONOI:
			case TextureHelper.TEX_DISTNOISE:
				return Type.ThreeDimensional;
			case TextureHelper.TEX_NONE:// No texture, do nothing
				return null;
			case TextureHelper.TEX_POINTDENSITY:
			case TextureHelper.TEX_VOXELDATA:
			case TextureHelper.TEX_PLUGIN:
			case TextureHelper.TEX_ENVMAP:
				LOGGER.log(Level.WARNING, "Texture type NOT supported: {0}", texType);
				return null;
			default:
				throw new IllegalStateException("Unknown texture type: " + texType);
		}
	}
}
