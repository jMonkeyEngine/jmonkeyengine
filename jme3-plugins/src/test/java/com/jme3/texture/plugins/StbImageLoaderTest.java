/*
 * Copyright (c) 2024 jMonkeyEngine
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
package com.jme3.texture.plugins;

import com.jme3.texture.Image;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests for {@link StbImageLoader}, covering formats whose channel count is
 * not reported by the decoder's {@code info()} method (e.g. TGA).
 */
public class StbImageLoaderTest {

    /**
     * Creates a minimal, valid TGA image as a byte array.
     *
     * @param width   image width in pixels
     * @param height  image height in pixels
     * @param bpp     bits per pixel: 24 (RGB) or 32 (RGBA)
     * @return raw TGA bytes
     */
    private static byte[] makeTga(int width, int height, int bpp) {
        int bytesPerPixel = bpp / 8;
        int pixelDataSize = width * height * bytesPerPixel;
        // TGA header is 18 bytes, followed by pixel data
        byte[] tga = new byte[18 + pixelDataSize];

        tga[0] = 0;               // ID length
        tga[1] = 0;               // Color map type: none
        tga[2] = 2;               // Image type: uncompressed true-color
        // Color map spec (5 bytes): all zero
        // Image spec:
        tga[8]  = 0; tga[9]  = 0; // X origin
        tga[10] = 0; tga[11] = 0; // Y origin
        tga[12] = (byte) (width  & 0xFF); tga[13] = (byte) ((width  >> 8) & 0xFF);
        tga[14] = (byte) (height & 0xFF); tga[15] = (byte) ((height >> 8) & 0xFF);
        tga[16] = (byte) bpp;     // Bits per pixel
        tga[17] = 0x20;           // Image descriptor: top-left origin

        // Fill pixel data with a simple pattern (stored as BGR or BGRA in TGA)
        for (int i = 0; i < width * height; i++) {
            int base = 18 + i * bytesPerPixel;
            tga[base]     = (byte) 0xFF; // B
            tga[base + 1] = (byte) 0x80; // G
            tga[base + 2] = (byte) 0x40; // R
            if (bpp == 32) {
                tga[base + 3] = (byte) 0xFF; // A
            }
        }
        return tga;
    }

    /**
     * Verifies that a 24-bit (RGB) TGA image loads without throwing an exception
     * and is decoded as {@link Image.Format#RGB8}.
     *
     * <p>This is a regression test for the bug where TGA images caused an
     * {@code IOException("Unsupported number of channels: 0")} because the
     * stb-image {@code TgaDecoder.info()} did not set the channel count.
     */
    @Test
    public void testLoad24BitTga() throws IOException {
        byte[] tgaData = makeTga(2, 2, 24);
        StbImageLoader loader = new StbImageLoader();
        Image image = loader.load(tgaData, false);

        Assertions.assertNotNull(image, "Image must not be null");
        Assertions.assertEquals(Image.Format.RGB8, image.getFormat(),
                "24-bit TGA should be decoded as RGB8");
        Assertions.assertEquals(2, image.getWidth());
        Assertions.assertEquals(2, image.getHeight());
    }

    /**
     * Verifies that a grayscale (8-bit, 1-channel) TGA image loads correctly
     * as {@link Image.Format#Luminance8}.
     */
    @Test
    public void testLoadGrayscaleTga() throws IOException {
        // Build a 1-channel (grayscale) TGA: image type 3 = uncompressed grayscale
        int width = 2, height = 2;
        byte[] tga = new byte[18 + width * height];
        tga[0] = 0;  // ID length
        tga[1] = 0;  // Color map type: none
        tga[2] = 3;  // Image type: uncompressed grayscale
        tga[12] = (byte) width;
        tga[14] = (byte) height;
        tga[16] = 8; // 8 bits per pixel
        tga[17] = 0x20; // top-left origin
        // Fill pixel data
        for (int i = 0; i < width * height; i++) {
            tga[18 + i] = (byte) (i * 64);
        }

        StbImageLoader loader = new StbImageLoader();
        Image image = loader.load(tga, false);

        Assertions.assertNotNull(image, "Image must not be null");
        Assertions.assertEquals(Image.Format.Luminance8, image.getFormat(),
                "Grayscale TGA should be decoded as Luminance8");
        Assertions.assertEquals(width, image.getWidth());
        Assertions.assertEquals(height, image.getHeight());
    }
}
