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
import java.util.logging.Level;
import java.util.logging.Logger;
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
        int x = (int) location.x;
        int z = (int) location.z;
        AbstractHeightMap heightmap = null;
        try {
            Logger.getLogger(ImageBasedHeightMapGrid.class.getCanonicalName()).log(Level.INFO, "Loading heightmap from file: " + textureBase + "_" + x + "_" + z + "." + textureExt);
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(textureBase + "_" + x + "_" + z + "." + textureExt);
            BufferedImage im = null;
            if (stream != null) {
                im = ImageIO.read(stream);
            } else {
                im = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY);
                Logger.getLogger(ImageBasedHeightMapGrid.class.getCanonicalName()).log(Level.INFO, "File: " + textureBase + "_" + x + "_" + z + "." + textureExt + " not found, loading zero heightmap instead");
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
