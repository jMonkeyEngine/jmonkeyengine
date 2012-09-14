package com.jme3.scene.plugins.blender.textures;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import jme3tools.converters.ImageToAwt;

import com.jme3.math.Vector2f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.BlenderContext.LoadedFeatureDataType;
import com.jme3.scene.plugins.blender.file.Structure;
import com.jme3.scene.plugins.blender.materials.MaterialContext;
import com.jme3.scene.plugins.blender.textures.TriangulatedTexture.TriangleTextureElement;
import com.jme3.scene.plugins.blender.textures.UVCoordinatesGenerator.UVCoordinatesType;
import com.jme3.scene.plugins.blender.textures.UVProjectionGenerator.UVProjectionType;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlender;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlenderFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;

/**
 * This class represents a texture that is defined for the material. It can be
 * made of several textures (both 2D and 3D) that are merged together and
 * returned as a single texture.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class CombinedTexture {
	private static final Logger LOGGER = Logger.getLogger(CombinedTexture.class.getName());
	
	/** The mapping type of the texture. Defined bu MaterialContext.MTEX_COL, MTEX_NOR etc. */
	private final int mappingType;
	/** The data for each of the textures. */
	private List<TextureData>	textureDatas	= new ArrayList<TextureData>();
	/** The variable indicates if the texture was already triangulated or not. */
	private boolean				wasTriangulated;
	/** The result texture. */
	private Texture				resultTexture;
	/** The UV values for the result texture. */
	private List<Vector2f>		resultUVS;

	/**
	 * Constructor. Stores the texture mapping type (ie. color map, normal map).
	 * 
	 * @param mappingType
	 *            texture mapping type
	 */
	public CombinedTexture(int mappingType) {
		this.mappingType = mappingType;
	}
	
	/**
	 * This method adds a texture data to the resulting texture.
	 * 
	 * @param texture
	 *            the source texture
	 * @param textureBlender
	 *            the texture blender (to mix the texture with its material
	 *            color)
	 * @param uvCoordinatesType
	 *            the type of UV coordinates
	 * @param projectionType
	 *            the type of UV coordinates projection (for flat textures)
	 * @param textureStructure
	 *            the texture sructure
	 * @param blenderContext
	 *            the blender context
	 */
	public void add(Texture texture, TextureBlender textureBlender, int uvCoordinatesType, int projectionType, Structure textureStructure, BlenderContext blenderContext) {
		if (!(texture instanceof GeneratedTexture) && !(texture instanceof Texture2D)) {
			throw new IllegalArgumentException("Unsupported texture type: " + (texture == null ? "null" : texture.getClass()));
		}
		if(!(texture instanceof GeneratedTexture) || blenderContext.getBlenderKey().isLoadGeneratedTextures()) {
			if(UVCoordinatesGenerator.isTextureCoordinateTypeSupported(UVCoordinatesType.valueOf(uvCoordinatesType))) {
				TextureData textureData = new TextureData();
				textureData.texture = texture;
				textureData.textureBlender = textureBlender;
				textureData.uvCoordinatesType = UVCoordinatesType.valueOf(uvCoordinatesType);
				textureData.projectionType = UVProjectionType.valueOf(projectionType);
				textureData.textureStructure = textureStructure;
	
				if (textureDatas.size() > 0 && this.isWithoutAlpha(textureData, blenderContext)) {
					textureDatas.clear();// clear previous textures, they will be covered anyway
				}
				textureDatas.add(textureData);
			} else {
				LOGGER.warning("The texture coordinates type is not supported: " + UVCoordinatesType.valueOf(uvCoordinatesType) + ". The texture '" + textureStructure.getName() + "'.");
			}
		}
	}

	/**
	 * This method flattens the texture and creates a single result of Texture2D
	 * type.
	 * 
	 * @param geometry
	 *            the geometry the texture is created for
	 * @param geometriesOMA
	 *            the old memory address of the geometries list that the given
	 *            geometry belongs to (needed for bounding box creation)
	 * @param userDefinedUVCoordinates
	 *            the UV's defined by user (null or zero length table if none
	 *            were defined)
	 * @param blenderContext
	 *            the blender context
	 */
	@SuppressWarnings("unchecked")
	public void flatten(Geometry geometry, Long geometriesOMA, List<Vector2f> userDefinedUVCoordinates, BlenderContext blenderContext) {
		TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
		Mesh mesh = geometry.getMesh();
		Texture previousTexture = null;
		UVCoordinatesType masterUVCoordinatesType = null;
		for (TextureData textureData : textureDatas) {
			// decompress compressed textures (all will be merged into one texture anyway)
			if (textureDatas.size() > 1 && textureData.texture.getImage().getFormat().isCompressed()) {
				textureData.texture.setImage(textureHelper.decompress(textureData.texture.getImage()));
				textureData.textureBlender = TextureBlenderFactory.alterTextureType(textureData.texture.getImage().getFormat(), textureData.textureBlender);
			}

			if (previousTexture == null) {// the first texture will lead the others to its shape
				if (textureData.texture instanceof GeneratedTexture) {
					resultTexture = ((GeneratedTexture) textureData.texture).triangulate(mesh, geometriesOMA, textureData.uvCoordinatesType, blenderContext);
				} else if (textureData.texture instanceof Texture2D) {
					resultTexture = textureData.texture;

					if(textureData.uvCoordinatesType == UVCoordinatesType.TEXCO_UV && userDefinedUVCoordinates != null && userDefinedUVCoordinates.size() > 0) {
						resultUVS = userDefinedUVCoordinates;
					} else {
						List<Geometry> geometries = (List<Geometry>) blenderContext.getLoadedFeature(geometriesOMA, LoadedFeatureDataType.LOADED_FEATURE);
						resultUVS = UVCoordinatesGenerator.generateUVCoordinatesFor2DTexture(mesh, textureData.uvCoordinatesType, textureData.projectionType, geometries);
					}
				}
				this.blend(resultTexture, textureData.textureBlender, blenderContext);

				previousTexture = resultTexture;
				masterUVCoordinatesType = textureData.uvCoordinatesType;
			} else {
				if (textureData.texture instanceof GeneratedTexture) {
					if (!(resultTexture instanceof TriangulatedTexture)) {
						resultTexture = new TriangulatedTexture((Texture2D) resultTexture, resultUVS, blenderContext);
						resultUVS = null;
						previousTexture = resultTexture;
					}

					TriangulatedTexture triangulatedTexture = ((GeneratedTexture) textureData.texture).triangulate(mesh, geometriesOMA, textureData.uvCoordinatesType, blenderContext);
					triangulatedTexture.castToUVS((TriangulatedTexture) resultTexture, blenderContext);
					triangulatedTexture.blend(textureData.textureBlender, (TriangulatedTexture) resultTexture, blenderContext);
					resultTexture = previousTexture = triangulatedTexture;
				} else if (textureData.texture instanceof Texture2D) {
					if (masterUVCoordinatesType == textureData.uvCoordinatesType && resultTexture instanceof Texture2D) {
						this.scale((Texture2D) textureData.texture, resultTexture.getImage().getWidth(), resultTexture.getImage().getHeight());
						this.merge((Texture2D) resultTexture, (Texture2D) textureData.texture);
						previousTexture = resultTexture;
					} else {
						if (!(resultTexture instanceof TriangulatedTexture)) {
							resultTexture = new TriangulatedTexture((Texture2D) resultTexture, resultUVS, blenderContext);
							resultUVS = null;
						}
						// first triangulate the current texture
						List<Vector2f> textureUVS = null;
						if(textureData.uvCoordinatesType == UVCoordinatesType.TEXCO_UV && userDefinedUVCoordinates != null && userDefinedUVCoordinates.size() > 0) {
							textureUVS = userDefinedUVCoordinates;
						} else {
							List<Geometry> geometries = (List<Geometry>) blenderContext.getLoadedFeature(geometriesOMA, LoadedFeatureDataType.LOADED_FEATURE);
							textureUVS = UVCoordinatesGenerator.generateUVCoordinatesFor2DTexture(mesh, textureData.uvCoordinatesType, textureData.projectionType, geometries);
						}
						TriangulatedTexture triangulatedTexture = new TriangulatedTexture((Texture2D) textureData.texture, textureUVS, blenderContext);
						// then move the texture to different UV's
						triangulatedTexture.castToUVS((TriangulatedTexture) resultTexture, blenderContext);
						((TriangulatedTexture) resultTexture).merge(triangulatedTexture);
					}
				}
			}
		}

		if (resultTexture instanceof TriangulatedTexture) {
			if(mappingType == MaterialContext.MTEX_NOR) {
				for(int i=0;i<((TriangulatedTexture) resultTexture).getFaceTextureCount();++i) {
					TriangleTextureElement triangleTextureElement = ((TriangulatedTexture) resultTexture).getFaceTextureElement(i);
					triangleTextureElement.image = textureHelper.convertToNormalMapTexture(triangleTextureElement.image, 1);//TODO: get proper strength factor
				}
			}
			resultUVS = ((TriangulatedTexture) resultTexture).getResultUVS();
			resultTexture = ((TriangulatedTexture) resultTexture).getResultTexture();
			wasTriangulated = true;
		}
		
		// setting additional data
		resultTexture.setWrap(WrapMode.Repeat);
		// the filters are required if generated textures are used because
		// otherwise ugly lines appear between the mesh faces
		resultTexture.setMagFilter(MagFilter.Nearest);
		resultTexture.setMinFilter(MinFilter.NearestNoMipMaps);
	}

	/**
	 * This method blends the texture.
	 * 
	 * @param texture
	 *            the texture to be blended
	 * @param textureBlender
	 *            blending definition for the texture
	 * @param blenderContext
	 *            the blender context
	 */
	private void blend(Texture texture, TextureBlender textureBlender, BlenderContext blenderContext) {
		if (texture instanceof TriangulatedTexture) {
			((TriangulatedTexture) texture).blend(textureBlender, null, blenderContext);
		} else if (texture instanceof Texture2D) {
			Image blendedImage = textureBlender.blend(texture.getImage(), null, blenderContext);
			texture.setImage(blendedImage);
		} else {
			throw new IllegalArgumentException("Invalid type for texture to blend!");
		}
	}

	/**
	 * This method casts the current image to the basic UV's owner UV's
	 * coordinates.
	 * 
	 * @param basicUVSOwner
	 *            the owner of the UV's we cast to
	 * @param blenderContext
	 *            the blender context
	 */
	public void castToUVS(CombinedTexture basicUVSOwner, BlenderContext blenderContext) {
		if (resultUVS.size() != basicUVSOwner.resultUVS.size()) {
			throw new IllegalStateException("The amount of UV coordinates must be equal in order to cast one UV's onto another!");
		}
		if (!resultUVS.equals(basicUVSOwner.resultUVS)) {
			if (!basicUVSOwner.wasTriangulated) {
				throw new IllegalStateException("The given texture must be triangulated!");
			}
			if (!this.wasTriangulated) {
				resultTexture = new TriangulatedTexture((Texture2D) resultTexture, resultUVS, blenderContext);
				resultUVS = ((TriangulatedTexture) resultTexture).getResultUVS();
				resultTexture = ((TriangulatedTexture) resultTexture).getResultTexture();
			}
			// casting algorithm
			TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
			ImageLoader imageLoader = new ImageLoader();
			List<TriangleTextureElement> faceTextures = new ArrayList<TriangleTextureElement>();
			List<Vector2f> basicUVS = basicUVSOwner.getResultUVS();
			int[] imageRectangle = new int[4];// minX, minY, maxX, maxY
			int[] sourceSize = new int[2], targetSize = new int[2];// width,
																	// height
			Vector2f[] destinationUVS = new Vector2f[3];
			Vector2f[] sourceUVS = new Vector2f[3];
			List<Vector2f> partImageUVS = Arrays.asList(new Vector2f(), new Vector2f(), new Vector2f());
			int faceIndex = 0;

			for (int i = 0; i < basicUVS.size(); i += 3) {
				// destination size nad UVS
				destinationUVS[0] = basicUVS.get(i);
				destinationUVS[1] = basicUVS.get(i + 1);
				destinationUVS[2] = basicUVS.get(i + 2);
				this.computeImageRectangle(destinationUVS, imageRectangle, basicUVSOwner.resultTexture.getImage().getWidth(), basicUVSOwner.resultTexture.getImage().getHeight(), blenderContext);
				targetSize[0] = imageRectangle[2] - imageRectangle[0];
				targetSize[1] = imageRectangle[3] - imageRectangle[1];
				for (int j = 0; j < 3; ++j) {
					partImageUVS.get(j).set((basicUVSOwner.resultTexture.getImage().getWidth() * destinationUVS[j].x - imageRectangle[0]) / targetSize[0],
							(basicUVSOwner.resultTexture.getImage().getHeight() * destinationUVS[j].y - imageRectangle[1]) / targetSize[1]);
				}

				// source size and UVS (translate UVS to (0,0) and stretch it to
				// the borders of the image)
				sourceUVS[0] = resultUVS.get(i);
				sourceUVS[1] = resultUVS.get(i + 1);
				sourceUVS[2] = resultUVS.get(i + 2);
				this.computeImageRectangle(sourceUVS, imageRectangle, resultTexture.getImage().getWidth(), resultTexture.getImage().getHeight(), blenderContext);
				sourceSize[0] = imageRectangle[2] - imageRectangle[0];
				sourceSize[1] = imageRectangle[3] - imageRectangle[1];
				float xTranslateFactor = imageRectangle[0] / (float) resultTexture.getImage().getWidth();
				float xStreachFactor = resultTexture.getImage().getWidth() / (float) sourceSize[0];
				float yTranslateFactor = imageRectangle[1] / (float) resultTexture.getImage().getHeight();
				float yStreachFactor = resultTexture.getImage().getHeight() / (float) sourceSize[1];
				for (int j = 0; j < 3; ++j) {
					sourceUVS[j].x = (sourceUVS[j].x - xTranslateFactor) * xStreachFactor;
					sourceUVS[j].y = (sourceUVS[j].y - yTranslateFactor) * yStreachFactor;
				}

				AffineTransform affineTransform = textureHelper.createAffineTransform(sourceUVS, partImageUVS.toArray(new Vector2f[3]), sourceSize, targetSize);

				Image image = textureHelper.getSubimage(resultTexture.getImage(), imageRectangle[0], imageRectangle[1], imageRectangle[2], imageRectangle[3]);

				// compute the result texture
				BufferedImage sourceImage = ImageToAwt.convert(image, false, true, 0);

				BufferedImage targetImage = new BufferedImage(targetSize[0], targetSize[1], sourceImage.getType());
				Graphics2D g = targetImage.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				g.drawImage(sourceImage, affineTransform, null);
				g.dispose();

				Image output = imageLoader.load(targetImage, false);
				faceTextures.add(new TriangleTextureElement(faceIndex++, output, partImageUVS, false, blenderContext));
			}
			TriangulatedTexture triangulatedTexture = new TriangulatedTexture(faceTextures, blenderContext);
			triangulatedTexture.setKeepIdenticalTextures(false);
			resultTexture = triangulatedTexture.getResultTexture();
			resultUVS = basicUVS;
		}
	}
	
	/**
	 * This method computes the rectangle of an image constrained by the
	 * triangle UV coordinates.
	 * 
	 * @param triangleVertices
	 *            the triangle UV coordinates
	 * @param result
	 *            the array where the result is stored
	 * @param totalImageWidth
	 *            the total image width
	 * @param totalImageHeight
	 *            the total image height
	 * @param blenderContext
	 *            the blender context
	 */
	private void computeImageRectangle(Vector2f[] triangleVertices, int[] result, int totalImageWidth, int totalImageHeight, BlenderContext blenderContext) {
		TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);

		float minX = Math.min(triangleVertices[0].x, triangleVertices[1].x);
		minX = Math.min(minX, triangleVertices[2].x);

		float maxX = Math.max(triangleVertices[0].x, triangleVertices[1].x);
		maxX = Math.max(maxX, triangleVertices[2].x);

		float minY = Math.min(triangleVertices[0].y, triangleVertices[1].y);
		minY = Math.min(minY, triangleVertices[2].y);

		float maxY = Math.max(triangleVertices[0].y, triangleVertices[1].y);
		maxY = Math.max(maxY, triangleVertices[2].y);

		result[0] = textureHelper.getPixelPosition(minX, totalImageWidth);
		result[1] = textureHelper.getPixelPosition(minY, totalImageHeight);
		result[2] = textureHelper.getPixelPosition(maxX, totalImageWidth);
		result[3] = textureHelper.getPixelPosition(maxY, totalImageHeight);
	}
	
	/**
	 * @return the result texture
	 */
	public Texture getResultTexture() {
		return resultTexture;
	}

	/**
	 * @return the result UV coordinates
	 */
	public List<Vector2f> getResultUVS() {
		return resultUVS;
	}
	
	/**
	 * @return the amount of added textures
	 */
	public int getTexturesCount() {
		return textureDatas.size();
	}

	/**
	 * @return <b>true</b> if the texture has at least one generated texture component and <b>false</b> otherwise
	 */
	public boolean hasGeneratedTextures() {
		if(textureDatas != null) {
			for(TextureData textureData : textureDatas) {
				if(textureData.texture instanceof GeneratedTexture) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * This method merges two given textures. The result is stored in the
	 * 'target' texture.
	 * 
	 * @param target
	 *            the target texture
	 * @param source
	 *            the source texture
	 */
	private void merge(Texture2D target, Texture2D source) {
		if(target.getImage().getDepth() != source.getImage().getDepth()) {
			throw new IllegalArgumentException("Cannot merge images with different depths!");
		}
		Image sourceImage = source.getImage();
		Image targetImage = target.getImage();
		PixelInputOutput sourceIO = PixelIOFactory.getPixelIO(sourceImage.getFormat());
		PixelInputOutput targetIO = PixelIOFactory.getPixelIO(targetImage.getFormat());
		TexturePixel sourcePixel = new TexturePixel();
		TexturePixel targetPixel = new TexturePixel();
		int depth = target.getImage().getDepth() == 0 ? 1 : target.getImage().getDepth();
		
		for (int layerIndex = 0; layerIndex < depth; ++layerIndex) {
			for (int x = 0; x < sourceImage.getWidth(); ++x) {
				for (int y = 0; y < sourceImage.getHeight(); ++y) {
					sourceIO.read(sourceImage, layerIndex, sourcePixel, x, y);
					targetIO.read(targetImage, layerIndex, targetPixel, x, y);
					targetPixel.merge(sourcePixel);
					targetIO.write(targetImage, layerIndex, targetPixel, x, y);
				}
			}
		}
	}

	/**
	 * This method determines if the given texture has no alpha channel.
	 * 
	 * @param texture
	 *            the texture to check for alpha channel
	 * @return <b>true</b> if the texture has no alpha channel and <b>false</b>
	 *         otherwise
	 */
	private boolean isWithoutAlpha(TextureData textureData, BlenderContext blenderContext) {
		ColorBand colorBand = new ColorBand(textureData.textureStructure, blenderContext);
		if (!colorBand.hasTransparencies()) {
			int type = ((Number) textureData.textureStructure.getFieldValue("type")).intValue();
			if (type == TextureHelper.TEX_MAGIC) {
				return true;
			}
			if (type == TextureHelper.TEX_VORONOI) {
				int voronoiColorType = ((Number) textureData.textureStructure.getFieldValue("vn_coltype")).intValue();
				return voronoiColorType != 0;// voronoiColorType == 0:
												// intensity, voronoiColorType
												// != 0: col1, col2 or col3
			}
			if (type == TextureHelper.TEX_CLOUDS) {
				int sType = ((Number) textureData.textureStructure.getFieldValue("stype")).intValue();
				return sType == 1;// sType==0: without colors, sType==1: with
									// colors
			}

			// checking the flat textures for alpha values presence
			if (type == TextureHelper.TEX_IMAGE) {
				Image image = textureData.texture.getImage();
				switch (image.getFormat()) {
					case BGR8:
					case DXT1:
					case Luminance16:
					case Luminance16F:
					case Luminance32F:
					case Luminance8:
					case RGB10:
					case RGB111110F:
					case RGB16:
					case RGB16F:
					case RGB32F:
					case RGB565:
					case RGB8:
						return true;// these types have no alpha by definition
					case ABGR8:
					case DXT3:
					case DXT5:
					case Luminance16Alpha16:
					case Luminance16FAlpha16F:
					case Luminance8Alpha8:
					case RGBA16:
					case RGBA16F:
					case RGBA32F:
					case RGBA8:// with these types it is better to make sure if the texture is or is not transparent
						PixelInputOutput pixelInputOutput = PixelIOFactory.getPixelIO(image.getFormat());
						TexturePixel pixel = new TexturePixel();
						int depth = image.getDepth() == 0 ? 1 : image.getDepth();
						for (int layerIndex = 0; layerIndex < depth; ++layerIndex) {
							for (int x = 0; x < image.getWidth(); ++x) {
								for (int y = 0; y < image.getHeight(); ++y) {
									pixelInputOutput.read(image, layerIndex, pixel, x, y);
									if (pixel.alpha < 1.0f) {
										return false;
									}
								}
							}
						}
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * This method scales the given texture to the given size.
	 * 
	 * @param texture
	 *            the texture to be scaled
	 * @param width
	 *            new width of the texture
	 * @param height
	 *            new height of the texture
	 */
	private void scale(Texture2D texture, int width, int height) {
		// first determine if scaling is required
		boolean scaleRequired = texture.getImage().getWidth() != width || texture.getImage().getHeight() != height;

		if (scaleRequired) {
			Image image = texture.getImage();
			BufferedImage sourceImage = ImageToAwt.convert(image, false, true, 0);

			int sourceWidth = sourceImage.getWidth();
			int sourceHeight = sourceImage.getHeight();

			BufferedImage targetImage = new BufferedImage(width, height, sourceImage.getType());

			Graphics2D g = targetImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(sourceImage, 0, 0, width, height, 0, 0, sourceWidth, sourceHeight, null);
			g.dispose();

			Image output = new ImageLoader().load(targetImage, false);
			image.setWidth(width);
			image.setHeight(height);
			image.setData(output.getData(0));
			image.setFormat(output.getFormat());
		}
	}

	/**
	 * A simple class to aggregate the texture data (improves code quality).
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static class TextureData {
		/** The texture. */
		public Texture				texture;
		/** The texture blender (to mix the texture with its material color). */
		public TextureBlender		textureBlender;
		/** The type of UV coordinates. */
		public UVCoordinatesType	uvCoordinatesType;
		/** The type of UV coordinates projection (for flat textures). */
		public UVProjectionType	projectionType;
		/** The texture sructure. */
		public Structure			textureStructure;
	}
}
