package com.jme3.scene.plugins.blender.textures;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import jme3tools.converters.ImageToAwt;

import com.jme3.bounding.BoundingBox;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.BlenderContext;
import com.jme3.scene.plugins.blender.textures.blending.TextureBlender;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;

/**
 * This texture holds a set of images for each face in the specified mesh. It
 * helps to flatten 3D texture, merge 3D and 2D textures and merge 2D textures
 * with different UV coordinates.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/* package */class TriangulatedTexture extends Texture2D {
	/** The result image format. */
	private Format								format;
	/** The collection of images for each face. */
	private Collection<TriangleTextureElement>	faceTextures;
	/**
	 * The maximum texture size (width/height). This is taken from the blender
	 * key.
	 */
	private int									maxTextureSize;
	/** A variable that can prevent removing identical textures. */
	private boolean								keepIdenticalTextures = false;
	/** The result texture. */
	private Texture2D							resultTexture;
	/** The result texture's UV coordinates. */
	private List<Vector2f>						resultUVS;
    
	/**
	 * This method triangulates the given flat texture. The given texture is not
	 * changed.
	 * 
	 * @param texture2d
	 *            the texture to be triangulated
	 * @param uvs
	 *            the UV coordinates for each face
	 */
	public TriangulatedTexture(Texture2D texture2d, List<Vector2f> uvs, BlenderContext blenderContext) {
		maxTextureSize = blenderContext.getBlenderKey().getMaxTextureSize();
		faceTextures = new TreeSet<TriangleTextureElement>(new Comparator<TriangleTextureElement>() {
			public int compare(TriangleTextureElement o1, TriangleTextureElement o2) {
				return o1.faceIndex - o2.faceIndex;
			}
		});
		int facesCount = uvs.size() / 3;
		for (int i = 0; i < facesCount; ++i) {
			faceTextures.add(new TriangleTextureElement(i, texture2d.getImage(), uvs, true, blenderContext));
		}
		this.format = texture2d.getImage().getFormat();
	}

	/**
	 * Constructor that simply stores precalculated images.
	 * 
	 * @param faceTextures
	 *            a collection of images for the mesh's faces
	 * @param blenderContext
	 *            the blender context
	 */
	public TriangulatedTexture(Collection<TriangleTextureElement> faceTextures, BlenderContext blenderContext) {
		maxTextureSize = blenderContext.getBlenderKey().getMaxTextureSize();
		this.faceTextures = faceTextures;
		for (TriangleTextureElement faceTextureElement : faceTextures) {
			if (format == null) {
				format = faceTextureElement.image.getFormat();
			} else if (format != faceTextureElement.image.getFormat()) {
				throw new IllegalArgumentException("Face texture element images MUST have the same image format!");
			}
		}
	}

	/**
	 * This method blends the each image using the given blender and taking base
	 * texture into consideration.
	 * 
	 * @param textureBlender
	 *            the texture blender that holds the blending definition
	 * @param baseTexture
	 *            the texture that is 'below' the current texture (can be null)
	 * @param blenderContext
	 *            the blender context
	 */
	public void blend(TextureBlender textureBlender, TriangulatedTexture baseTexture, BlenderContext blenderContext) {
		Format newFormat = null;
		for (TriangleTextureElement triangleTextureElement : this.faceTextures) {
			Image baseImage = baseTexture == null ? null : baseTexture.getFaceTextureElement(triangleTextureElement.faceIndex).image;
			triangleTextureElement.image = textureBlender.blend(triangleTextureElement.image, baseImage, blenderContext);
			if (newFormat == null) {
				newFormat = triangleTextureElement.image.getFormat();
			} else if (newFormat != triangleTextureElement.image.getFormat()) {
				throw new IllegalArgumentException("Face texture element images MUST have the same image format!");
			}
		}
		this.format = newFormat;
	}

	/**
	 * This method alters the images to fit them into UV coordinates of the
	 * given target texture.
	 * 
	 * @param targetTexture
	 *            the texture to whose UV coordinates we fit current images
	 * @param blenderContext
	 *            the blender context
	 */
	public void castToUVS(TriangulatedTexture targetTexture, BlenderContext blenderContext) {
		int[] sourceSize = new int[2], targetSize = new int[2];
		ImageLoader imageLoader = new ImageLoader();
		TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
		for (TriangleTextureElement entry : faceTextures) {
			TriangleTextureElement targetFaceTextureElement = targetTexture.getFaceTextureElement(entry.faceIndex);
			Vector2f[] dest = targetFaceTextureElement.uv;

			// get the sizes of the source and target images
			sourceSize[0] = entry.image.getWidth();
			sourceSize[1] = entry.image.getHeight();
			targetSize[0] = targetFaceTextureElement.image.getWidth();
			targetSize[1] = targetFaceTextureElement.image.getHeight();

			// create triangle transformation
			AffineTransform affineTransform = textureHelper.createAffineTransform(entry.uv, dest, sourceSize, targetSize);

			// compute the result texture
			BufferedImage sourceImage = ImageToAwt.convert(entry.image, false, true, 0);

			BufferedImage targetImage = new BufferedImage(targetSize[0], targetSize[1], sourceImage.getType());
			Graphics2D g = targetImage.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(sourceImage, affineTransform, null);
			g.dispose();

			Image output = imageLoader.load(targetImage, false);
			entry.image = output;
			entry.uv[0].set(dest[0]);
			entry.uv[1].set(dest[1]);
			entry.uv[2].set(dest[2]);
		}
	}

	/**
	 * This method merges the current texture with the given one. The given
	 * texture is not changed.
	 * 
	 * @param triangulatedTexture
	 *            the texture we merge current texture with
	 */
	public void merge(TriangulatedTexture triangulatedTexture) {
		TexturePixel sourcePixel = new TexturePixel();
		TexturePixel targetPixel = new TexturePixel();
		for (TriangleTextureElement triangleTextureElement : this.faceTextures) {
			Image sourceImage = triangulatedTexture.getFaceTextureElement(triangleTextureElement.faceIndex).image;
			Image targetImage = triangleTextureElement.image;
			PixelInputOutput sourceIO = PixelIOFactory.getPixelIO(sourceImage.getFormat());
			PixelInputOutput targetIO = PixelIOFactory.getPixelIO(targetImage.getFormat());

			for (int x = 0; x < sourceImage.getWidth(); ++x) {
				for (int y = 0; y < sourceImage.getHeight(); ++y) {
					sourceIO.read(sourceImage, 0, sourcePixel, x, y);
					targetIO.read(targetImage, 0, targetPixel, x, y);
					targetPixel.merge(sourcePixel);
					targetIO.write(targetImage, 0, targetPixel, x, y);
				}
			}
		}
	}

	/**
	 * This method returns the flat texture. It is calculated if required or if
	 * it was not created before. Images that are identical are discarded to
	 * reduce the texture size.
	 * 
	 * @param rebuild
	 *            a variable that forces texture recomputation (even if it was
	 *            computed vefore)
	 * @return flat result texture (all images merged into one)
	 */
	public Texture2D getResultTexture(boolean rebuild) {
		if (resultTexture == null || rebuild) {
			// sorting the parts by their height (from highest to the lowest)
			List<TriangleTextureElement> list = new ArrayList<TriangleTextureElement>(faceTextures);
			Collections.sort(list, new Comparator<TriangleTextureElement>() {
				public int compare(TriangleTextureElement o1, TriangleTextureElement o2) {
					return o2.image.getHeight() - o1.image.getHeight();
				}
			});

			// arraging the images on the resulting image (calculating the result image width and height)
			Set<Integer> duplicatedFaceIndexes = new HashSet<Integer>();
			int resultImageHeight = list.get(0).image.getHeight();
			int resultImageWidth = 0;
			int currentXPos = 0, currentYPos = 0;
			Map<TriangleTextureElement, Integer[]> imageLayoutData = new HashMap<TriangleTextureElement, Integer[]>(list.size());
			while (list.size() > 0) {
				TriangleTextureElement currentElement = list.remove(0);
				if (currentXPos + currentElement.image.getWidth() > maxTextureSize) {
					currentXPos = 0;
					currentYPos = resultImageHeight;
					resultImageHeight += currentElement.image.getHeight();
				}
				Integer[] currentPositions = new Integer[] { currentXPos, currentYPos };
				imageLayoutData.put(currentElement, currentPositions);

				if(keepIdenticalTextures) {// removing identical images
					for (int i = 0; i < list.size(); ++i) {
						if (currentElement.image.equals(list.get(i).image)) {
							duplicatedFaceIndexes.add(list.get(i).faceIndex);
							imageLayoutData.put(list.remove(i--), currentPositions);
						}
					}
				}

				currentXPos += currentElement.image.getWidth();
				resultImageWidth = Math.max(resultImageWidth, currentXPos);
				// currentYPos += currentElement.image.getHeight();

				// TODO: implement that to compact the result image
				// try to add smaller images below the current one
				// int remainingHeight = resultImageHeight -
				// currentElement.image.getHeight();
				// while(remainingHeight > 0) {
				// for(int i=list.size() - 1;i>=0;--i) {
				//
				// }
				// }
			}

			// computing the result UV coordinates
			resultUVS = new ArrayList<Vector2f>(imageLayoutData.size() * 3);
			for (int i = 0; i < imageLayoutData.size() * 3; ++i) {
				resultUVS.add(null);
			}
			Vector2f[] uvs = new Vector2f[3];
			for (Entry<TriangleTextureElement, Integer[]> entry : imageLayoutData.entrySet()) {
				Integer[] position = entry.getValue();
				entry.getKey().computeFinalUVCoordinates(resultImageWidth, resultImageHeight, position[0], position[1], uvs);
				resultUVS.set(entry.getKey().faceIndex * 3, uvs[0]);
				resultUVS.set(entry.getKey().faceIndex * 3 + 1, uvs[1]);
				resultUVS.set(entry.getKey().faceIndex * 3 + 2, uvs[2]);
			}

			Image resultImage = new Image(format, resultImageWidth, resultImageHeight, BufferUtils.createByteBuffer(resultImageWidth * resultImageHeight * (format.getBitsPerPixel() >> 3)));
			resultTexture = new Texture2D(resultImage);
			for (Entry<TriangleTextureElement, Integer[]> entry : imageLayoutData.entrySet()) {
				if (!duplicatedFaceIndexes.contains(entry.getKey().faceIndex)) {
					this.draw(resultImage, entry.getKey().image, entry.getValue()[0], entry.getValue()[1]);
				}
			}
			
			// setting additional data
			resultTexture.setWrap(WrapAxis.S, this.getWrap(WrapAxis.S));
			resultTexture.setWrap(WrapAxis.T, this.getWrap(WrapAxis.T));
			resultTexture.setMagFilter(this.getMagFilter());
			resultTexture.setMinFilter(this.getMinFilter());
		}
		return resultTexture;
	}

	/**
	 * @return the result flat texture
	 */
	public Texture2D getResultTexture() {
		return this.getResultTexture(false);
	}

	/**
	 * @return the result texture's UV coordinates
	 */
	public List<Vector2f> getResultUVS() {
		this.getResultTexture();// this is called here to make sure that the result UVS are computed
		return resultUVS;
	}

	/**
	 * This method returns a single image element for the given face index.
	 * 
	 * @param faceIndex
	 *            the face index
	 * @return image element for the required face index
	 * @throws IllegalStateException
	 *             this exception is thrown if the current image set does not
	 *             contain an image for the given face index
	 */
	public TriangleTextureElement getFaceTextureElement(int faceIndex) {
		for (TriangleTextureElement textureElement : faceTextures) {
			if (textureElement.faceIndex == faceIndex) {
				return textureElement;
			}
		}
		throw new IllegalStateException("No face texture element found for index: " + faceIndex);
	}
	
	/**
	 * @return the amount of texture faces
	 */
	public int getFaceTextureCount() {
		return faceTextures.size();
	}

	/**
	 * Tells the object wheather to keep or reduce identical face textures.
	 * 
	 * @param keepIdenticalTextures
	 *            keeps or discards identical textures
	 */
	public void setKeepIdenticalTextures(boolean keepIdenticalTextures) {
		this.keepIdenticalTextures = keepIdenticalTextures;
	}

	/**
	 * This method draws the source image on the target image starting with the
	 * specified positions.
	 * 
	 * @param target
	 *            the target image
	 * @param source
	 *            the source image
	 * @param targetXPos
	 *            start X position on the target image
	 * @param targetYPos
	 *            start Y position on the target image
	 */
	private void draw(Image target, Image source, int targetXPos, int targetYPos) {
		PixelInputOutput sourceIO = PixelIOFactory.getPixelIO(source.getFormat());
		PixelInputOutput targetIO = PixelIOFactory.getPixelIO(target.getFormat());
		TexturePixel pixel = new TexturePixel();

		for (int x = 0; x < source.getWidth(); ++x) {
			for (int y = 0; y < source.getHeight(); ++y) {
				sourceIO.read(source, 0, pixel, x, y);
				targetIO.write(target, 0, pixel, targetXPos + x, targetYPos + y);
			}
		}
	}

	/**
	 * A class that represents an image for a single face of the mesh.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	/* package */static class TriangleTextureElement {
		/** The image for the face. */
		public Image			image;
		/** The UV coordinates for the image. */
		public final Vector2f[]	uv;
		/** The index of the face this image refers to. */
		public final int		faceIndex;

		/**
		 * Constructor that creates the image element from the given texture and
		 * UV coordinates (it cuts out the smallest rectasngle possible from the
		 * given image that will hold the triangle defined by the given UV
		 * coordinates). After the image is cut out the UV coordinates are
		 * recalculated to be fit for the new image.
		 * 
		 * @param faceIndex
		 *            the index of mesh's face this image refers to
		 * @param sourceImage
		 *            the source image
		 * @param uvCoordinates
		 *            the UV coordinates that define the image
		 */
		public TriangleTextureElement(int faceIndex, Image sourceImage, List<Vector2f> uvCoordinates, boolean wholeUVList, BlenderContext blenderContext) {
			TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
			this.faceIndex = faceIndex;

			uv = wholeUVList ? 
					new Vector2f[] { uvCoordinates.get(faceIndex * 3).clone(), uvCoordinates.get(faceIndex * 3 + 1).clone(), uvCoordinates.get(faceIndex * 3 + 2).clone() } :
					new Vector2f[] { uvCoordinates.get(0).clone(), uvCoordinates.get(1).clone(), uvCoordinates.get(2).clone() };

			// be careful here, floating point operations might cause the
			// texture positions to be inapropriate
			int[][] texturePosition = new int[3][2];
			for (int i = 0; i < texturePosition.length; ++i) {
				texturePosition[i][0] = textureHelper.getPixelPosition(uv[i].x, sourceImage.getWidth());
				texturePosition[i][1] = textureHelper.getPixelPosition(uv[i].y, sourceImage.getHeight());
			}

			// calculating the extent of the texture
			int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
			int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
			float minUVX = Float.MAX_VALUE, minUVY = Float.MAX_VALUE;
			float maxUVX = Float.MIN_VALUE, maxUVY = Float.MIN_VALUE;

			for (int i = 0; i < texturePosition.length; ++i) {
				minX = Math.min(texturePosition[i][0], minX);
				minY = Math.min(texturePosition[i][1], minY);

				maxX = Math.max(texturePosition[i][0], maxX);
				maxY = Math.max(texturePosition[i][1], maxY);

				minUVX = Math.min(uv[i].x, minUVX);
				minUVY = Math.min(uv[i].y, minUVY);
				maxUVX = Math.max(uv[i].x, maxUVX);
				maxUVY = Math.max(uv[i].y, maxUVY);
			}
			int width = maxX - minX;
			int height = maxY - minY;

			if (width == 0) {
				width = 1;
			}
			if (height == 0) {
				height = 1;
			}

			// copy the pixel from the texture to the result image
			PixelInputOutput pixelReader = PixelIOFactory.getPixelIO(sourceImage.getFormat());
			TexturePixel pixel = new TexturePixel();
			ByteBuffer data = BufferUtils.createByteBuffer(width * height * 4);
			for (int y = minY; y < maxY; ++y) {
				for (int x = minX; x < maxX; ++x) {
					int xPos = x >= sourceImage.getWidth() ? x - sourceImage.getWidth() : x;
					int yPos = y >= sourceImage.getHeight() ? y - sourceImage.getHeight() : y;
					pixelReader.read(sourceImage, 0, pixel, xPos, yPos);
					data.put(pixel.getR8());
					data.put(pixel.getG8());
					data.put(pixel.getB8());
					data.put(pixel.getA8());
				}
			}
			image = new Image(Format.RGBA8, width, height, data);

			// modify the UV values so that they fit the new image
			float heightUV = maxUVY - minUVY;
			float widthUV = maxUVX - minUVX;
			for (int i = 0; i < uv.length; ++i) {
				// first translate it to the image borders
				uv[i].x -= minUVX;
				uv[i].y -= minUVY;
				// then scale so that it fills the whole area
				uv[i].x /= widthUV;
				uv[i].y /= heightUV;
			}
		}

		/**
		 * Constructor that creates an image element from the 3D texture
		 * (generated texture). It computes a flat smallest rectangle that can
		 * hold a (3D) triangle defined by the given UV coordinates. Then it
		 * defines the image pixels for points in 3D space that define the
		 * calculated rectangle.
		 * 
		 * @param faceIndex
		 *            the face index this image refers to
		 * @param boundingBox
		 *            the bounding box of the mesh
		 * @param texture
		 *            the texture that allows to compute a pixel value in 3D
		 *            space
		 * @param uv
		 *            the UV coordinates of the mesh
		 * @param blenderContext
		 *            the blender context
		 */
		public TriangleTextureElement(int faceIndex, BoundingBox boundingBox, GeneratedTexture texture, Vector3f[] uv, int[] uvIndices, BlenderContext blenderContext) {
			this.faceIndex = faceIndex;

			// compute the face vertices from the UV coordinates
			float width = boundingBox.getXExtent() * 2;
			float height = boundingBox.getYExtent() * 2;
			float depth = boundingBox.getZExtent() * 2;

			Vector3f min = boundingBox.getMin(null);
			Vector3f v1 = min.add(uv[uvIndices[0]].x * width, uv[uvIndices[0]].y * height, uv[uvIndices[0]].z * depth);
			Vector3f v2 = min.add(uv[uvIndices[1]].x * width, uv[uvIndices[1]].y * height, uv[uvIndices[1]].z * depth);
			Vector3f v3 = min.add(uv[uvIndices[2]].x * width, uv[uvIndices[2]].y * height, uv[uvIndices[2]].z * depth);

			// get the rectangle envelope for the triangle
			RectangleEnvelope envelope = this.getTriangleEnvelope(v1, v2, v3);

			// create the result image
			Format imageFormat = texture.getImage().getFormat();
			int imageWidth = (int) (envelope.width * blenderContext.getBlenderKey().getGeneratedTexturePPU());
			if(imageWidth == 0) {
				imageWidth = 1;
			}
			int imageHeight = (int) (envelope.height * blenderContext.getBlenderKey().getGeneratedTexturePPU());
			if(imageHeight == 0) {
				imageHeight = 1;
			}
			ByteBuffer data = BufferUtils.createByteBuffer(imageWidth * imageHeight * (imageFormat.getBitsPerPixel() >> 3));
			image = new Image(texture.getImage().getFormat(), imageWidth, imageHeight, data);

			// computing the pixels
			PixelInputOutput pixelWriter = PixelIOFactory.getPixelIO(imageFormat);
			TexturePixel pixel = new TexturePixel();
			float[] uvs = new float[3];
			Vector3f point = new Vector3f(envelope.min);
			Vector3f vecY = new Vector3f();
			Vector3f wDelta = new Vector3f(envelope.w).multLocal(1.0f / imageWidth);
			Vector3f hDelta = new Vector3f(envelope.h).multLocal(1.0f / imageHeight);
			for (int x = 0; x < imageWidth; ++x) {
				for (int y = 0; y < imageHeight; ++y) {
					this.toTextureUV(boundingBox, point, uvs);
					texture.getPixel(pixel, uvs[0], uvs[1], uvs[2]);
					pixelWriter.write(image, 0, pixel, x, y);
					point.addLocal(hDelta);
				}

				vecY.addLocal(wDelta);
				point.set(envelope.min).addLocal(vecY);
			}

			// preparing UV coordinates for the flatted texture
			this.uv = new Vector2f[3];
			this.uv[0] = new Vector2f(FastMath.clamp(v1.subtract(envelope.min).length(), 0, Float.MAX_VALUE) / envelope.height, 0);
			Vector3f heightDropPoint = v2.subtract(envelope.w);// w is directed from the base to v2
			this.uv[1] = new Vector2f(1, heightDropPoint.subtractLocal(envelope.min).length() / envelope.height);
			this.uv[2] = new Vector2f(0, 1);
		}

		/**
		 * This method computes the final UV coordinates for the image (after it
		 * is combined with other images and drawed on the result image).
		 * 
		 * @param totalImageWidth
		 *            the result image width
		 * @param totalImageHeight
		 *            the result image height
		 * @param xPos
		 *            the most left x coordinate of the image
		 * @param yPos
		 *            the most top y coordinate of the image
		 * @param result
		 *            a vector where the result is stored
		 */
		public void computeFinalUVCoordinates(int totalImageWidth, int totalImageHeight, int xPos, int yPos, Vector2f[] result) {
			for (int i = 0; i < 3; ++i) {
				result[i] = new Vector2f();
				result[i].x = xPos / (float) totalImageWidth + this.uv[i].x * (this.image.getWidth() / (float) totalImageWidth);
				result[i].y = yPos / (float) totalImageHeight + this.uv[i].y * (this.image.getHeight() / (float) totalImageHeight);
			}
		}

		/**
		 * This method converts the given point into 3D UV coordinates.
		 * 
		 * @param boundingBox
		 *            the bounding box of the mesh
		 * @param point
		 *            the point to be transformed
		 * @param uvs
		 *            the result UV coordinates
		 */
		private void toTextureUV(BoundingBox boundingBox, Vector3f point, float[] uvs) {
			uvs[0] = (point.x - boundingBox.getCenter().x)/(boundingBox.getXExtent() == 0 ? 1 : boundingBox.getXExtent());
			uvs[1] = (point.y - boundingBox.getCenter().y)/(boundingBox.getYExtent() == 0 ? 1 : boundingBox.getYExtent());
			uvs[2] = (point.z - boundingBox.getCenter().z)/(boundingBox.getZExtent() == 0 ? 1 : boundingBox.getZExtent());
		}

		/**
		 * This method returns an envelope of a minimal rectangle, that is set
		 * in 3D space, and contains the given triangle.
		 * 
		 * @param triangle
		 *            the triangle
		 * @return a rectangle minimum and maximum point and height and width
		 */
		private RectangleEnvelope getTriangleEnvelope(Vector3f v1, Vector3f v2, Vector3f v3) {
			Vector3f h = v3.subtract(v1);// the height of the resulting rectangle
			Vector3f temp = v2.subtract(v1);

			float field = 0.5f * h.cross(temp).length();// the field of the rectangle: Field = 0.5 * ||h x temp||
			if (field <= 0.0f) {
				return new RectangleEnvelope(v1);// return single point envelope
			}

			float cosAlpha = h.dot(temp) / (h.length() * temp.length());// the cosinus of angle betweenh and temp

			float triangleHeight = 2 * field / h.length();// the base of the height is the h vector
			// now calculate the distance between v1 vertex and the point where
			// the above calculated height 'touches' the base line (it can be
			// settled outside the h vector)
			float x = Math.abs((float) Math.sqrt(FastMath.clamp(temp.lengthSquared() - triangleHeight * triangleHeight, 0, Float.MAX_VALUE))) * Math.signum(cosAlpha);
			// now get the height base point
			Vector3f xPoint = v1.add(h.normalize().multLocal(x));

			// get the minimum point of the envelope
			Vector3f min = x < 0 ? xPoint : v1;
			if (x < 0) {
				h = v3.subtract(min);
			} else if (x > h.length()) {
				h = xPoint.subtract(min);
			}

			Vector3f envelopeWidth = v2.subtract(xPoint);
			return new RectangleEnvelope(min, envelopeWidth, h);
		}
	}

	/**
	 * A class that represents a flat rectangle in 3D space that is built on a
	 * triangle in 3D space.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static class RectangleEnvelope {
		/** The minimum point of the rectangle. */
		public final Vector3f	min;
		/** The width vector. */
		public final Vector3f	w;
		/** The height vector. */
		public final Vector3f	h;
		/** The width of the rectangle. */
		public final float		width;
		/** The height of the rectangle. */
		public final float		height;

		/**
		 * Constructs a rectangle that actually holds a point, not a triangle.
		 * This is a special case that is sometimes used when generating a
		 * texture where UV coordinates are defined by normals instead of
		 * vertices.
		 * 
		 * @param pointPosition
		 *            a position in 3D space
		 */
		public RectangleEnvelope(Vector3f pointPosition) {
			this.min = pointPosition;
			this.h = this.w = Vector3f.ZERO;
			this.width = this.height = 1;
		}

		/**
		 * Constructs a rectangle envelope.
		 * 
		 * @param min
		 *            the minimum rectangle point
		 * @param w
		 *            the width vector
		 * @param h
		 *            the height vector
		 */
		public RectangleEnvelope(Vector3f min, Vector3f w, Vector3f h) {
			this.min = min;
			this.h = h;
			this.w = w;
			this.width = w.length();
			this.height = h.length();
		}

		@Override
		public String toString() {
			return "Envelope[min = " + min + ", w = " + w + ", h = " + h + "]";
		}
	}

	@Override
	public Texture createSimpleClone() {
		return null;
	}
}
