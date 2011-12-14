/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jme3.terrain.geomipmap.grid;

import com.jme3.asset.AssetManager;
import com.jme3.asset.AssetNotFoundException;
import com.jme3.asset.TextureKey;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainGridTileLoader;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.*;
import com.jme3.texture.Texture;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private float heightScale = 1;
    //private int imageType = BufferedImage.TYPE_USHORT_GRAY; // 16 bit grayscale
    //private ImageHeightmap customImageHeightmap;

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
     * Effects vertical scale of the height of the terrain when loaded.
     */
    public void setHeightScale(float heightScale) {
        this.heightScale = heightScale;
    }
    
    
    /**
     * Lets you specify the type of images that are being loaded. All images
     * must be the same type.
     * @param imageType eg. BufferedImage.TYPE_USHORT_GRAY
     */
    /*public void setImageType(int imageType) {
        this.imageType = imageType;
    }*/

    /**
     * The ImageHeightmap that will parse the image type that you 
     * specify with setImageType().
     * @param customImageHeightmap must extend AbstractHeightmap
     */
    /*public void setCustomImageHeightmap(ImageHeightmap customImageHeightmap) {
        if (!(customImageHeightmap instanceof AbstractHeightMap)) {
            throw new IllegalArgumentException("customImageHeightmap must be an AbstractHeightMap!");
        }
        this.customImageHeightmap = customImageHeightmap;
    }*/

    private HeightMap getHeightMapAt(Vector3f location) {
        // HEIGHTMAP image (for the terrain heightmap)
        int x = (int) location.x;
        int z = (int) location.z;
        
        AbstractHeightMap heightmap = null;
        //BufferedImage im = null;
        
        String name = null;
        try {
            name = namer.getName(x, z);
            logger.log(Level.INFO, "Loading heightmap from file: {0}", name);
            final Texture texture = assetManager.loadTexture(new TextureKey(name));
            heightmap = new ImageBasedHeightMap(texture.getImage());
            /*if (assetInfo != null){
                InputStream in = assetInfo.openStream();
                im = ImageIO.read(in);
            } else {
                im = new BufferedImage(patchSize, patchSize, imageType);
                logger.log(Level.WARNING, "File: {0} not found, loading zero heightmap instead", name);
            }*/
            // CREATE HEIGHTMAP
            /*if (imageType == BufferedImage.TYPE_USHORT_GRAY) {
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
            }*/
            heightmap.setHeightScale(1);
            heightmap.load();
        //} catch (IOException e) {
        //    e.printStackTrace();
        } catch (AssetNotFoundException e) {
            logger.log(Level.WARNING, "Asset {0} not found, loading zero heightmap instead", name);
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
