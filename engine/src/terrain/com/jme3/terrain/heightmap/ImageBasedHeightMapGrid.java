/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.heightmap;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.TextureKey;
import com.jme3.math.Vector3f;
import com.jme3.texture.Texture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads Terrain grid tiles with image heightmaps.
 * By default it expects a 16-bit grayscale image as the heightmap, but
 * you can also call setImageType(BufferedImage.TYPE_) to set it to be a different
 * image type. If you do this, you must also set a custom ImageHeightmap that will
 * understand and be able to parse the image. By default if you pass in an image of type
 * BufferedImage.TYPE_3BYTE_BGR, it will use the ImageBasedHeightMap for you.
 * 
 * @author Anthyon, Brent Owens
 */
@Deprecated
/**
 * @Deprecated in favor of ImageTileLoader
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
        //BufferedImage im = null;
        
        try {
            String name = namer.getName(x, z);
            logger.log(Level.INFO, "Loading heightmap from file: {0}", name);
            final Texture texture = assetManager.loadTexture(new TextureKey(name));
            
            // CREATE HEIGHTMAP
            heightmap = new ImageBasedHeightMap(texture.getImage());
            
            heightmap.setHeightScale(1);
            heightmap.load();
        
        } catch (AssetNotFoundException e) {
            logger.log(Level.SEVERE, "Asset Not found! ", e);
        }
        return heightmap;
    }

    public void setSize(int size) {
        this.size = size - 1;
    }
}
