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
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ImageDifference}, the rule that decides when renderer noise is tolerated and
 * when a screenshot test has found a real change.
 */
public class ImageDifferenceTest {

    @Test
    public void identicalImagesHaveNoDifference() {
        Image reference = blackImage(20, 10);
        Image generated = blackImage(20, 10);

        ImageDifference difference = ImageDifference.of(reference, generated);

        assertEquals(0, difference.getDifferentPixels());
        assertEquals(20 * 10, difference.getTotalPixels());
        assertEquals(0, difference.getWorstDifference());
        assertTrue(difference.isNegligible(), difference.describe());
    }

    @Test
    public void aHandfulOfNoisyPixelsStillCountsAsTheSameImage() {
        Image reference = blackImage(100, 100);
        Image generated = blackImage(100, 100);
        makePixelsDifferent(generated, 5);

        ImageDifference difference = ImageDifference.of(reference, generated);

        assertEquals(5, difference.getDifferentPixels());
        assertEquals(255, difference.getWorstDifference(), "a noisy pixel is a fully different pixel");
        assertEquals(ImageDifference.MINIMUM_ALLOWED_DIFFERENT_PIXELS,
                difference.getAllowedDifferentPixels());
        assertTrue(difference.isNegligible(), difference.describe());
        assertTrue(difference.describe().contains("5 of 10000 pixels"));
    }

    @Test
    public void moreNoiseThanTheBudgetIsNotTheSameImage() {
        Image reference = blackImage(100, 100);
        Image generated = blackImage(100, 100);
        makePixelsDifferent(generated, ImageDifference.MINIMUM_ALLOWED_DIFFERENT_PIXELS + 1);

        ImageDifference difference = ImageDifference.of(reference, generated);

        assertEquals(ImageDifference.MINIMUM_ALLOWED_DIFFERENT_PIXELS + 1,
                difference.getDifferentPixels());
        assertFalse(difference.isNegligible(), difference.describe());
    }

    @Test
    public void aChangedAreaIsNotTheSameImage() {
        Image reference = blackImage(100, 100);
        Image generated = blackImage(100, 100);
        ImageRaster raster = ImageRaster.create(generated);
        for (int y = 10; y < 40; y++) {
            for (int x = 10; x < 40; x++) {
                raster.setPixel(x, y, ColorRGBA.White);
            }
        }

        ImageDifference difference = ImageDifference.of(reference, generated);

        assertEquals(30 * 30, difference.getDifferentPixels());
        assertFalse(difference.isNegligible(), "a 30x30 change is a real change, not noise");
    }

    @Test
    public void theBudgetScalesWithTheImageSize() {
        // 1280x800 is the size of an emulator screenshot, 500x400 an ordinary desktop screenshot
        assertEquals(204, new ImageDifference(0, 1280 * 800, 0).getAllowedDifferentPixels());
        assertEquals(40, new ImageDifference(0, 500 * 400, 0).getAllowedDifferentPixels());
    }

    @Test
    public void imagesOfDifferentSizesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ImageDifference.of(blackImage(4, 4), blackImage(4, 5)));
    }

    @Test
    public void impossibleCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ImageDifference(11, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new ImageDifference(-1, 10, 0));
    }

    private static Image blackImage(int width, int height) {
        return new Image(Image.Format.RGBA8, width, height,
                ByteBuffer.allocateDirect(width * height * 4), ColorSpace.sRGB);
    }

    /**
     * Sets the first {@code count} pixels of an image to white, so that they differ from the
     * (black) reference by the maximum possible amount.
     */
    private static void makePixelsDifferent(Image image, int count) {
        ImageRaster raster = ImageRaster.create(image);
        for (int i = 0; i < count; i++) {
            raster.setPixel(i % image.getWidth(), i / image.getWidth(), ColorRGBA.White);
        }
    }
}
