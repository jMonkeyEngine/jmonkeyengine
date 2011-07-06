/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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
package com.jme3.terrain.heightmap;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Anthyon
 */
public class Grayscale16BitHeightMap extends AbstractHeightMap {

    private BufferedImage image;

    public Grayscale16BitHeightMap() {
    }

    public Grayscale16BitHeightMap(BufferedImage image) {
        this.image = image;
    }

    public Grayscale16BitHeightMap(String filename) {
        this(new File(filename));
    }

    public Grayscale16BitHeightMap(File file) {
        try {
            this.image = ImageIO.read(file);
        } catch (IOException ex) {
            Logger.getLogger(Grayscale16BitHeightMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public boolean load() {
        return load(false, false);
    }

    public boolean load(boolean flipX, boolean flipY) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        if (imageWidth != imageHeight) {
            throw new RuntimeException("imageWidth: " + imageWidth
                    + " != imageHeight: " + imageHeight);
        }

        Object out = new short[imageWidth * imageHeight];
        out = image.getData().getDataElements(0, 0, imageWidth, imageHeight, out);
        short[] values = (short[]) out;
        heightData = new float[imageWidth * imageHeight];
        int i = 0;
        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++, i++) {
                heightData[i] = heightScale * (values[i] & 0x0000FFFF) / 65536f;
            }
        }

        return true;
    }
}
