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
package org.jmonkeyengine.screenshottests.image;

import com.jme3.texture.Image;

import java.nio.ByteBuffer;

public class ImagePixelWrapper {

    private final Image underlyingImage;
    ByteBuffer bytes;
    int sizeX;
    int sizeY;

    public ImagePixelWrapper(Image underlyingImage) {
        this.underlyingImage = underlyingImage;
        if(underlyingImage.getFormat() != Image.Format.RGBA8){
            throw new RuntimeException("Expected RGBA8 but was " + underlyingImage.getFormat());
        }
        this.bytes = underlyingImage.getData(0);
        this.sizeX = underlyingImage.getWidth();
        this.sizeY = underlyingImage.getHeight();
    }

    public int getARGB(int x, int y){
        int idx = getStartIndex(x, y);
        int r = bytes.get(idx)     & 0xFF;
        int g = bytes.get(idx + 1) & 0xFF;
        int b = bytes.get(idx + 2) & 0xFF;
        int a = bytes.get(idx + 3) & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public int getR(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex) & 0xFF;
    }

    public int getG(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex+1) & 0xFF;
    }

    public int getB(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex+2) & 0xFF;
    }

    public int getA(int x, int y){
        int startIndex = getStartIndex(x,y);
        return bytes.get(startIndex+3) & 0xFF;
    }

    public void setARGB(int x, int y, int packedColour) {
        int idx = getStartIndex(x, y);
        bytes.put(idx,     (byte) ((packedColour >> 16) & 0xFF)); // R (bits 16-23)
        bytes.put(idx + 1, (byte) ((packedColour >> 8)  & 0xFF)); // G (bits 8-15)
        bytes.put(idx + 2, (byte) (packedColour & 0xFF));        // B (bits 0-7)
        bytes.put(idx + 3, (byte) ((packedColour >> 24) & 0xFF)); // A (bits 24-31)
    }

    private int getStartIndex(int x, int y) {
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY) {
            throw new IndexOutOfBoundsException("x: " + x + ", y: " + y);
        }
        return (y * sizeX + x) * 4; // 4 bytes per pixel (RGBA8)
    }

    public Image getUnderlyingImage() {
        return underlyingImage;
    }
}
