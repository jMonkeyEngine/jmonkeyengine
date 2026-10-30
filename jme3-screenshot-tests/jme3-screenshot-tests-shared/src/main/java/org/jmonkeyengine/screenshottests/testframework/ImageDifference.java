/*
 * Copyright (c) 2026 jMonkeyEngine
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
package org.jmonkeyengine.screenshottests.testframework;

import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.image.ImageRaster;

/**
 * Measures how different two screenshots are and decides whether that difference is small enough
 * for a screenshot test to treat them as the same image.
 *
 * <p>
 * The screenshot tests compare the output of software renderers (the desktop job renders with
 * Mesa, the Android job renders with the emulator's GLES renderer) against reference images that
 * were captured on the CI machines. Those renderers do not rasterise, sample and blend
 * bit-for-bit identically on every host, so the very same commit regularly produces an image in
 * which a handful of pixels differ from the reference even though nothing about jME has changed.
 * Demanding that every single pixel matches therefore fails the build for a reason no code change
 * can fix, and because the difference is a property of the machine the test runs on, re-running
 * the test there (which is what the CI retries do) reproduces it exactly.
 * </p>
 *
 * <p>
 * A real rendering regression is different in kind, not just in degree: it changes a large part
 * of the image rather than a few pixels. The rule applied here is therefore that the images count
 * as the same when no more than {@link #ALLOWED_DIFFERENT_PIXEL_RATIO} of the pixels (and at
 * least {@link #MINIMUM_ALLOWED_DIFFERENT_PIXELS}, so that small images still get a usable
 * budget) differ from the reference by more than {@link #PIXEL_TOLERANCE} on any colour channel.
 * </p>
 *
 * @author jaime-jmebot
 */
public final class ImageDifference {

    /**
     * A pixel counts as different when any of its colour channels differs from the reference by
     * more than this value. It is deliberately the same threshold that
     * {@link PixelSamenessDegree#NEGLIGIBLY_DIFFERENT} marks as "negligibly different" in the
     * generated diff images.
     */
    public static final int PIXEL_TOLERANCE =
            PixelSamenessDegree.NEGLIGIBLY_DIFFERENT.getMaximumAllowedDifference();

    /**
     * The fraction of an image that may differ by more than {@link #PIXEL_TOLERANCE} and still be
     * accepted as the same image. 0.02% is a few pixels on the small test images and a couple of
     * hundred pixels on a full screen emulator screenshot, which is far more than the handful of
     * pixels renderer rounding produces and far less than any visible change to a scene.
     */
    public static final float ALLOWED_DIFFERENT_PIXEL_RATIO = 0.0002f;

    /**
     * The smallest allowance {@link #ALLOWED_DIFFERENT_PIXEL_RATIO} may yield, so that small test
     * images are not compared with an allowance of (nearly) zero pixels.
     */
    public static final int MINIMUM_ALLOWED_DIFFERENT_PIXELS = 10;

    private final int differentPixels;

    private final int totalPixels;

    private final int worstDifference;

    /**
     * Creates a difference from already counted pixel values.
     *
     * @param differentPixels how many pixels differed by more than {@link #PIXEL_TOLERANCE}
     * @param totalPixels how many pixels were compared
     * @param worstDifference the largest single colour channel difference found
     */
    public ImageDifference(int differentPixels, int totalPixels, int worstDifference) {
        if (differentPixels < 0 || totalPixels < 0 || differentPixels > totalPixels) {
            throw new IllegalArgumentException("differentPixels (" + differentPixels
                    + ") must be between 0 and totalPixels (" + totalPixels + ")");
        }
        this.differentPixels = differentPixels;
        this.totalPixels = totalPixels;
        this.worstDifference = worstDifference;
    }

    /**
     * Measures the difference between two images of the same size.
     *
     * @param image1 one image
     * @param image2 the image to compare it with
     * @return the measured difference
     * @throws IllegalArgumentException if the images do not have the same dimensions
     */
    public static ImageDifference of(Image image1, Image image2) {
        if (image1.getWidth() != image2.getWidth() || image1.getHeight() != image2.getHeight()) {
            throw new IllegalArgumentException("Images must have the same size: "
                    + image1.getWidth() + "x" + image1.getHeight() + " vs "
                    + image2.getWidth() + "x" + image2.getHeight());
        }

        ImageRaster image1Raster = ImageRaster.create(image1);
        ImageRaster image2Raster = ImageRaster.create(image2);

        ColorRGBA color1 = new ColorRGBA();
        ColorRGBA color2 = new ColorRGBA();

        int differentPixels = 0;
        int worstDifference = 0;

        for (int y = 0; y < image1.getHeight(); y++) {
            for (int x = 0; x < image1.getWidth(); x++) {
                image1Raster.getPixel(x, y, color1);
                image2Raster.getPixel(x, y, color2);

                int difference = maximumComponentDifference(color1.asIntARGB(), color2.asIntARGB());

                if (difference > worstDifference) {
                    worstDifference = difference;
                }
                if (difference > PIXEL_TOLERANCE) {
                    differentPixels++;
                }
            }
        }

        return new ImageDifference(differentPixels, image1.getWidth() * image1.getHeight(),
                worstDifference);
    }

    /**
     * Compares two pixels and returns the difference of the colour channel that differs most.
     *
     * @param pixel1 a pixel in ARGB order
     * @param pixel2 the pixel to compare it with, in ARGB order
     * @return the largest difference (0 to 255) between the two pixels
     */
    public static int maximumComponentDifference(int pixel1, int pixel2) {
        int r1 = (pixel1 >> 16) & 0xFF;
        int g1 = (pixel1 >> 8) & 0xFF;
        int b1 = pixel1 & 0xFF;
        int a1 = (pixel1 >> 24) & 0xFF;

        int r2 = (pixel2 >> 16) & 0xFF;
        int g2 = (pixel2 >> 8) & 0xFF;
        int b2 = pixel2 & 0xFF;
        int a2 = (pixel2 >> 24) & 0xFF;

        return Math.max(Math.abs(r1 - r2),
                Math.max(Math.abs(g1 - g2), Math.max(Math.abs(b1 - b2), Math.abs(a1 - a2))));
    }

    /**
     * @return how many pixels differed by more than {@link #PIXEL_TOLERANCE}
     */
    public int getDifferentPixels() {
        return differentPixels;
    }

    /**
     * @return how many pixels were compared
     */
    public int getTotalPixels() {
        return totalPixels;
    }

    /**
     * @return the largest single colour channel difference found, 0 when the images are identical
     */
    public int getWorstDifference() {
        return worstDifference;
    }

    /**
     * @return how many pixels are allowed to differ before the images stop counting as the same
     */
    public int getAllowedDifferentPixels() {
        return Math.max(MINIMUM_ALLOWED_DIFFERENT_PIXELS,
                (int) (totalPixels * ALLOWED_DIFFERENT_PIXEL_RATIO));
    }

    /**
     * @return true when the difference is small enough to be renderer noise rather than a change
     *         in what was drawn
     */
    public boolean isNegligible() {
        return differentPixels <= getAllowedDifferentPixels();
    }

    /**
     * @return a human readable summary of the difference, for the test report and failure messages
     */
    public String describe() {
        return differentPixels + " of " + totalPixels + " pixels differ by more than "
                + PIXEL_TOLERANCE + " (at most " + getAllowedDifferentPixels()
                + " tolerated), largest single channel difference " + worstDifference;
    }
}
