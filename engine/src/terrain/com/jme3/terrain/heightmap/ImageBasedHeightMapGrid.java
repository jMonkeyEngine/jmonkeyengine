/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
            logger.log(Level.FINE, "Loading heightmap from file: {0}", name);
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
