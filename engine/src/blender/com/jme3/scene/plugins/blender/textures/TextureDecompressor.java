package com.jme3.scene.plugins.blender.textures;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import jme3tools.converters.RGB565;

import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

/**
 * This class decompresses the given image (if necessary) to the RGBA8 format.
 * Currently supported compressed textures are: DXT1, DXT3, DXT5.
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class TextureDecompressor {
	private static final Logger	LOGGER	= Logger.getLogger(TextureDecompressor.class.getName());

	/**
	 * This method decompresses the given image. If the given image is already
	 * decompressed nothing happens and it is simply returned.
	 * 
	 * @param image
	 *            the image to decompress
	 * @return the decompressed image
	 */
	public static Image decompress(Image image) {//TODO: support 3D textures
		byte[] bytes = null;
		TexturePixel[] colors = null;
		ByteBuffer data = image.getData(0);
		data.rewind();
		Format format = image.getFormat();

		DDSTexelData texelData = new DDSTexelData(data.remaining() / (format.getBitsPerPixel() * 2), image.getWidth(), image.getHeight(), format != Format.DXT1);
		switch (format) {// TODO: DXT1A
			case DXT1:// BC1
				bytes = new byte[image.getWidth() * image.getHeight() * 4];
				colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
				while (data.hasRemaining()) {
					short c0 = data.getShort();
					short c1 = data.getShort();
					int col0 = RGB565.RGB565_to_ARGB8(c0);
					int col1 = RGB565.RGB565_to_ARGB8(c1);
					colors[0].fromARGB8(col0);
					colors[1].fromARGB8(col1);

					if (col0 > col1) {
						// creating color2 = 2/3color0 + 1/3color1
						colors[2].fromPixel(colors[0]);
						colors[2].mult(2);
						colors[2].add(colors[1]);
						colors[2].divide(3);

						// creating color3 = 1/3color0 + 2/3color1;
						colors[3].fromPixel(colors[1]);
						colors[3].mult(2);
						colors[3].add(colors[0]);
						colors[3].divide(3);
					} else {
						// creating color2 = 1/2color0 + 1/2color1
						colors[2].fromPixel(colors[0]);
						colors[2].add(colors[1]);
						colors[2].mult(0.5f);

						colors[3].fromARGB8(0);
					}
					int indexes = data.getInt();// 4-byte table with color indexes in decompressed table
					texelData.add(colors, indexes);
				}
				break;
			case DXT3:// BC2
				bytes = new byte[image.getWidth() * image.getHeight() * 4];
				colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
				while (data.hasRemaining()) {
					long alpha = data.getLong();
					float[] alphas = new float[16];
					long alphasIndex = 0;
					for (int i = 0; i < 16; ++i) {
						alphasIndex |= i << (i * 4);
						byte a = (byte) (((alpha >> (i * 4)) & 0x0F) << 4);
						alphas[i] = a >= 0 ? a / 255.0f : 1.0f - (~a) / 255.0f;
					}

					short c0 = data.getShort();
					short c1 = data.getShort();
					int col0 = RGB565.RGB565_to_ARGB8(c0);
					int col1 = RGB565.RGB565_to_ARGB8(c1);
					colors[0].fromARGB8(col0);
					colors[1].fromARGB8(col1);

					// creating color2 = 2/3color0 + 1/3color1
					colors[2].fromPixel(colors[0]);
					colors[2].mult(2);
					colors[2].add(colors[1]);
					colors[2].divide(3);

					// creating color3 = 1/3color0 + 2/3color1;
					colors[3].fromPixel(colors[1]);
					colors[3].mult(2);
					colors[3].add(colors[0]);
					colors[3].divide(3);

					int indexes = data.getInt();// 4-byte table with color indexes in decompressed table
					texelData.add(colors, indexes, alphas, alphasIndex);
				}
				break;
			case DXT5:// BC3
				bytes = new byte[image.getWidth() * image.getHeight() * 4];
				colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
				float[] alphas = new float[8];
				while (data.hasRemaining()) {
					alphas[0] = data.get() * 255.0f;
					alphas[1] = data.get() * 255.0f;
					long alphaIndices = (int) data.get() | ((int) data.get() << 8) | ((int) data.get() << 16) | ((int) data.get() << 24) | ((int) data.get() << 32) | ((int) data.get() << 40);
					if (alphas[0] > alphas[1]) {// 6 interpolated alpha values.
						alphas[2] = (6 * alphas[0] + alphas[1]) / 7;
						alphas[3] = (5 * alphas[0] + 2 * alphas[1]) / 7;
						alphas[4] = (4 * alphas[0] + 3 * alphas[1]) / 7;
						alphas[5] = (3 * alphas[0] + 4 * alphas[1]) / 7;
						alphas[6] = (2 * alphas[0] + 5 * alphas[1]) / 7;
						alphas[7] = (alphas[0] + 6 * alphas[1]) / 7;
					} else {
						alphas[2] = (4 * alphas[0] + alphas[1]) * 0.2f;
						alphas[3] = (3 * alphas[0] + 2 * alphas[1]) * 0.2f;
						alphas[4] = (2 * alphas[0] + 3 * alphas[1]) * 0.2f;
						alphas[5] = (alphas[0] + 4 * alphas[1]) * 0.2f;
						alphas[6] = 0;
						alphas[7] = 1;
					}

					short c0 = data.getShort();
					short c1 = data.getShort();
					int col0 = RGB565.RGB565_to_ARGB8(c0);
					int col1 = RGB565.RGB565_to_ARGB8(c1);
					colors[0].fromARGB8(col0);
					colors[1].fromARGB8(col1);

					// creating color2 = 2/3color0 + 1/3color1
					colors[2].fromPixel(colors[0]);
					colors[2].mult(2);
					colors[2].add(colors[1]);
					colors[2].divide(3);

					// creating color3 = 1/3color0 + 2/3color1;
					colors[3].fromPixel(colors[1]);
					colors[3].mult(2);
					colors[3].add(colors[0]);
					colors[3].divide(3);

					int indexes = data.getInt();// 4-byte table with color
												// indexes in decompressed table
					texelData.add(colors, indexes, alphas, alphaIndices);
				}
				break;
			default:
				LOGGER.fine("Unsupported decompression format.");
		}

		if (bytes != null) {// writing the data to the result table
			byte[] pixelBytes = new byte[4];
			for (int i = 0; i < image.getWidth(); ++i) {
				for (int j = 0; j < image.getHeight(); ++j) {
					texelData.getRGBA8(i, j, pixelBytes);
					bytes[(j * image.getWidth() + i) * 4] = pixelBytes[0];
					bytes[(j * image.getWidth() + i) * 4 + 1] = pixelBytes[1];
					bytes[(j * image.getWidth() + i) * 4 + 2] = pixelBytes[2];
					bytes[(j * image.getWidth() + i) * 4 + 3] = pixelBytes[3];
				}
			}
			return new Image(Format.RGBA8, image.getWidth(), image.getHeight(), BufferUtils.createByteBuffer(bytes));
		}
		return image;
	}

	/**
	 * The data that helps in bytes calculations for the result image.
	 * 
	 * @author Marcin Roguski (Kaelthas)
	 */
	private static class DDSTexelData {
		/** The colors of the texes. */
		private TexturePixel[][]	colors;
		/** The indexes of the texels. */
		private long[]				indexes;
		/** The alphas of the texels (might be null). */
		private float[][]			alphas;
		/** The indexels of texels alpha values (might be null). */
		private long[]				alphaIndexes;
		/** The counter of texel x column. */
		private int					xCounter;
		/** The counter of texel y row. */
		private int					yCounter;
		/** The width of the image in pixels. */
		private int					pixelWidth;
		/** The height of the image in pixels. */
		private int					pixelHeight;
		/** The total texel count. */
		private int					xTexelCount;

		/**
		 * Constructor. Allocates the required memory. Initializes variables.
		 * 
		 * @param textelsCount
		 *            the total count of the texels
		 * @param pixelWidth
		 *            the width of the image in pixels
		 * @param pixelHeight
		 *            the height of the image in pixels
		 * @param isAlpha
		 *            indicates if the memory for alpha values should be
		 *            allocated
		 */
		public DDSTexelData(int textelsCount, int pixelWidth, int pixelHeight, boolean isAlpha) {
			textelsCount = (pixelWidth * pixelHeight) >> 4;
			this.colors = new TexturePixel[textelsCount][];
			this.indexes = new long[textelsCount];
			this.xTexelCount = pixelWidth >> 2;
			this.yCounter = (pixelHeight >> 2) - 1;// xCounter is 0 for now
			this.pixelHeight = pixelHeight;
			this.pixelWidth = pixelWidth;
			if (isAlpha) {
				this.alphas = new float[textelsCount][];
				this.alphaIndexes = new long[textelsCount];
			}
		}

		/**
		 * This method adds a color and indexes for a texel.
		 * 
		 * @param colors
		 *            the colors of the texel
		 * @param indexes
		 *            the indexes of the texel
		 */
		public void add(TexturePixel[] colors, int indexes) {
			this.add(colors, indexes, null, 0);
		}

		/**
		 * This method adds a color, color indexes and alha values (with their
		 * indexes) for a texel.
		 * 
		 * @param colors
		 *            the colors of the texel
		 * @param indexes
		 *            the indexes of the texel
		 * @param alphas
		 *            the alpha values
		 * @param alphaIndexes
		 *            the indexes of the given alpha values
		 */
		public void add(TexturePixel[] colors, int indexes, float[] alphas, long alphaIndexes) {
			int index = yCounter * xTexelCount + xCounter;
			this.colors[index] = colors;
			this.indexes[index] = indexes;
			if (alphas != null) {
				this.alphas[index] = alphas;
				this.alphaIndexes[index] = alphaIndexes;
			}
			++this.xCounter;
			if (this.xCounter >= this.xTexelCount) {
				this.xCounter = 0;
				--this.yCounter;
			}
		}

		/**
		 * This method returns the values of the pixel located on the given
		 * coordinates on the result image.
		 * 
		 * @param x
		 *            the x coordinate of the pixel
		 * @param y
		 *            the y coordinate of the pixel
		 * @param result
		 *            the table where the result is stored
		 */
		public void getRGBA8(int x, int y, byte[] result) {
			int xTexetlIndex = (x % pixelWidth) / 4;
			int yTexelIndex = (y % pixelHeight) / 4;

			int texelIndex = yTexelIndex * xTexelCount + xTexetlIndex;
			TexturePixel[] colors = this.colors[texelIndex];

			// coordinates of the pixel in the selected texel
			x = x - 4 * xTexetlIndex;// pixels are arranged from left to right
			y = 3 - y - 4 * yTexelIndex;// pixels are arranged from bottom to top (that is why '3 - ...' is at the start)

			int pixelIndexInTexel = (y * 4 + x) * (int) FastMath.log(colors.length, 2);
			int alphaIndexInTexel = alphas != null ? (y * 4 + x) * (int) FastMath.log(alphas.length, 2) : 0;

			// getting the pixel
			int indexMask = colors.length - 1;
			int colorIndex = (int) ((this.indexes[texelIndex] >> pixelIndexInTexel) & indexMask);
			float alpha = this.alphas != null ? this.alphas[texelIndex][(int) ((this.alphaIndexes[texelIndex] >> alphaIndexInTexel) & 0x07)] : colors[colorIndex].alpha;
			result[0] = (byte) (colors[colorIndex].red * 255.0f);
			result[1] = (byte) (colors[colorIndex].green * 255.0f);
			result[2] = (byte) (colors[colorIndex].blue * 255.0f);
			result[3] = (byte) (alpha * 255.0f);
		}
	}
}
