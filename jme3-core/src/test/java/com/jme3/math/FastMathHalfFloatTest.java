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
package com.jme3.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jme3.texture.Image;
import com.jme3.texture.image.ColorSpace;
import com.jme3.texture.image.ImageRaster;
import com.jme3.util.BufferUtils;
import org.junit.jupiter.api.Test;

class FastMathHalfFloatTest {
    @Test
    void everyBinary16PatternDecodesWithTheCorrectClassificationAndFiniteValue() {
        for (int bits = 0; bits <= 0xffff; bits++) {
            int exponent = (bits >>> 10) & 31;
            int fraction = bits & 1023;
            float decoded = FastMath.convertHalfToFloat((short) bits);
            if (exponent == 31 && fraction != 0) {
                assertTrue(Float.isNaN(decoded), "binary16 NaN " + bits);
                continue;
            }
            float magnitude = exponent == 31 ? Float.POSITIVE_INFINITY
                    : exponent == 0 ? Math.scalb((float) fraction, -24)
                    : Math.scalb((float) (1024 + fraction), exponent - 25);
            float expected = (bits & 0x8000) == 0 ? magnitude : -magnitude;
            assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(decoded),
                    "exact binary16 decode " + bits);
            assertEquals((short) bits, FastMath.convertFloatToHalf(decoded),
                    "exact representable round trip " + bits);
        }
    }

    @Test
    void everyFinitePositiveMidpointAndItsNeighborsRoundToNearestEven() {
        for (int lower = 0; lower < 0x7bff; lower++) {
            float a = FastMath.convertHalfToFloat((short) lower);
            float b = FastMath.convertHalfToFloat((short) (lower + 1));
            float midpoint = (a + b) * .5f;
            int even = (lower & 1) == 0 ? lower : lower + 1;
            assertEquals((short) lower, FastMath.convertFloatToHalf(Math.nextDown(midpoint)));
            assertEquals((short) even, FastMath.convertFloatToHalf(midpoint));
            assertEquals((short) (lower + 1), FastMath.convertFloatToHalf(Math.nextUp(midpoint)));
            assertEquals((short) (0x8000 | lower), FastMath.convertFloatToHalf(-Math.nextDown(midpoint)));
            assertEquals((short) (0x8000 | even), FastMath.convertFloatToHalf(-midpoint));
            assertEquals((short) (0x8000 | (lower + 1)), FastMath.convertFloatToHalf(-Math.nextUp(midpoint)));
        }
    }

    @Test
    void signedZerosInfinitiesAndFiniteSaturationKeepTheirContracts() {
        assertEquals((short) 0, FastMath.convertFloatToHalf(0f));
        assertEquals((short) 0x8000, FastMath.convertFloatToHalf(-0f));
        assertEquals((short) 0x7c00, FastMath.convertFloatToHalf(Float.POSITIVE_INFINITY));
        assertEquals((short) 0xfc00, FastMath.convertFloatToHalf(Float.NEGATIVE_INFINITY));
        assertEquals((short) 0x7bff, FastMath.convertFloatToHalf(Float.MAX_VALUE));
        assertEquals((short) 0xfbff, FastMath.convertFloatToHalf(-Float.MAX_VALUE));
        assertThrows(UnsupportedOperationException.class, () -> FastMath.convertFloatToHalf(Float.NaN));
    }

    @Test
    void hdrImageRasterStoresSmallRadianceAndNegativeZeroWithoutInflation() {
        Image image = new Image(Image.Format.RGBA16F, 1, 1, BufferUtils.createByteBuffer(8), ColorSpace.Linear);
        ImageRaster raster = ImageRaster.create(image);
        raster.setPixel(0, 0, new ColorRGBA(0x1p-24f, -0f, 2f, 1f));
        ColorRGBA decoded = raster.getPixel(0, 0);
        assertEquals(0x1p-24f, decoded.r, 0f);
        assertEquals(Float.floatToRawIntBits(-0f), Float.floatToRawIntBits(decoded.g));
        assertEquals(2f, decoded.b, 0f);
        assertEquals(1f, decoded.a, 0f);
    }
}
