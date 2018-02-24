package com.jme3.scene.plugins.blender.textures;

import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jme3tools.converters.ImageToAwt;
import jme3tools.converters.RGB565;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.plugins.blender.textures.io.PixelIOFactory;
import com.jme3.scene.plugins.blender.textures.io.PixelInputOutput;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.util.BufferUtils;

/**
 * This utility class has the methods that deal with images.
 * 
 * @author Marcin Roguski (Kaelthas)
 */
public final class ImageUtils {
    /**
     * Creates an image of the given size and depth.
     * @param format
     *            the image format
     * @param width
     *            the image width
     * @param height
     *            the image height
     * @param depth
     *            the image depth
     * @return the new image instance
     */
    public static Image createEmptyImage(Format format, int width, int height, int depth) {
        int bufferSize = width * height * (format.getBitsPerPixel() >> 3);
        if (depth < 2) {
            return new Image(format, width, height, BufferUtils.createByteBuffer(bufferSize), com.jme3.texture.image.ColorSpace.Linear);
        }
        ArrayList<ByteBuffer> data = new ArrayList<ByteBuffer>(depth);
        for (int i = 0; i < depth; ++i) {
            data.add(BufferUtils.createByteBuffer(bufferSize));
        }
        return new Image(Format.RGB8, width, height, depth, data, com.jme3.texture.image.ColorSpace.Linear);
    }

    /**
     * The method sets a color for the given pixel by merging the two given colors.
     * The lowIntensityColor will be most visible when the pixel has low intensity.
     * The highIntensityColor will be most visible when the pixel has high intensity.
     * 
     * @param pixel
     *            the pixel that will have the colors altered
     * @param lowIntensityColor
     *            the low intensity color
     * @param highIntensityColor
     *            the high intensity color
     * @return the altered pixel (the same instance)
     */
    public static TexturePixel color(TexturePixel pixel, ColorRGBA lowIntensityColor, ColorRGBA highIntensityColor) {
        float intensity = pixel.intensity;
        pixel.fromColor(lowIntensityColor);
        pixel.mult(1 - pixel.intensity);
        pixel.add(highIntensityColor.mult(intensity));
        return pixel;
    }

    /**
     * This method merges two given images. The result is stored in the
     * 'target' image.
     * 
     * @param targetImage
     *            the target image
     * @param sourceImage
     *            the source image
     */
    public static void merge(Image targetImage, Image sourceImage) {
        if (sourceImage.getDepth() != targetImage.getDepth()) {
            throw new IllegalArgumentException("The given images should have the same depth to merge them!");
        }
        if (sourceImage.getWidth() != targetImage.getWidth()) {
            throw new IllegalArgumentException("The given images should have the same width to merge them!");
        }
        if (sourceImage.getHeight() != targetImage.getHeight()) {
            throw new IllegalArgumentException("The given images should have the same height to merge them!");
        }

        PixelInputOutput sourceIO = PixelIOFactory.getPixelIO(sourceImage.getFormat());
        PixelInputOutput targetIO = PixelIOFactory.getPixelIO(targetImage.getFormat());
        TexturePixel sourcePixel = new TexturePixel();
        TexturePixel targetPixel = new TexturePixel();
        int depth = targetImage.getDepth() == 0 ? 1 : targetImage.getDepth();

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
     * This method merges two given images. The result is stored in the
     * 'target' image.
     * 
     * @param targetImage
     *            the target image
     * @param sourceImage
     *            the source image
     */
    public static void mix(Image targetImage, Image sourceImage) {
        if (sourceImage.getDepth() != targetImage.getDepth()) {
            throw new IllegalArgumentException("The given images should have the same depth to merge them!");
        }
        if (sourceImage.getWidth() != targetImage.getWidth()) {
            throw new IllegalArgumentException("The given images should have the same width to merge them!");
        }
        if (sourceImage.getHeight() != targetImage.getHeight()) {
            throw new IllegalArgumentException("The given images should have the same height to merge them!");
        }

        PixelInputOutput sourceIO = PixelIOFactory.getPixelIO(sourceImage.getFormat());
        PixelInputOutput targetIO = PixelIOFactory.getPixelIO(targetImage.getFormat());
        TexturePixel sourcePixel = new TexturePixel();
        TexturePixel targetPixel = new TexturePixel();
        int depth = targetImage.getDepth() == 0 ? 1 : targetImage.getDepth();

        for (int layerIndex = 0; layerIndex < depth; ++layerIndex) {
            for (int x = 0; x < sourceImage.getWidth(); ++x) {
                for (int y = 0; y < sourceImage.getHeight(); ++y) {
                    sourceIO.read(sourceImage, layerIndex, sourcePixel, x, y);
                    targetIO.read(targetImage, layerIndex, targetPixel, x, y);
                    targetPixel.mix(sourcePixel);
                    targetIO.write(targetImage, layerIndex, targetPixel, x, y);
                }
            }
        }
    }

    /**
     * Resizes the image to the given width and height.
     * @param source
     *            the source image (this remains untouched, the new image instance is created)
     * @param width
     *            the target image width
     * @param height
     *            the target image height
     * @return the resized image
     */
    public static Image resizeTo(Image source, int width, int height) {
        BufferedImage sourceImage = ImageToAwt.convert(source, false, false, 0);

        double scaleX = width / (double) sourceImage.getWidth();
        double scaleY = height / (double) sourceImage.getHeight();
        AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
        AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);

        BufferedImage scaledImage = bilinearScaleOp.filter(sourceImage, new BufferedImage(width, height, sourceImage.getType()));
        return ImageUtils.toJmeImage(scaledImage, source.getFormat());
    }

    /**
     * This method converts the given texture into normal-map texture.
     * 
     * @param source
     *            the source texture
     * @param strengthFactor
     *            the normal strength factor
     * @return normal-map texture
     */
    public static Image convertToNormalMapTexture(Image source, float strengthFactor) {
        BufferedImage sourceImage = ImageToAwt.convert(source, false, false, 0);

        BufferedImage heightMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        BufferedImage bumpMap = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        ColorConvertOp gscale = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        gscale.filter(sourceImage, heightMap);

        Vector3f S = new Vector3f();
        Vector3f T = new Vector3f();
        Vector3f N = new Vector3f();

        for (int x = 0; x < bumpMap.getWidth(); ++x) {
            for (int y = 0; y < bumpMap.getHeight(); ++y) {
                // generating bump pixel
                S.x = 1;
                S.y = 0;
                S.z = strengthFactor * ImageUtils.getHeight(heightMap, x + 1, y) - strengthFactor * ImageUtils.getHeight(heightMap, x - 1, y);
                T.x = 0;
                T.y = 1;
                T.z = strengthFactor * ImageUtils.getHeight(heightMap, x, y + 1) - strengthFactor * ImageUtils.getHeight(heightMap, x, y - 1);

                float den = (float) Math.sqrt(S.z * S.z + T.z * T.z + 1);
                N.x = -S.z;
                N.y = -T.z;
                N.z = 1;
                N.divideLocal(den);

                // setting the pixel in the result image
                bumpMap.setRGB(x, y, ImageUtils.vectorToColor(N.x, N.y, N.z));
            }
        }
        return ImageUtils.toJmeImage(bumpMap, source.getFormat());
    }

    /**
     * This method converts the given texture into black and whit (grayscale) texture.
     * 
     * @param source
     *            the source texture
     * @return grayscale texture
     */
    public static Image convertToGrayscaleTexture(Image source) {
        BufferedImage sourceImage = ImageToAwt.convert(source, false, false, 0);
        ColorConvertOp op = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
        op.filter(sourceImage, sourceImage);
        return ImageUtils.toJmeImage(sourceImage, source.getFormat());
    }

    /**
     * This method decompresses the given image. If the given image is already
     * decompressed nothing happens and it is simply returned.
     * 
     * @param image
     *            the image to decompress
     * @return the decompressed image
     */
    public static Image decompress(Image image) {
        Format format = image.getFormat();
        int depth = image.getDepth();
        if (depth == 0) {
            depth = 1;
        }
        ArrayList<ByteBuffer> dataArray = new ArrayList<ByteBuffer>(depth);
        int[] sizes = image.getMipMapSizes() != null ? image.getMipMapSizes() : new int[1];
        int[] newMipmapSizes = image.getMipMapSizes() != null ? new int[image.getMipMapSizes().length] : null;

        for (int dataLayerIndex = 0; dataLayerIndex < depth; ++dataLayerIndex) {
            ByteBuffer data = image.getData(dataLayerIndex);
            data.rewind();
            if (sizes.length == 1) {
                sizes[0] = data.remaining();
            }
            float widthToHeightRatio = image.getWidth() / image.getHeight();// this should always be constant for each mipmap
            List<DDSTexelData> texelDataList = new ArrayList<DDSTexelData>(sizes.length);
            int maxPosition = 0, resultSize = 0;

            for (int sizeIndex = 0; sizeIndex < sizes.length; ++sizeIndex) {
                maxPosition += sizes[sizeIndex];
                DDSTexelData texelData = new DDSTexelData(sizes[sizeIndex], widthToHeightRatio, format);
                texelDataList.add(texelData);
                switch (format) {
                    case DXT1:// BC1
                    case DXT1A:
                        while (data.position() < maxPosition) {
                            TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
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
                        while (data.position() < maxPosition) {
                            TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
                            long alpha = data.getLong();
                            float[] alphas = new float[16];
                            long alphasIndex = 0;
                            for (int i = 0; i < 16; ++i) {
                                alphasIndex |= i << i * 4;
                                byte a = (byte) ((alpha >> i * 4 & 0x0F) << 4);
                                alphas[i] = a >= 0 ? a / 255.0f : 1.0f - ~a / 255.0f;
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
                        float[] alphas = new float[8];
                        while (data.position() < maxPosition) {
                            TexturePixel[] colors = new TexturePixel[] { new TexturePixel(), new TexturePixel(), new TexturePixel(), new TexturePixel() };
                            alphas[0] = data.get() * 255.0f;
                            alphas[1] = data.get() * 255.0f;
                            //the casts to long must be done here because otherwise 32-bit integers would be shifetd by 32 and 40 bits which would result in improper values
                            long alphaIndices = data.get() | (long)data.get() << 8 | (long)data.get() << 16 | (long)data.get() << 24 | (long)data.get() << 32 | (long)data.get() << 40;
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

                            int indexes = data.getInt();// 4-byte table with color indexes in decompressed table
                            texelData.add(colors, indexes, alphas, alphaIndices);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown compressed format: " + format);
                }
                newMipmapSizes[sizeIndex] = texelData.getSizeInBytes();
                resultSize += texelData.getSizeInBytes();
            }
            byte[] bytes = new byte[resultSize];
            int offset = 0;
            byte[] pixelBytes = new byte[4];
            for (DDSTexelData texelData : texelDataList) {
                for (int i = 0; i < texelData.getPixelWidth(); ++i) {
                    for (int j = 0; j < texelData.getPixelHeight(); ++j) {
                        if (texelData.getRGBA8(i, j, pixelBytes)) {
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4] = pixelBytes[0];
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4 + 1] = pixelBytes[1];
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4 + 2] = pixelBytes[2];
                            bytes[offset + (j * texelData.getPixelWidth() + i) * 4 + 3] = pixelBytes[3];
                        } else {
                            break;
                        }
                    }
                }
                offset += texelData.getSizeInBytes();
            }
            dataArray.add(BufferUtils.createByteBuffer(bytes));
        }

        Image result = depth > 1 ? new Image(Format.RGBA8, image.getWidth(), image.getHeight(), depth, dataArray, com.jme3.texture.image.ColorSpace.Linear) : 
                                   new Image(Format.RGBA8, image.getWidth(), image.getHeight(), dataArray.get(0), com.jme3.texture.image.ColorSpace.Linear);
        if (newMipmapSizes != null) {
            result.setMipMapSizes(newMipmapSizes);
        }
        return result;
    }

    /**
     * This method returns the height represented by the specified pixel in the
     * given texture. The given texture should be a height-map.
     * 
     * @param image
     *            the height-map texture
     * @param x
     *            pixel's X coordinate
     * @param y
     *            pixel's Y coordinate
     * @return height represented by the given texture in the specified location
     */
    private static int getHeight(BufferedImage image, int x, int y) {
        if (x < 0) {
            x = 0;
        } else if (x >= image.getWidth()) {
            x = image.getWidth() - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y >= image.getHeight()) {
            y = image.getHeight() - 1;
        }
        return image.getRGB(x, y) & 0xff;
    }

    /**
     * This method transforms given vector's coordinates into ARGB color (A is
     * always = 255).
     * 
     * @param x
     *            X factor of the vector
     * @param y
     *            Y factor of the vector
     * @param z
     *            Z factor of the vector
     * @return color representation of the given vector
     */
    private static int vectorToColor(float x, float y, float z) {
        int r = Math.round(255 * (x + 1f) / 2f);
        int g = Math.round(255 * (y + 1f) / 2f);
        int b = Math.round(255 * (z + 1f) / 2f);
        return (255 << 24) + (r << 16) + (g << 8) + b;
    }

    /**
     * Converts java awt image to jme image.
     * @param bufferedImage
     *            the java awt image
     * @param format
     *            the result image format
     * @return the jme image
     */
    private static Image toJmeImage(BufferedImage bufferedImage, Format format) {
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(bufferedImage.getWidth() * bufferedImage.getHeight() * 3);
        ImageToAwt.convert(bufferedImage, format, byteBuffer);
        return new Image(format, bufferedImage.getWidth(), bufferedImage.getHeight(), byteBuffer, com.jme3.texture.image.ColorSpace.Linear);
    }
}
