/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap.grid;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainGridTileLoader;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Anthyon, normenhansen
 */
public class ImageTileLoader implements TerrainGridTileLoader{
    private static final Logger logger = Logger.getLogger(ImageTileLoader.class.getName());
    private final AssetManager assetManager;
    private final Namer namer;
    private int patchSize;
    private int quadSize;
    private int imageType = BufferedImage.TYPE_USHORT_GRAY; // 16 bit grayscale
    private ImageHeightmap customImageHeightmap;

    public ImageTileLoader(final String textureBase, final String textureExt, AssetManager assetManager) {
        this(assetManager, new Namer() {

            public String getName(int x, int y) {
                return textureBase + "_" + x + "_" + y + "." + textureExt;
            }
        });
    }

    public ImageTileLoader(AssetManager assetManager, Namer namer) {
        this.assetManager = assetManager;
        this.namer = namer;
    }
    
    /**
     * Lets you specify the type of images that are being loaded. All images
     * must be the same type.
     * @param imageType eg. BufferedImage.TYPE_USHORT_GRAY
     */
    public void setImageType(int imageType) {
        this.imageType = imageType;
    }

    /**
     * The ImageHeightmap that will parse the image type that you 
     * specify with setImageType().
     * @param customImageHeightmap must extend AbstractHeightmap
     */
    public void setCustomImageHeightmap(ImageHeightmap customImageHeightmap) {
        if (!(customImageHeightmap instanceof AbstractHeightMap)) {
            throw new IllegalArgumentException("customImageHeightmap must be an AbstractHeightMap!");
        }
        this.customImageHeightmap = customImageHeightmap;
    }

    private HeightMap getHeightMapAt(Vector3f location) {
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
                im = new BufferedImage(patchSize, patchSize, imageType);
                logger.log(Level.WARNING, "File: {0} not found, loading zero heightmap instead", name);
            }
            // CREATE HEIGHTMAP
            if (imageType == BufferedImage.TYPE_USHORT_GRAY) {
                heightmap = new Grayscale16BitHeightMap(im);
            } else if (imageType == BufferedImage.TYPE_3BYTE_BGR) {
                heightmap = new ImageBasedHeightMap(im);
            } else if (customImageHeightmap != null && customImageHeightmap instanceof AbstractHeightMap) {
                // If it gets here, it means you have specified a different image type, and you must
                // then also supply a custom image heightmap class that can parse that image into
                // a heightmap.
                customImageHeightmap.setImage(im);
                heightmap = (AbstractHeightMap) customImageHeightmap;
            } else {
                // error, no supported image format and no custom image heightmap specified
                if (customImageHeightmap == null)
                    logger.log(Level.SEVERE, "Custom image type specified [{0}] but no customImageHeightmap declared! Use setCustomImageHeightmap()",imageType);
                if (!(customImageHeightmap instanceof AbstractHeightMap))
                    logger.severe("customImageHeightmap must be an AbstractHeightMap!");
                return null;
            }
            heightmap.setHeightScale(256);
            heightmap.load();
        } catch (IOException e) {
        } catch (AssetNotFoundException e) {
        }
        return heightmap;
    }

    public void setSize(int size) {
        this.patchSize = size - 1;
    }

    public TerrainQuad getTerrainQuadAt(Vector3f location) {
        HeightMap heightMapAt = getHeightMapAt(location);
        TerrainQuad q = new TerrainQuad("Quad" + location, patchSize, quadSize, heightMapAt == null ? null : heightMapAt.getHeightMap());
        return q;
    }

    public void setPatchSize(int patchSize) {
        this.patchSize = patchSize;
    }

    public void setQuadSize(int quadSize) {
        this.quadSize = quadSize;
    }

    public void write(JmeExporter ex) throws IOException {
        //TODO: serialization
    }

    public void read(JmeImporter im) throws IOException {
        //TODO: serialization
    }
}
