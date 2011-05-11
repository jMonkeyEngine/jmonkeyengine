/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
