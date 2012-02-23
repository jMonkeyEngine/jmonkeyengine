package com.jme3.scene.plugins.blender.materials;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.exceptions.BlenderFileException;
import com.jme3.scene.plugins.blender.file.DynamicArray;
import com.jme3.scene.plugins.blender.file.Pointer;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialHelper.DiffuseShader;
import com.jme3.scene.plugins.blender.materials.MaterialHelper.SpecularShader;
import com.jme3.scene.plugins.blender.textures.TextureHelper;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlender;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlenderFactory;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.Type;
import com.jme3.texture.Texture.WrapMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class holds the data about the material.
 * @author Marcin Roguski (Kaelthas)
 */
public final class MaterialContext {
	private static final Logger			LOGGER				= Logger.getLogger(MaterialContext.class.getName());

	//texture mapping types
	public static final int				MTEX_COL = 0x01;
	public static final int				MTEX_NOR = 0x02;
	public static final int				MTEX_SPEC = 0x04;
	public static final int				MTEX_EMIT = 0x40;
	public static final int				MTEX_ALPHA = 0x80;
	
	/* package */final String			name;
	/* package */final List<Structure>	mTexs;
	/* package */final List<Structure>	textures;
	/* package */final Map<Number, Texture> loadedTextures;
	/* package */final Map<Texture, Structure> textureToMTexMap;
	/* package */final int				texturesCount;
	/* package */final Type				textureType;

	/* package */final ColorRGBA		diffuseColor;
	/* package */final DiffuseShader 	diffuseShader;
	/* package */final SpecularShader 	specularShader;
	/* package */final ColorRGBA		specularColor;
	/* package */final ColorRGBA		ambientColor;
	/* package */final float 			shininess;
	/* package */final boolean			shadeless;
	/* package */final boolean			vertexColor;
	/* package */final boolean			transparent;
	/* package */final boolean			vTangent;

	/* package */int					uvCoordinatesType	= -1;
	/* package */int					projectionType;

	@SuppressWarnings("unchecked")
	/* package */MaterialContext(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
		name = structure.getName();

		int mode = ((Number) structure.getFieldValue("mode")).intValue();
		shadeless = (mode & 0x4) != 0;
		vertexColor = (mode & 0x80) != 0;
		vTangent = (mode & 0x4000000) != 0; // NOTE: Requires tangents

		int diff_shader = ((Number) structure.getFieldValue("diff_shader")).intValue();
		diffuseShader = DiffuseShader.values()[diff_shader];
		
		if(this.shadeless) {
            float r = ((Number) structure.getFieldValue("r")).floatValue();
            float g = ((Number) structure.getFieldValue("g")).floatValue();
            float b = ((Number) structure.getFieldValue("b")).floatValue();
            float alpha = ((Number) structure.getFieldValue("alpha")).floatValue();

			diffuseColor = new ColorRGBA(r, g, b, alpha);
			specularShader = null;
			specularColor = ambientColor = null;
			shininess = 0.0f;
		} else {
			diffuseColor = this.readDiffuseColor(structure, diffuseShader);
			
			int spec_shader = ((Number) structure.getFieldValue("spec_shader")).intValue();
			specularShader = SpecularShader.values()[spec_shader];
			specularColor = this.readSpecularColor(structure, specularShader);
			
			float r = ((Number) structure.getFieldValue("ambr")).floatValue();
			float g = ((Number) structure.getFieldValue("ambg")).floatValue();
			float b = ((Number) structure.getFieldValue("ambb")).floatValue();
			float alpha = ((Number) structure.getFieldValue("alpha")).floatValue();
			ambientColor = new ColorRGBA(r, g, b, alpha);
			
			float shininess = ((Number) structure.getFieldValue("emit")).floatValue();
			this.shininess = shininess > 0.0f ? shininess : MaterialHelper.DEFAULT_SHININESS;
		}
		
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
		
		//loading the textures and merging them
		Map<Number, List<Structure[]>> sortedTextures = this.sortAndFilterTextures();
		loadedTextures = new HashMap<Number, Texture>(sortedTextures.size());
		textureToMTexMap = new HashMap<Texture, Structure>();
		float[] diffuseColorArray = new float[] {diffuseColor.r, diffuseColor.g, diffuseColor.b, diffuseColor.a};
		TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
		for(Entry<Number, List<Structure[]>> entry : sortedTextures.entrySet()) {
			if(entry.getValue().size()>0) {
				List<Texture> textures = new ArrayList<Texture>(entry.getValue().size());
				for(Structure[] mtexAndTex : entry.getValue()) {
					int texflag = ((Number) mtexAndTex[0].getFieldValue("texflag")).intValue();
					boolean negateTexture = (texflag & 0x04) != 0;
					Texture texture = textureHelper.getTexture(mtexAndTex[1], blenderContext);
					int blendType = ((Number) mtexAndTex[0].getFieldValue("blendtype")).intValue();
					float[] color = new float[] { ((Number) mtexAndTex[0].getFieldValue("r")).floatValue(), 
												  ((Number) mtexAndTex[0].getFieldValue("g")).floatValue(), 
												  ((Number) mtexAndTex[0].getFieldValue("b")).floatValue() };
					float colfac = ((Number) mtexAndTex[0].getFieldValue("colfac")).floatValue();
					TextureBlender textureBlender = TextureBlenderFactory.createTextureBlender(texture.getImage().getFormat());
					texture = textureBlender.blend(diffuseColorArray, texture, color, colfac, blendType, negateTexture, blenderContext);
					texture.setWrap(WrapMode.Repeat);
					textures.add(texture);
					textureToMTexMap.put(texture, mtexAndTex[0]);
				}
				loadedTextures.put(entry.getKey(), textureHelper.mergeTextures(textures, this));
			}
		}

		this.texturesCount = mTexs.size();
		this.textureType = firstTextureType;
		
		//veryfying if  the transparency is present
		//(in blender transparent mask is 0x10000 but its better to verify it because blender can indicate transparency when
		//it is not required
		boolean transparent = false;
		if(diffuseColor != null) {
			transparent = diffuseColor.a < 1.0f;
			if(sortedTextures.size() > 0) {//texutre covers the material color
				diffuseColor.set(1, 1, 1, 1);
			}
		}
		if(specularColor != null) {
			transparent = transparent || specularColor.a < 1.0f;
		}
		if(ambientColor != null) {
			transparent = transparent || ambientColor.a < 1.0f;
		}
		this.transparent = transparent;
	}
	
	/**
	 * This method sorts the textures by their mapping type.
	 * In each group only textures of one type are put (either two- or three-dimensional).
	 * If the mapping type is MTEX_COL then if the texture has no alpha channel then all textures before it are
	 * discarded and will not be loaded and merged because texture with no alpha will cover them anyway.
	 * @return a map with sorted and filtered textures
	 */
	private Map<Number, List<Structure[]>> sortAndFilterTextures() {
		Map<Number, List<Structure[]>> result = new HashMap<Number, List<Structure[]>>();
		for (int i = 0; i < mTexs.size(); ++i) {
			Structure mTex = mTexs.get(i);
			Structure texture  = textures.get(i);
			Number mapto = (Number) mTex.getFieldValue("mapto");
			List<Structure[]> mtexs = result.get(mapto);
			if(mtexs==null) {
				mtexs = new ArrayList<Structure[]>();
				result.put(mapto, mtexs);
			}
			if(mapto.intValue() == MTEX_COL && this.isWithoutAlpha(textures.get(i))) {
				mtexs.clear();//remove previous textures, they will be covered anyway
			}
			mtexs.add(new Structure[] {mTex, texture});
		}
		return result;
	}
	
	/**
	 * This method determines if the given texture has no alpha channel.
	 * 
	 * @param texture
	 *            the texture to check for alpha channel
	 * @return <b>true</b> if the texture has no alpha channel and <b>false</b>
	 *         otherwise
	 */
	private boolean isWithoutAlpha(Structure texture) {
		int flag = ((Number) texture.getFieldValue("flag")).intValue();
		if((flag & 0x01) == 0) {//the texture has no colorband
			int type = ((Number) texture.getFieldValue("type")).intValue();
			if(type==TextureHelper.TEX_MAGIC) {
				return true;
			}
			if(type==TextureHelper.TEX_VORONOI) {
				int voronoiColorType = ((Number) texture.getFieldValue("vn_coltype")).intValue();
				return voronoiColorType != 0;//voronoiColorType == 0: intensity, voronoiColorType != 0: col1, col2 or col3
			}
			if(type==TextureHelper.TEX_CLOUDS) {
				int sType = ((Number) texture.getFieldValue("stype")).intValue();
				return sType == 1;//sType==0: without colors, sType==1: with colors
			}
		}
		return false;
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
	 * This method returns the diffuse color.
	 * 
	 * @param materialStructure the material structure
	 * @param diffuseShader the diffuse shader
	 * @return the diffuse color
	 */
	private ColorRGBA readDiffuseColor(Structure materialStructure, DiffuseShader diffuseShader) {
		// bitwise 'or' of all textures mappings
		int commonMapto = ((Number) materialStructure.getFieldValue("mapto")).intValue();

		// diffuse color
		float r = ((Number) materialStructure.getFieldValue("r")).floatValue();
		float g = ((Number) materialStructure.getFieldValue("g")).floatValue();
		float b = ((Number) materialStructure.getFieldValue("b")).floatValue();
		float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
		if ((commonMapto & 0x01) == 0x01) {// Col
			return new ColorRGBA(r, g, b, alpha);
		} else {
			switch (diffuseShader) {
				case FRESNEL:
				case ORENNAYAR:
				case TOON:
					break;// TODO: find what is the proper modification
				case MINNAERT:
				case LAMBERT:// TODO: check if that is correct
					float ref = ((Number) materialStructure.getFieldValue("ref")).floatValue();
					r *= ref;
					g *= ref;
					b *= ref;
					break;
				default:
					throw new IllegalStateException("Unknown diffuse shader type: " + diffuseShader.toString());
			}
			return new ColorRGBA(r, g, b, alpha);
		}
	}

	/**
	 * This method returns a specular color used by the material.
	 * 
	 * @param materialStructure
	 *        the material structure filled with data
	 * @return a specular color used by the material
	 */
	private ColorRGBA readSpecularColor(Structure materialStructure, SpecularShader specularShader) {
		float r = ((Number) materialStructure.getFieldValue("specr")).floatValue();
		float g = ((Number) materialStructure.getFieldValue("specg")).floatValue();
		float b = ((Number) materialStructure.getFieldValue("specb")).floatValue();
		float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
		switch (specularShader) {
			case BLINN:
			case COOKTORRENCE:
			case TOON:
			case WARDISO:// TODO: find what is the proper modification
				break;
			case PHONG:// TODO: check if that is correct
				float spec = ((Number) materialStructure.getFieldValue("spec")).floatValue();
				r *= spec * 0.5f;
				g *= spec * 0.5f;
				b *= spec * 0.5f;
				break;
			default:
				throw new IllegalStateException("Unknown specular shader type: " + specularShader.toString());
		}
		return new ColorRGBA(r, g, b, alpha);
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
	
	/**
	 * @return he material's name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @return a copy of diffuse color
	 */
	public ColorRGBA getDiffuseColor() {
		return diffuseColor.clone();
	}
	
	/**
	 * @return an enum describing the type of a diffuse shader used by this material
	 */
	public DiffuseShader getDiffuseShader() {
		return diffuseShader;
	}
	
	/**
	 * @return a copy of specular color
	 */
	public ColorRGBA getSpecularColor() {
		return specularColor.clone();
	}
	
	/**
	 * @return an enum describing the type of a specular shader used by this material
	 */
	public SpecularShader getSpecularShader() {
		return specularShader;
	}
	
	/**
	 * @return an ambient color used by the material
	 */
	public ColorRGBA getAmbientColor() {
		return ambientColor;
	}
	
	/**
	 * @return the sihiness of this material
	 */
	public float getShininess() {
		return shininess;
	}
	
	/**
	 * @return <b>true</b> if the material is shadeless and <b>false</b> otherwise
	 */
	public boolean isShadeless() {
		return shadeless;
	}
	
	/**
	 * @return <b>true</b> if the material uses vertex color and <b>false</b> otherwise
	 */
	public boolean isVertexColor() {
		return vertexColor;
	}
	
	/**
	 * @return <b>true</b> if the material is transparent and <b>false</b> otherwise
	 */
	public boolean isTransparent() {
		return transparent;
	}
	
	/**
	 * @return <b>true</b> if the material uses tangents and <b>false</b> otherwise
	 */
	public boolean isvTangent() {
		return vTangent;
	}
	
	/**
	 * @param texture
	 *            the texture for which its mtex structure definition will be
	 *            fetched
	 * @return mtex structure of the current texture or <b>null</b> if none
	 *         exists
	 */
	public Structure getMTex(Texture texture) {
		return textureToMTexMap.get(texture);
	}
}
