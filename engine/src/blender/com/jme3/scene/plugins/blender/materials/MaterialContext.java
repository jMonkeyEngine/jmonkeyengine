package com.jme3.scene.plugins.blender.materials;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.scene.plugins.blender.DataRepository;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.texture.Texture.Type;

/*package*/final class MaterialContext {
	private static final Logger LOGGER = Logger.getLogger(MaterialContext.class.getName());
	
	public final String name;
	public final List<Structure> mTexs;
	public final List<Structure> textures;
	public final int texturesCount;
	public final Type textureType;
	public final int textureCoordinatesType;
	
	public final boolean shadeless;
	public final boolean vertexColor;
	public final boolean transparent;
	public final boolean vtangent;
	
	@SuppressWarnings("unchecked")
	public MaterialContext(Structure structure, DataRepository dataRepository) throws BlenderFileException {
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
		int texco = -1;
		for (int i = 0; i < mtexsArray.getTotalSize(); ++i) {
			Pointer p = mtexsArray.get(i);
			if (p.isNotNull() && (separatedTextures & 1 << i) == 0) {
				Structure mtex = p.fetchData(dataRepository.getInputStream()).get(0);
				
				//the first texture determines the texture coordinates type
				if(texco == -1) {
					texco = ((Number) mtex.getFieldValue("texco")).intValue();
				} else if(texco != ((Number) mtex.getFieldValue("texco")).intValue()) {
					LOGGER.log(Level.WARNING, "The texture with index: {0} has different UV coordinates type than the first texture! This texture will NOT be loaded!", i+1);
					continue;
				}
				
				Pointer pTex = (Pointer) mtex.getFieldValue("tex");
				if(pTex.isNotNull()) {
					Structure tex = pTex.fetchData(dataRepository.getInputStream()).get(0);
					int type = ((Number) tex.getFieldValue("type")).intValue();
					Type textureType = this.getType(type);
					if(textureType != null) {
						if(firstTextureType == null) {
							firstTextureType = textureType;
							mTexs.add(mtex);
							textures.add(tex);
						} else if(firstTextureType == textureType) {
							mTexs.add(mtex);
							textures.add(tex);
						} else {
							LOGGER.log(Level.WARNING, "The texture with index: {0} is of different dimension than the first one! This texture will NOT be loaded!", i+1);
						}
					}
				}
			}
		}
		
		this.texturesCount = mTexs.size();
		this.textureCoordinatesType = texco;
		this.textureType = firstTextureType;
	}
	
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
