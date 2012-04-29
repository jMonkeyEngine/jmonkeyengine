package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.FastMath;

/**
 * The data that helps in bytes calculations for the result image.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
/*package*/ class DDSTexelData {
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
		textelsCount = pixelWidth * pixelHeight >> 4;
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
		int xTexetlIndex = x % pixelWidth / 4;
		int yTexelIndex = y % pixelHeight / 4;

		int texelIndex = yTexelIndex * xTexelCount + xTexetlIndex;
		TexturePixel[] colors = this.colors[texelIndex];

		// coordinates of the pixel in the selected texel
		x = x - 4 * xTexetlIndex;// pixels are arranged from left to right
		y = 3 - y - 4 * yTexelIndex;// pixels are arranged from bottom to top (that is why '3 - ...' is at the start)

		int pixelIndexInTexel = (y * 4 + x) * (int) FastMath.log(colors.length, 2);
		int alphaIndexInTexel = alphas != null ? (y * 4 + x) * (int) FastMath.log(alphas.length, 2) : 0;

		// getting the pixel
		int indexMask = colors.length - 1;
		int colorIndex = (int) (this.indexes[texelIndex] >> pixelIndexInTexel & indexMask);
		float alpha = this.alphas != null ? this.alphas[texelIndex][(int) (this.alphaIndexes[texelIndex] >> alphaIndexInTexel & 0x07)] : colors[colorIndex].alpha;
		result[0] = (byte) (colors[colorIndex].red * 255.0f);
		result[1] = (byte) (colors[colorIndex].green * 255.0f);
		result[2] = (byte) (colors[colorIndex].blue * 255.0f);
		result[3] = (byte) (alpha * 255.0f);
	}
}
