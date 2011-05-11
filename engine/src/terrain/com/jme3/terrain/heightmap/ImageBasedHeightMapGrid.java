/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.heightmap;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import jme3tools.converters.ImageToAwt;

/**
 *
 * @author Anthyon
 */
public class ImageBasedHeightMapGrid implements HeightMapGrid {

    private final String textureBase;
    private final String textureExt;
    private final AssetManager assetManager;
    private int size;

    public ImageBasedHeightMapGrid(String textureBase, String textureExt, AssetManager assetManager) {
        this.textureBase = textureBase;
        this.textureExt = textureExt;
        this.assetManager = assetManager;
    }

    public HeightMap getHeightMapAt(Vector3f location) {
        // HEIGHTMAP image (for the terrain heightmap)
        int x = (int) (FastMath.floor(location.x / this.size) * this.size);
        int z = (int) (FastMath.floor(location.z / this.size) * this.size);
        AbstractHeightMap heightmap = null;
        try {
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(textureBase + "_" + x + "_" + z + "." + textureExt);
            BufferedImage im = null;
            if (stream != null) {
                im = ImageIO.read(stream);
            } else {
                im = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY);
            }
            // CREATE HEIGHTMAP
            heightmap = new Grayscale16BitHeightMap(im);
            heightmap.setHeightScale(256);
            heightmap.load();
        } catch (IOException e) {
        } catch (AssetNotFoundException e) {
        }
        return heightmap;
    }

    public void setSize(int size) {
        this.size = size - 1;
    }
}
