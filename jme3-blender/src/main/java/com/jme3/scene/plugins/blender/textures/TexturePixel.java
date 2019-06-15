package com.jme3.scene.plugins.blender.textures;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;

/**
 * The class that stores the pixel values of a texture.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public class TexturePixel implements Cloneable {
    /** The pixel data. */
    public float intensity, red, green, blue, alpha;

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
    public void fromARGB(float a, float r, float g, float b) {
        this.alpha = a;
        this.red = r;
        this.green = g;
        this.blue = b;
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
    public void fromARGB8(byte a, byte r, byte g, byte b) {
        this.alpha = a >= 0 ? a / 255.0f : 1.0f - ~a / 255.0f;
        this.red = r >= 0 ? r / 255.0f : 1.0f - ~r / 255.0f;
        this.green = g >= 0 ? g / 255.0f : 1.0f - ~g / 255.0f;
        this.blue = b >= 0 ? b / 255.0f : 1.0f - ~b / 255.0f;
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
    public void fromARGB16(short a, short r, short g, short b) {
        this.alpha = a >= 0 ? a / 65535.0f : 1.0f - ~a / 65535.0f;
        this.red = r >= 0 ? r / 65535.0f : 1.0f - ~r / 65535.0f;
        this.green = g >= 0 ? g / 65535.0f : 1.0f - ~g / 65535.0f;
        this.blue = b >= 0 ? b / 65535.0f : 1.0f - ~b / 65535.0f;
    }

    /**
     * Copies the intensity from the given value.
     * 
     * @param intensity
     *            the intensity value
     */
    public void fromIntensity(byte intensity) {
        this.intensity = intensity >= 0 ? intensity / 255.0f : 1.0f - ~intensity / 255.0f;
    }

    /**
     * Copies the intensity from the given value.
     * 
     * @param intensity
     *            the intensity value
     */
    public void fromIntensity(short intensity) {
        this.intensity = intensity >= 0 ? intensity / 65535.0f : 1.0f - ~intensity / 65535.0f;
    }

    /**
     * This method sets the alpha value (converts it to float number from range
     * [0, 1]).
     * 
     * @param alpha
     *            the alpha value
     */
    public void setAlpha(byte alpha) {
        this.alpha = alpha >= 0 ? alpha / 255.0f : 1.0f - ~alpha / 255.0f;
    }

    /**
     * This method sets the alpha value (converts it to float number from range
     * [0, 1]).
     * 
     * @param alpha
     *            the alpha value
     */
    public void setAlpha(short alpha) {
        this.alpha = alpha >= 0 ? alpha / 65535.0f : 1.0f - ~alpha / 65535.0f;
    }

    /**
     * Copies the values from the given integer that stores the ARGB8 data.
     * 
     * @param argb8
     *            the data stored in an integer
     */
    public void fromARGB8(int argb8) {
        byte pixelValue = (byte) ((argb8 & 0xFF000000) >> 24);
        this.alpha = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - ~pixelValue / 255.0f;
        pixelValue = (byte) ((argb8 & 0xFF0000) >> 16);
        this.red = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - ~pixelValue / 255.0f;
        pixelValue = (byte) ((argb8 & 0xFF00) >> 8);
        this.green = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - ~pixelValue / 255.0f;
        pixelValue = (byte) (argb8 & 0xFF);
        this.blue = pixelValue >= 0 ? pixelValue / 255.0f : 1.0f - ~pixelValue / 255.0f;
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
     * @return the intensity of the pixel
     */
    public byte getInt() {
        return (byte) (this.intensity * 255.0f);
    }

    /**
     * @return the alpha value of the pixel
     */
    public byte getA8() {
        return (byte) (this.alpha * 255.0f);
    }

    /**
     * @return the alpha red of the pixel
     */
    public byte getR8() {
        return (byte) (this.red * 255.0f);
    }

    /**
     * @return the green value of the pixel
     */
    public byte getG8() {
        return (byte) (this.green * 255.0f);
    }

    /**
     * @return the blue value of the pixel
     */
    public byte getB8() {
        return (byte) (this.blue * 255.0f);
    }

    /**
     * @return the alpha value of the pixel
     */
    public short getA16() {
        return (byte) (this.alpha * 65535.0f);
    }

    /**
     * @return the alpha red of the pixel
     */
    public short getR16() {
        return (byte) (this.red * 65535.0f);
    }

    /**
     * @return the green value of the pixel
     */
    public short getG16() {
        return (byte) (this.green * 65535.0f);
    }

    /**
     * @return the blue value of the pixel
     */
    public short getB16() {
        return (byte) (this.blue * 65535.0f);
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
        this.alpha = (this.alpha + pixel.alpha) * 0.5f;
    }
    
    /**
     * Mixes two pixels.
     * 
     * @param pixel
     *            the pixel we mix with
     */
    public void mix(TexturePixel pixel) {
        this.red = 0.5f * (this.red + pixel.red);
        this.green = 0.5f * (this.green + pixel.green);
        this.blue = 0.5f * (this.blue + pixel.blue);
        this.alpha = 0.5f * (this.alpha + pixel.alpha);
        this.intensity = 0.5f * (this.intensity + pixel.intensity);
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
     * This method adds the calues of the given pixel to the current pixel.
     * 
     * @param pixel
     *            the pixel we add
     */
    public void add(ColorRGBA pixel) {
        this.red += pixel.r;
        this.green += pixel.g;
        this.blue += pixel.b;
        this.alpha += pixel.a;
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
