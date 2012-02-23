package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image.Format;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The class that stores the pixel values of a texture.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class TexturePixel implements Cloneable {
	private static final Logger	LOGGER	= Logger.getLogger(TexturePixel.class.getName());

	/** The pixel data. */
	public float				intensity, red, green, blue, alpha;

	/**
	 * Copies the values from the given pixel.
	 * 
	 * @param pixel
	 *            the pixel that we read from
	 */
	public void fromPixel(TexturePixel pixel) {
		this.intensity = pixel.intensity;
		this.red = pixel.red;
		this.green = pixel.green;
		this.blue = pixel.blue;
		this.alpha = pixel.alpha;
	}

	/**
	 * Copies the values from the given color.
	 * 
	 * @param colorRGBA
	 *            the color that we read from
	 */
	public void fromColor(ColorRGBA colorRGBA) {
		this.red = colorRGBA.r;
		this.green = colorRGBA.g;
		this.blue = colorRGBA.b;
		this.alpha = colorRGBA.a;
	}

	/**
	 * Copies the values from the given values.
	 * 
	 * @param a
	 *            the alpha value
	 * @param r
	 *            the red value
	 * @param g
	 *            the green value
	 * @param b
	 *            the blue value
	 */
	public void fromARGB8(float a, float r, float g, float b) {
		this.alpha = a;
		this.red = r;
		this.green = g;
		this.blue = b;
	}

	/**
	 * Copies the values from the given integer that stores the ARGB8 data.
	 * 
	 * @param argb8
	 *            the data stored in an integer
	 */
	public void fromARGB8(int argb8) {
		byte pixelValue = (byte) ((argb8 & 0xFF000000) >> 24);
		this.alpha = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
		pixelValue = (byte) ((argb8 & 0xFF0000) >> 16);
		this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
		pixelValue = (byte) ((argb8 & 0xFF00) >> 8);
		this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
		pixelValue = (byte) (argb8 & 0xFF);
		this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
	}

	/**
	 * Copies the data from the given image.
	 * 
	 * @param imageFormat
	 *            the image format
	 * @param data
	 *            the image data
	 * @param pixelIndex
	 *            the index of the required pixel
	 */
	public void fromImage(Format imageFormat, ByteBuffer data, int pixelIndex) {
		int firstByteIndex;
		byte pixelValue;
		switch (imageFormat) {
			case ABGR8:
				firstByteIndex = pixelIndex << 2;
				pixelValue = data.get(firstByteIndex);
				this.alpha = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 3);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case RGBA8:
				firstByteIndex = pixelIndex << 2;
				pixelValue = data.get(firstByteIndex);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 3);
				this.alpha = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			case BGR8:
				firstByteIndex = pixelIndex * 3;
				pixelValue = data.get(firstByteIndex);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				this.alpha = 1.0f;
				break;
			case RGB8:
				firstByteIndex = pixelIndex * 3;
				pixelValue = data.get(firstByteIndex);
				this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 1);
				this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				pixelValue = data.get(firstByteIndex + 2);
				this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				this.alpha = 1.0f;
				break;
			case Luminance8:
				pixelValue = data.get(pixelIndex);
				this.intensity = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - (~pixelValue) / 255.0f;
				break;
			default:
				LOGGER.log(Level.FINEST, "Unknown type of texture: {0}. Black pixel used!", imageFormat);
				this.intensity = this.blue = this.red = this.green = this.alpha = 0.0f;
		}
	}

	/**
	 * Stores RGBA values in the given array.
	 * 
	 * @param result
	 *            the array to store values
	 */
	public void toRGBA(float[] result) {
		result[0] = this.red;
		result[1] = this.green;
		result[2] = this.blue;
		result[3] = this.alpha;
	}

	/**
	 * Stores the data in the given table.
	 * 
	 * @param result
	 *            the result table
	 */
	public void toRGBA8(byte[] result) {
		result[0] = (byte) (this.red * 255.0f);
		result[1] = (byte) (this.green * 255.0f);
		result[2] = (byte) (this.blue * 255.0f);
		result[3] = (byte) (this.alpha * 255.0f);
	}

	/**
	 * Stores the pixel values in the integer.
	 * 
	 * @return the integer that stores the pixel values
	 */
	public int toARGB8() {
		int result = 0;
		int b = (int) (this.alpha * 255.0f);
		result |= b << 24;
		b = (int) (this.red * 255.0f);
		result |= b << 16;
		b = (int) (this.green * 255.0f);
		result |= b << 8;
		b = (int) (this.blue * 255.0f);
		result |= b;
		return result;
	}

	/**
	 * Merges two pixels (adds the values of each color).
	 * 
	 * @param pixel
	 *            the pixel we merge with
	 */
	public void merge(TexturePixel pixel) {
		float oneMinusAlpha = 1 - pixel.alpha;
		this.red = oneMinusAlpha * this.red + pixel.alpha * pixel.red;
		this.green = oneMinusAlpha * this.green + pixel.alpha * pixel.green;
		this.blue = oneMinusAlpha * this.blue + pixel.alpha * pixel.blue;
		// alpha should be always 1.0f as a result
	}

	/**
	 * This method negates the colors.
	 */
	public void negate() {
		this.red = 1.0f - this.red;
		this.green = 1.0f - this.green;
		this.blue = 1.0f - this.blue;
		this.alpha = 1.0f - this.alpha;
	}

	/**
	 * This method clears the pixel values.
	 */
	public void clear() {
		this.intensity = this.blue = this.red = this.green = this.alpha = 0.0f;
	}

	/**
	 * This method adds the calues of the given pixel to the current pixel.
	 * 
	 * @param pixel
	 *            the pixel we add
	 */
	public void add(TexturePixel pixel) {
		this.red += pixel.red;
		this.green += pixel.green;
		this.blue += pixel.blue;
		this.alpha += pixel.alpha;
		this.intensity += pixel.intensity;
	}

	/**
	 * This method multiplies the values of the given pixel by the given value.
	 * 
	 * @param value
	 *            multiplication factor
	 */
	public void mult(float value) {
		this.red *= value;
		this.green *= value;
		this.blue *= value;
		this.alpha *= value;
		this.intensity *= value;
	}

	/**
	 * This method divides the values of the given pixel by the given value.
	 * ATTENTION! Beware of the zero value. This will cause you NaN's in the
	 * pixel values.
	 * 
	 * @param value
	 *            division factor
	 */
	public void divide(float value) {
		this.red /= value;
		this.green /= value;
		this.blue /= value;
		this.alpha /= value;
		this.intensity /= value;
	}

	/**
	 * This method clamps the pixel values to the given borders.
	 * 
	 * @param min
	 *            the minimum value
	 * @param max
	 *            the maximum value
	 */
	public void clamp(float min, float max) {
		this.red = FastMath.clamp(this.red, min, max);
		this.green = FastMath.clamp(this.green, min, max);
		this.blue = FastMath.clamp(this.blue, min, max);
		this.alpha = FastMath.clamp(this.alpha, min, max);
		this.intensity = FastMath.clamp(this.intensity, min, max);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return "[" + red + ", " + green + ", " + blue + ", " + alpha + " {" + intensity + "}]";
	}
}
