package com.jme3.texture.image;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import java.nio.ByteBuffer;

/**
 * Utility class for reading and writing from jME3 {@link Image images}.
 * <br>
 * Allows directly manipulating pixels of the image by writing and 
 * reading {@link ColorRGBA colors} at any coordinate, without
 * regard to the underlying {@link Image.Format format} of the image.
 * NOTE: compressed and depth formats are <strong>not supported</strong>.
 * Special RGB formats like RGB111110F and RGB9E5 are not supported
 * at the moment, but may be added later on. For now 
 * use RGB16F_to_RGB111110F and RGB16F_to_RGB9E5 to handle
 * the conversion on the GPU.
 * <p>
 * If direct manipulations are done to the image, such as replacing
 * the image data, or changing the width, height, or format, then
 * all current instances of <code>ImageReadWrite</code> become invalid, and
 * new instances must be created in order to properly access
 * the image data.
 * 
 * Usage example:<br>
 * <code>
 * Image myImage = ...
 * ImageRaster raster = new ImageRaster(myImage);
 * raster.setPixel(1, 5, ColorRGBA.Green);
 * System.out.println( raster.getPixel(1, 5) ); // Will print [0.0, 1.0, 0.0, 1.0].
 * </code>
 * 
 * @author Kirill Vainer
 */
public final class ImageRaster {

    private final int[] components = new int[4];
    private final ByteBuffer buffer;
    private final Image image;
    private final ImageCodec codec;
    private final int width;
    private final int height;
    private final byte[] temp;

    /**
     * Create new image reader / writer.
     *
     * @param image The image to read / write to.
     * @param slice Which slice to use. Only applies to 3D images, 2D image
     * arrays or cubemaps.
     */
    public ImageRaster(Image image, int slice) {
        this.image = image;
        this.buffer = image.getData(slice);
        this.codec = ImageCodec.lookup(image.getFormat());
        this.width = image.getWidth();
        this.height = image.getHeight();
        if (codec instanceof ByteAlignedImageCodec) {
            this.temp = new byte[codec.bpp];
        } else {
            this.temp = null;
        }
    }
    
    /**
     * Create new image reader / writer for 2D images.
     * 
     * @param image The image to read / write to.
     */
    public ImageRaster(Image image) {
        this(image, 0);
        if (image.getData().size() > 1) {
            throw new IllegalStateException("Use constructor that takes slices argument to read from multislice image");
        }
    }
    
    private void rangeCheck(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            throw new IllegalArgumentException("x and y must be inside the image dimensions");
        }
    }
    
    /**
     * Sets the pixel at the given coordinate to the given color.
     * <p>
     * For all integer based formats (those not ending in "F"), the 
     * color is first clamped to 0.0 - 1.0 before converting it to
     * an integer to avoid overflow. For floating point based formats, 
     * components larger than 1.0 can be represented, but components
     * lower than 0.0 are still not allowed (as all formats are unsigned).
     * <p>
     * If the underlying format is grayscale (e.g. one of the luminance formats,
     * such as {@link Image.Format#Luminance8}) then a color to grayscale
     * conversion is done first, before writing the result into the image.
     * <p>
     * If the image does not have some of the components in the color (such
     * as alpha, or any of the color components), then these components
     * will be ignored. The only exception to this is luminance formats
     * for which the color is converted to luminance first (see above).
     * <p>
     * After writing the color, the image shall be marked as requiring an
     * update. The next time it is used for rendering, all pixel changes
     * will be reflected when the image is rendered.
     * 
     * @param x The x coordinate, from 0 to width - 1.
     * @param y The y coordinate, from 0 to height - 1.
     * @param color The color to write. 
     * @throws IllegalArgumentException If x or y are outside the image dimensions.
     */
    public void setPixel(int x, int y, ColorRGBA color) {
        rangeCheck(x, y);
        
        // Check flags for grayscale
        if ((codec.flags & ImageCodec.FLAG_GRAY) != 0) {
            float gray = color.r * 0.27f + color.g * 0.67f + color.b * 0.06f;
            color = new ColorRGBA(gray, gray, gray, color.a);
        }

        if ((codec.flags & ImageCodec.FLAG_F16) != 0) {
            components[0] = (int) FastMath.convertFloatToHalf(color.a);
            components[1] = (int) FastMath.convertFloatToHalf(color.r);
            components[2] = (int) FastMath.convertFloatToHalf(color.g);
            components[3] = (int) FastMath.convertFloatToHalf(color.b);
        } else if ((codec.flags & ImageCodec.FLAG_F32) != 0) {
            components[0] = (int) Float.floatToIntBits(color.a);
            components[1] = (int) Float.floatToIntBits(color.r);
            components[2] = (int) Float.floatToIntBits(color.g);
            components[3] = (int) Float.floatToIntBits(color.b);
        } else {
            // Convert color to bits by multiplying by size
            components[0] = Math.min( (int) (color.a * codec.maxAlpha + 0.5f), codec.maxAlpha);
            components[1] = Math.min( (int) (color.r * codec.maxRed + 0.5f), codec.maxRed);
            components[2] = Math.min( (int) (color.g * codec.maxGreen + 0.5f), codec.maxGreen);
            components[3] = Math.min( (int) (color.b * codec.maxBlue + 0.5f), codec.maxBlue);
        }

        codec.writeComponents(buffer, x, y, width, components, temp);
        
        image.setUpdateNeeded();
    }
    
    /**
     * Retrieve the color at the given coordinate.
     * <p>
     * Any components that are not defined in the image format
     * will be set to 1.0 in the returned color. For example,
     * reading from an {@link Image.Format#Alpha8} format will
     * return a ColorRGBA with the R, G, and B components set to 1.0, and
     * the A component set to the alpha in the image.
     * <p>
     * For grayscale or luminance formats, the luminance value is replicated
     * in the R, G, and B components. 
     * <p>
     * Integer formats are converted to the range 0.0 - 1.0, based
     * on the maximum possible integer value that can be represented
     * by the number of bits the component has.
     * For example, the {@link Image.Format#RGB5A1} format can
     * contain the integer values 0 - 31, a conversion to floating point
     * is done by diving the integer value by 31 (done with floating point
     * precision).
     * 
     * @param x The x coordinate, from 0 to width - 1.
     * @param y The y coordinate, from 0 to height - 1.
     * @param store Storage location for the read color, if <code>null</code>, 
     * then a new ColorRGBA is created and returned with the read color.
     * @return The store parameter, if it is null, then a new ColorRGBA
     * with the read color.
     * @throws IllegalArgumentException If x or y are outside the image dimensions.
     */
    public ColorRGBA getPixel(int x, int y, ColorRGBA store) {
        rangeCheck(x, y);
        
        codec.readComponents(buffer, x, y, width, components, temp);
     
        if (store == null) {
            store = new ColorRGBA();
        }
        if ((codec.flags & ImageCodec.FLAG_F16) != 0) {
            store.set(FastMath.convertHalfToFloat((short)components[1]),
                      FastMath.convertHalfToFloat((short)components[2]),
                      FastMath.convertHalfToFloat((short)components[3]),
                      FastMath.convertHalfToFloat((short)components[0]));
        } else if ((codec.flags & ImageCodec.FLAG_F32) != 0) {
            store.set(Float.intBitsToFloat((int)components[1]),
                      Float.intBitsToFloat((int)components[2]),
                      Float.intBitsToFloat((int)components[3]),
                      Float.intBitsToFloat((int)components[0]));
        } else {
            // Convert to float and divide by bitsize to get into range 0.0 - 1.0.
            store.set((float)components[1] / codec.maxRed,
                      (float)components[2] / codec.maxGreen,
                      (float)components[3] / codec.maxBlue,
                      (float)components[0] / codec.maxAlpha);
        }
        if ((codec.flags & ImageCodec.FLAG_GRAY) != 0) {
            store.g = store.b = store.r;
        } else {
            if (codec.maxRed == 0) {
                store.r = 1;
            }
            if (codec.maxGreen == 0) {
                store.g = 1;
            }
            if (codec.maxBlue == 0) {
                store.b = 1;
            }
            if (codec.maxAlpha == 0) {
                store.a = 1;
            }
        }
        return store;
    }
    
    /**
     * Retrieve the color at the given coordinate.
     * <p>
     * Convenience method that does not take a store argument. Equivalent
     * to calling getPixel(x, y, null). 
     * See {@link #getPixel(int, int, com.jme3.math.ColorRGBA) } for
     * more information.
     * 
     * @param x The x coordinate, from 0 to width - 1.
     * @param y The y coordinate, from 0 to height - 1.
     * @return A new ColorRGBA with the read color.
     * @throws IllegalArgumentException If x or y are outside the image dimensions
     */
    public ColorRGBA getPixel(int x, int y) { 
        return getPixel(x, y, null);
    }
}
