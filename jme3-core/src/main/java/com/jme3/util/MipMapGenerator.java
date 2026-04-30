/*
 * Copyright (c) 2009-2026 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.util;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

public final class MipMapGenerator {

    private static final float EPSILON_ALPHA = 1e-8f;

    private MipMapGenerator() {
    }

    /**
     * Scales the base level of a 2D image.
     *
     * The returned image keeps the same Image.Format and ColorSpace as the input.
     * Pixel format conversion is delegated to ImageRaster.
     *
     * For normal color textures, this method filters in linear space.
     */
    public static Image scaleImage(Image inputImage, int outputWidth, int outputHeight) {
        return scaleImage(inputImage, outputWidth, outputHeight, true, isSrgb(inputImage));
    }

    /**
     * Scales the base level of a 2D image.
     *
     * @param convertToLinear if true, ImageRaster exposes pixels to this code in linear space
     * @param alphaWeighted   if true, RGB is filtered weighted by alpha to reduce transparent-edge halos
     */
    public static Image scaleImage(Image inputImage,
                                   int outputWidth,
                                   int outputHeight,
                                   boolean convertToLinear,
                                   boolean alphaWeighted) {
        return scaleLevel(inputImage, 0, outputWidth, outputHeight, convertToLinear, alphaWeighted);
    }

    public static Image resizeToPowerOf2(Image original) {
        int potWidth = FastMath.nearestPowerOfTwo(original.getWidth());
        int potHeight = FastMath.nearestPowerOfTwo(original.getHeight());
        return scaleImage(original, potWidth, potHeight);
    }

    /**
     * Returns true if this image has CPU-side data in a format supported by this
     * mipmap generator.
     *
     * This generator works on uncompressed, non-depth, byte-addressable texture
     * formats supported by ImageRaster.
     */
    public static boolean canGenerateMipmaps(Image image) {
        if (image == null
                || image.getWidth() < 1
                || image.getHeight() < 1
                || image.getDepth() > 1
                || image.getFormat().isCompressed()
                || image.getFormat().isDepthFormat()
                || image.getData() == null
                || image.getData().isEmpty()) {
            return false;
        }

        int bitsPerPixel = image.getFormat().getBitsPerPixel();
        if (bitsPerPixel <= 0 || (bitsPerPixel % 8) != 0) {
            return false;
        }

        int baseLevelSize;
        try {
            baseLevelSize = levelSize(image.getFormat(), image.getWidth(), image.getHeight());
        } catch (RuntimeException exception) {
            return false;
        }

        for (ByteBuffer data : image.getData()) {
            if (data == null || data.capacity() < baseLevelSize) {
                return false;
            }
        }

        return ImageRaster.isSupported(image.getFormat());
    }

    /**
     * Generates a complete mip chain for the image.
     *
     * Default behavior is intended for normal color/albedo textures:
     * - filtering is done in linear space;
     * - sRGB images with alpha use alpha-weighted RGB filtering.
     *
     * For normal maps, roughness, metallic, AO, height maps, or packed data maps,
     * prefer generateMipMaps(image, true, false), assuming the image is not marked sRGB.
     */
    public static void generateMipMaps(Image image) {
        generateMipMaps(image, true, isSrgb(image));
    }

    /**
     * Generates a complete mip chain for every data buffer/slice in the image.
     *
     * @param convertToLinear if true, ImageRaster exposes pixels to this code in linear space
     * @param alphaWeighted   if true, RGB is filtered weighted by alpha
     */
    public static void generateMipMaps(Image image, boolean convertToLinear, boolean alphaWeighted) {
        validateImage(image);

        int baseWidth = image.getWidth();
        int baseHeight = image.getHeight();

        ArrayList<MipChain> chains = new ArrayList<>(image.getData().size());
        int dataCount = image.getData().size();

        for (int dataIndex = 0; dataIndex < dataCount; dataIndex++) {
            chains.add(generateMipChainForSlice(
                    image,
                    dataIndex,
                    baseWidth,
                    baseHeight,
                    convertToLinear,
                    alphaWeighted
            ));
        }

        for (int dataIndex = 0; dataIndex < chains.size(); dataIndex++) {
            image.setData(dataIndex, chains.get(dataIndex).combinedData);
        }

        if (!chains.isEmpty()) {
            image.setMipMapSizes(chains.get(0).mipSizes);
        }
    }

    private static MipChain generateMipChainForSlice(Image sourceImage,
                                                     int sourceSlice,
                                                     int baseWidth,
                                                     int baseHeight,
                                                     boolean convertToLinear,
                                                     boolean alphaWeighted) {
        ArrayList<ByteBuffer> levels = new ArrayList<>();

        Image.Format format = sourceImage.getFormat();
        ColorSpace colorSpace = sourceImage.getColorSpace();

        ByteBuffer baseLevel = copyBaseLevel(
                sourceImage.getData(sourceSlice),
                levelSize(format, baseWidth, baseHeight)
        );

        Image current = new Image(format, baseWidth, baseHeight, baseLevel, colorSpace);
        levels.add(baseLevel);

        int width = baseWidth;
        int height = baseHeight;

        while (width > 1 || height > 1) {
            int nextWidth = Math.max(1, width / 2);
            int nextHeight = Math.max(1, height / 2);

            Image next = scaleLevel(
                    current,
                    0,
                    nextWidth,
                    nextHeight,
                    convertToLinear,
                    alphaWeighted
            );

            levels.add(next.getData(0));

            current = next;
            width = nextWidth;
            height = nextHeight;
        }

        int totalSize = 0;
        int[] mipSizes = new int[levels.size()];

        for (int i = 0; i < levels.size(); i++) {
            int size = levels.get(i).capacity();
            mipSizes[i] = size;
            totalSize += size;
        }

        ByteBuffer combined = BufferUtils.createByteBuffer(totalSize);

        for (ByteBuffer level : levels) {
            ByteBuffer duplicate = level.duplicate();
            duplicate.clear();
            combined.put(duplicate);
        }

        combined.flip();

        return new MipChain(combined, mipSizes);
    }

    private static Image scaleLevel(Image inputImage,
                                    int inputSlice,
                                    int outputWidth,
                                    int outputHeight,
                                    boolean convertToLinear,
                                    boolean alphaWeighted) {
        if (outputWidth < 1 || outputHeight < 1) {
            throw new IllegalArgumentException("Output size must be at least 1x1");
        }

        validateImage(inputImage);

        int outputSize = levelSize(inputImage.getFormat(), outputWidth, outputHeight);
        ByteBuffer outputBuffer = BufferUtils.createByteBuffer(outputSize);

        Image outputImage = new Image(
                inputImage.getFormat(),
                outputWidth,
                outputHeight,
                outputBuffer,
                inputImage.getColorSpace()
        );

        ImageRaster input = ImageRaster.create(inputImage, inputSlice, 0, convertToLinear);
        ImageRaster output = ImageRaster.create(outputImage, 0, 0, convertToLinear);

        boolean downscale = outputWidth <= input.getWidth() && outputHeight <= input.getHeight();
        boolean clampOutput = !inputImage.getFormat().isFloatingPont();

        if (downscale) {
            areaResample(input, output, alphaWeighted, clampOutput);
        } else {
            bilinearResample(input, output, alphaWeighted, clampOutput);
        }

        return outputImage;
    }

    /**
     * Area filter.
     *
     * This is the right default for mipmap generation because every destination
     * pixel represents the average area of the corresponding source rectangle.
     */
    private static void areaResample(ImageRaster input,
                                     ImageRaster output,
                                     boolean alphaWeighted,
                                     boolean clampOutput) {
        int sourceWidth = input.getWidth();
        int sourceHeight = input.getHeight();

        int targetWidth = output.getWidth();
        int targetHeight = output.getHeight();

        double scaleX = (double) sourceWidth / (double) targetWidth;
        double scaleY = (double) sourceHeight / (double) targetHeight;

        ColorRGBA sample = new ColorRGBA();
        ColorRGBA result = new ColorRGBA();
        PixelAccumulator accumulator = new PixelAccumulator();

        for (int y = 0; y < targetHeight; y++) {
            double sourceY0 = y * scaleY;
            double sourceY1 = (y + 1) * scaleY;

            int yStart = Math.max(0, (int) Math.floor(sourceY0));
            int yEnd = Math.min(sourceHeight, Math.max(yStart + 1, (int) Math.ceil(sourceY1)));

            for (int x = 0; x < targetWidth; x++) {
                double sourceX0 = x * scaleX;
                double sourceX1 = (x + 1) * scaleX;

                int xStart = Math.max(0, (int) Math.floor(sourceX0));
                int xEnd = Math.min(sourceWidth, Math.max(xStart + 1, (int) Math.ceil(sourceX1)));

                accumulator.clear();

                for (int sy = yStart; sy < yEnd; sy++) {
                    double overlapY0 = Math.max(sourceY0, sy);
                    double overlapY1 = Math.min(sourceY1, sy + 1.0);
                    float weightY = (float) Math.max(0.0, overlapY1 - overlapY0);

                    if (weightY <= 0f) {
                        continue;
                    }

                    for (int sx = xStart; sx < xEnd; sx++) {
                        double overlapX0 = Math.max(sourceX0, sx);
                        double overlapX1 = Math.min(sourceX1, sx + 1.0);
                        float weightX = (float) Math.max(0.0, overlapX1 - overlapX0);

                        if (weightX <= 0f) {
                            continue;
                        }

                        float weight = weightX * weightY;

                        input.getPixel(sx, sy, sample);
                        accumulator.add(sample, weight, alphaWeighted, clampOutput);
                    }
                }

                accumulator.toColor(result, alphaWeighted, clampOutput);
                output.setPixel(x, y, result);
            }
        }
    }

    /**
     * Bilinear filter.
     *
     * Used only when scaleImage() is asked to upscale.
     * Mipmap generation itself normally uses areaResample().
     */
    private static void bilinearResample(ImageRaster input,
                                         ImageRaster output,
                                         boolean alphaWeighted,
                                         boolean clampOutput) {
        int sourceWidth = input.getWidth();
        int sourceHeight = input.getHeight();

        int targetWidth = output.getWidth();
        int targetHeight = output.getHeight();

        double scaleX = (double) sourceWidth / (double) targetWidth;
        double scaleY = (double) sourceHeight / (double) targetHeight;

        ColorRGBA sample = new ColorRGBA();
        ColorRGBA result = new ColorRGBA();
        PixelAccumulator accumulator = new PixelAccumulator();

        for (int y = 0; y < targetHeight; y++) {
            double sourceY = (y + 0.5) * scaleY - 0.5;

            int y0 = (int) Math.floor(sourceY);
            double ty = sourceY - y0;

            if (y0 < 0) {
                y0 = 0;
                ty = 0.0;
            }

            int y1 = y0 + 1;

            if (y1 >= sourceHeight) {
                y1 = sourceHeight - 1;
                y0 = y1;
                ty = 0.0;
            }

            float wy0 = (float) (1.0 - ty);
            float wy1 = (float) ty;

            for (int x = 0; x < targetWidth; x++) {
                double sourceX = (x + 0.5) * scaleX - 0.5;

                int x0 = (int) Math.floor(sourceX);
                double tx = sourceX - x0;

                if (x0 < 0) {
                    x0 = 0;
                    tx = 0.0;
                }

                int x1 = x0 + 1;

                if (x1 >= sourceWidth) {
                    x1 = sourceWidth - 1;
                    x0 = x1;
                    tx = 0.0;
                }

                float wx0 = (float) (1.0 - tx);
                float wx1 = (float) tx;

                accumulator.clear();

                input.getPixel(x0, y0, sample);
                accumulator.add(sample, wx0 * wy0, alphaWeighted, clampOutput);

                input.getPixel(x1, y0, sample);
                accumulator.add(sample, wx1 * wy0, alphaWeighted, clampOutput);

                input.getPixel(x0, y1, sample);
                accumulator.add(sample, wx0 * wy1, alphaWeighted, clampOutput);

                input.getPixel(x1, y1, sample);
                accumulator.add(sample, wx1 * wy1, alphaWeighted, clampOutput);

                accumulator.toColor(result, alphaWeighted, clampOutput);
                output.setPixel(x, y, result);
            }
        }
    }

    private static void validateImage(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }

        if (image.getWidth() < 1 || image.getHeight() < 1) {
            throw new IllegalArgumentException("Image size must be at least 1x1");
        }

        if (image.getData() == null || image.getData().isEmpty()) {
            throw new IllegalArgumentException("Image has no data buffers");
        }

        int bitsPerPixel = image.getFormat().getBitsPerPixel();

        if (bitsPerPixel <= 0 || (bitsPerPixel % 8) != 0) {
            throw new UnsupportedOperationException(
                    "CPU mipmap generation requires byte-addressable formats. Unsupported format: "
                            + image.getFormat()
                            + " with "
                            + bitsPerPixel
                            + " bits per pixel"
            );
        }

        int baseLevelSize = levelSize(image.getFormat(), image.getWidth(), image.getHeight());
        for (int dataIndex = 0; dataIndex < image.getData().size(); dataIndex++) {
            ByteBuffer data = image.getData(dataIndex);
            if (data == null) {
                throw new IllegalArgumentException("Image data buffer " + dataIndex + " is null");
            }
            if (data.capacity() < baseLevelSize) {
                throw new IllegalArgumentException(
                        "Image data buffer " + dataIndex + " is smaller than expected base level size. Data capacity="
                                + data.capacity()
                                + ", expected="
                                + baseLevelSize
                );
            }
        }
    }

    private static int levelSize(Image.Format format, int width, int height) {
        int bitsPerPixel = format.getBitsPerPixel();

        long bits = (long) width * (long) height * (long) bitsPerPixel;

        if ((bits % 8L) != 0L) {
            throw new UnsupportedOperationException(
                    "Image level is not byte-addressable: "
                            + width
                            + "x"
                            + height
                            + " "
                            + format
            );
        }

        long bytes = bits / 8L;

        if (bytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(
                    "Image level is too large: "
                            + width
                            + "x"
                            + height
                            + " "
                            + format
            );
        }

        return (int) bytes;
    }

    /**
     * If the input image already has mipmaps, its ByteBuffer may contain all levels.
     * For rebuilding mipmaps, we only want the base level.
     */
    private static ByteBuffer copyBaseLevel(ByteBuffer source, int baseLevelSize) {
        if (source == null) {
            throw new IllegalArgumentException("Image data buffer is null");
        }
        if (source.capacity() < baseLevelSize) {
            throw new IllegalArgumentException(
                    "Image data is smaller than expected base level size. Data capacity="
                            + source.capacity()
                            + ", expected="
                            + baseLevelSize
            );
        }

        ByteBuffer duplicate = source.duplicate();
        duplicate.clear();
        duplicate.limit(baseLevelSize);

        ByteBuffer copy = BufferUtils.createByteBuffer(baseLevelSize);
        copy.put(duplicate);
        copy.flip();

        return copy;
    }

    private static boolean isSrgb(Image image) {
        if (image.getColorSpace() == ColorSpace.sRGB) {
            return true;
        }

        String formatName = image.getFormat().name().toLowerCase(Locale.ROOT);
        return formatName.contains("srgb");
    }

    private static final class MipChain {
        final ByteBuffer combinedData;
        final int[] mipSizes;

        MipChain(ByteBuffer combinedData, int[] mipSizes) {
            this.combinedData = combinedData;
            this.mipSizes = mipSizes;
        }
    }

    private static final class PixelAccumulator {
        private float r;
        private float g;
        private float b;
        private float a;
        private float weight;

        void clear() {
            r = 0f;
            g = 0f;
            b = 0f;
            a = 0f;
            weight = 0f;
        }

        void add(ColorRGBA color, float sampleWeight, boolean alphaWeighted, boolean clampOutput) {
            if (sampleWeight <= 0f) {
                return;
            }

            float alpha = clampOutput ? clamp01(color.a) : color.a;

            if (alphaWeighted) {
                r += color.r * alpha * sampleWeight;
                g += color.g * alpha * sampleWeight;
                b += color.b * alpha * sampleWeight;
            } else {
                r += color.r * sampleWeight;
                g += color.g * sampleWeight;
                b += color.b * sampleWeight;
            }

            a += alpha * sampleWeight;
            weight += sampleWeight;
        }

        void toColor(ColorRGBA store, boolean alphaWeighted, boolean clampOutput) {
            if (weight <= 0f) {
                store.set(0f, 0f, 0f, 0f);
                return;
            }

            float outA = a / weight;

            float outR;
            float outG;
            float outB;

            if (alphaWeighted) {
                if (a > EPSILON_ALPHA) {
                    outR = r / a;
                    outG = g / a;
                    outB = b / a;
                } else {
                    outR = 0f;
                    outG = 0f;
                    outB = 0f;
                }
            } else {
                outR = r / weight;
                outG = g / weight;
                outB = b / weight;
            }

            if (clampOutput) {
                outR = clamp01(outR);
                outG = clamp01(outG);
                outB = clamp01(outB);
                outA = clamp01(outA);
            }

            store.set(outR, outG, outB, outA);
        }

        private static float clamp01(float value) {
            if (value <= 0f) {
                return 0f;
            }

            if (value >= 1f) {
                return 1f;
            }

            return value;
        }
    }
}
