/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.heightmap;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.math.Vector3f;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Anthyon
 */
public class ImageBasedHeightMapGrid implements HeightMapGrid {

    private static final Logger logger = Logger.getLogger(ImageBasedHeightMapGrid.class.getName());
    private final AssetManager assetManager;
    private final Namer namer;
    private int size;

    public ImageBasedHeightMapGrid(final String textureBase, final String textureExt, AssetManager assetManager) {
        this(assetManager, new Namer() {

            public String getName(int x, int y) {
                return textureBase + "_" + x + "_" + y + "." + textureExt;
            }
        });
    }

    public ImageBasedHeightMapGrid(AssetManager assetManager, Namer namer) {
        this.assetManager = assetManager;
        this.namer = namer;
    }

    public HeightMap getHeightMapAt(Vector3f location) {
        // HEIGHTMAP image (for the terrain heightmap)
        int x = (int) location.x;
        int z = (int) location.z;
        
        AbstractHeightMap heightmap = null;
        BufferedImage im = null;
        
        try {
            String name = namer.getName(x, z);
            logger.log(Level.INFO, "Loading heightmap from file: {0}", name);
            final AssetInfo assetInfo = assetManager.locateAsset(new AssetKey(name));
            if (assetInfo != null){
                InputStream in = assetInfo.openStream();
                im = ImageIO.read(in);
            } else {
                im = new BufferedImage(size, size, BufferedImage.TYPE_USHORT_GRAY);
                logger.log(Level.WARNING, "File: {0} not found, loading zero heightmap instead", name);
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
