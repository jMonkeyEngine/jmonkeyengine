package com.jme3.scene.plugins.blender.textures;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.textures.TriangulatedTexture.TriangleTextureElement;
import com.jme3.scene.plugins.blender.textures.UVCoordinatesGenerator.UVCoordinatesType;
import com.jme3.scene.plugins.blender.textures.generating.TextureGenerator;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * The generated texture loaded from blender file. The texture is not generated
 * after being read. This class rather stores all required data and can compute
 * a pixel in the required 3D space position.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class GeneratedTexture extends Texture {
	// flag values
	public static final int			TEX_COLORBAND		= 1;
	public static final int			TEX_FLIPBLEND		= 2;
	public static final int			TEX_NEGALPHA		= 4;
	public static final int			TEX_CHECKER_ODD		= 8;
	public static final int			TEX_CHECKER_EVEN	= 16;
	public static final int			TEX_PRV_ALPHA		= 32;
	public static final int			TEX_PRV_NOR			= 64;
	public static final int			TEX_REPEAT_XMIR		= 128;
	public static final int			TEX_REPEAT_YMIR		= 256;
	public static final int			TEX_FLAG_MASK		= TEX_COLORBAND | TEX_FLIPBLEND | TEX_NEGALPHA | TEX_CHECKER_ODD | TEX_CHECKER_EVEN | TEX_PRV_ALPHA | TEX_PRV_NOR | TEX_REPEAT_XMIR | TEX_REPEAT_YMIR;

	/** Material-texture link structure. */
	private final Structure			mTex;
	/** Texture generateo for the specified texture type. */
	private final TextureGenerator	textureGenerator;

	/**
	 * Constructor. Reads the required data from the 'tex' structure.
	 * 
	 * @param tex
	 *            the texture structure
	 * @param mTex
	 *            the material-texture link data structure
	 * @param textureGenerator
	 *            the generator for the required texture type
	 * @param blenderContext
	 *            the blender context
	 */
	public GeneratedTexture(Structure tex, Structure mTex, TextureGenerator textureGenerator, BlenderContext blenderContext) {
		this.mTex = mTex;
		this.textureGenerator = textureGenerator;
		this.textureGenerator.readData(tex, blenderContext);
		super.setImage(new GeneratedTextureImage(textureGenerator.getImageFormat()));
	}

	/**
	 * This method computes the textyre color/intensity at the specified (u, v,
	 * s) position in 3D space.
	 * 
	 * @param pixel
	 *            the pixel where the result is stored
	 * @param u
	 *            the U factor
	 * @param v
	 *            the V factor
	 * @param s
	 *            the S factor
	 */
	public void getPixel(TexturePixel pixel, float u, float v, float s) {
		textureGenerator.getPixel(pixel, u, v, s);
	}

	/**
	 * This method triangulates the texture. In the result we get a set of small
	 * flat textures for each face of the given mesh. This can be later merged
	 * into one flat texture.
	 * 
	 * @param mesh
	 *            the mesh we create the texture for
	 * @param geometriesOMA
	 *            the old memory address of the geometries group that the given
	 *            mesh belongs to (required for bounding box calculations)
	 * @param coordinatesType
	 *            the types of UV coordinates
	 * @param blenderContext
	 *            the blender context
	 * @return triangulated texture
	 */
	@SuppressWarnings("unchecked")
	public TriangulatedTexture triangulate(Mesh mesh, Long geometriesOMA, UVCoordinatesType coordinatesType, BlenderContext blenderContext) {
		List<Geometry> geometries = (List<Geometry>) blenderContext.getLoadedFeature(geometriesOMA, LoadedFeatureDataType.LOADED_FEATURE);

		int[] coordinatesSwappingIndexes = new int[] { ((Number) mTex.getFieldValue("projx")).intValue(), ((Number) mTex.getFieldValue("projy")).intValue(), ((Number) mTex.getFieldValue("projz")).intValue() };
		List<Vector3f> uvs = UVCoordinatesGenerator.generateUVCoordinatesFor3DTexture(mesh, coordinatesType, coordinatesSwappingIndexes, geometries);
		Vector3f[] uvsArray = uvs.toArray(new Vector3f[uvs.size()]);
		BoundingBox boundingBox = UVCoordinatesGenerator.getBoundingBox(geometries);
		Set<TriangleTextureElement> triangleTextureElements = new TreeSet<TriangleTextureElement>(new Comparator<TriangleTextureElement>() {
			public int compare(TriangleTextureElement o1, TriangleTextureElement o2) {
				return o1.faceIndex - o2.faceIndex;
			}
		});
		int[] indices = new int[3];
		for (int i = 0; i < mesh.getTriangleCount(); ++i) {
			mesh.getTriangle(i, indices);
			triangleTextureElements.add(new TriangleTextureElement(i, boundingBox, this, uvsArray, indices, blenderContext));
		}
		return new TriangulatedTexture(triangleTextureElements, blenderContext);
	}

	@Override
	public void setWrap(WrapAxis axis, WrapMode mode) {
	}

	@Override
	public void setWrap(WrapMode mode) {
	}

	@Override
	public WrapMode getWrap(WrapAxis axis) {
		return null;
	}

	@Override
	public Type getType() {
		return Type.ThreeDimensional;
	}

	@Override
	public Texture createSimpleClone() {
		return null;
	}

	/**
	 * Private class to give the format of the 'virtual' 3D texture image.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static class GeneratedTextureImage extends Image {
		public GeneratedTextureImage(Format imageFormat) {
			super.format = imageFormat;
		}
	}
}
