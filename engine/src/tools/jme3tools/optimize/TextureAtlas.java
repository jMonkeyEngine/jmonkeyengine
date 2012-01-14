/*
 *  Copyright (c) 2009-2010 jMonkeyEngine
 *  All rights reserved.
 * 
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are
 *  met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 
 *  * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 *  TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 *  PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jme3tools.optimize;

import com.jme3.asset.AssetKey;
import com.jme3.math.Vector2f;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Lukasz Bruun - lukasz.dk, normenhansen
 */
public class TextureAtlas {

    private static final Logger logger = Logger.getLogger(TextureAtlas.class.getName());
    private Map<String, byte[]> images;
    private int width, height;
    private Format format;
    private Node root;
    private Map<String, TextureLocation> locationMap;
    private String rootMapName;

    public TextureAtlas(int width, int height) {
        this(Format.ABGR8, width, height);
    }

    public TextureAtlas(Format format, int width, int height) {
        this.format = format;
        this.width = width;
        this.height = height;
        root = new Node(0, 0, width, height);
        locationMap = new TreeMap<String, TextureLocation>();
    }

    /**
     * Add a texture for a specific map name
     * @param texture A texture to add to the atlas
     * @param mapName A freely chosen map name that can be later retrieved as a Texture. The first map name supplied will be the master texture.
     * @return 
     */
    public boolean addTexture(Texture texture, String mapName) {
        return addTexture(texture, mapName, null);
    }

    /**
     * Add a texture for a specific map name at the location of another existing texture.
     * @param texture A texture to add to the atlas.
     * @param mapName A freely chosen map name that can be later retrieved as a Texture.
     * @param sourceTextureName Name of the original texture location.
     * @return 
     */
    public boolean addTexture(Texture texture, String mapName, String sourceTextureName) {
        if (texture == null) {
            return false;
        }
        AssetKey key = texture.getKey();
        if (texture.getImage() != null && key != null && key.getName() != null) {
            return addImage(texture.getImage(), key.getName(), mapName, sourceTextureName);
        } else {
            return false;
        }
    }

    private boolean addImage(Image image, String name, String mapName, String sourceTextureName) {
        if (rootMapName == null) {
            rootMapName = mapName;
        }
        if (sourceTextureName == null && !rootMapName.equals(mapName)) {
            throw new IllegalStateException("Cannot add texture to new map without source texture");
        }
        TextureLocation location;
        if (sourceTextureName == null) {
            Node node = root.insert(image);
            if (node == null) {
                return false;
            }
            location = node.location;
        } else {
            location = locationMap.get(sourceTextureName);
        }
        if (location == null) {
            logger.log(Level.WARNING, "Source texture not found");
            return false;
        }
        locationMap.put(name, location);
        drawImage(image, location.getX(), location.getY(), mapName);
        return true;
    }

    private void drawImage(Image source, int x, int y, String mapName) {
        //TODO: all buffers?
        if (images == null) {
            images = new HashMap<String, byte[]>();
        }
        byte[] image = images.get(mapName);
        if (image == null) {
            image = new byte[width * height * 4];
            images.put(mapName, image);
        }
        ByteBuffer sourceData = source.getData(0);
        int height = source.getHeight();
        int width = source.getWidth();
        for (int yPos = 0; yPos < height; yPos++) {
            for (int xPos = 0; xPos < width; xPos++) {
                int i = ((xPos + x) + (yPos + y) * this.width) * 4;
                if (source.getFormat() == Format.ABGR8) {
                    int j = (xPos + yPos * width) * 4;
                    image[i] = sourceData.get(j); //a
                    image[i + 1] = sourceData.get(j + 1); //b
                    image[i + 2] = sourceData.get(j + 2); //g
                    image[i + 3] = sourceData.get(j + 3); //r
                } else if (source.getFormat() == Format.BGR8) {
                    int j = (xPos + yPos * width) * 3;
                    image[i] = 0; //b
                    image[i + 1] = sourceData.get(j); //b
                    image[i + 2] = sourceData.get(j + 1); //g
                    image[i + 3] = sourceData.get(j + 2); //r
                }else{
                    logger.log(Level.WARNING, "Could not draw texture {0} ", mapName);
                }
            }
        }
    }

    public TextureLocation getTextureLocation(String assetName) {
        return locationMap.get(assetName);
    }

    public Texture getAtlasTexture(String mapName) {
        if (images == null) {
            return null;
        }
        byte[] image = images.get(mapName);
        if (image != null) {
            return new Texture2D(new Image(format, width, height, BufferUtils.createByteBuffer(image)));
        }
        return null;
    }

    private static class Node {

        public TextureLocation location;
        public Node child[];
        public Image image;

        public Node(int x, int y, int width, int height) {
            location = new TextureLocation(x, y, width, height);
            child = new Node[2];
            child[0] = null;
            child[1] = null;
            image = null;
        }

        public boolean isLeaf() {
            return child[0] == null && child[1] == null;
        }

        // Algorithm from http://www.blackpawn.com/texts/lightmaps/
        public Node insert(Image image) {
            if (!isLeaf()) {
                Node newNode = child[0].insert(image);

                if (newNode != null) {
                    return newNode;
                }

                return child[1].insert(image);
            } else {
                if (this.image != null) {
                    return null; // occupied
                }

                if (image.getWidth() > location.getWidth() || image.getHeight() > location.getHeight()) {
                    return null; // does not fit
                }

                if (image.getWidth() == location.getWidth() && image.getHeight() == location.getHeight()) {
                    this.image = image; // perfect fit
                    return this;
                }

                int dw = location.getWidth() - image.getWidth();
                int dh = location.getHeight() - image.getHeight();

                if (dw > dh) {
                    child[0] = new Node(location.getX(), location.getY(), image.getWidth(), location.getHeight());
                    child[1] = new Node(location.getX() + image.getWidth(), location.getY(), location.getWidth() - image.getWidth(), location.getHeight());
                } else {
                    child[0] = new Node(location.getX(), location.getY(), location.getWidth(), image.getHeight());
                    child[1] = new Node(location.getX(), location.getY() + image.getHeight(), location.getWidth(), location.getHeight() - image.getHeight());
                }

                return child[0].insert(image);
            }
        }
    }

    public static class TextureLocation {

        private int x;
        private int y;
        private int width;
        private int height;

        public TextureLocation(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public Vector2f getLocation(Vector2f previousLocation) {
            float x = (float) getX() / (float) width;
            float y = (float) getY() / (float) height;
            float w = (float) getWidth() / (float) width;
            float h = (float) getHeight() / (float) height;
            Vector2f location = new Vector2f(x, y);
            Vector2f scale = new Vector2f(w, h);
            return location.add(previousLocation.multLocal(scale));
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }
}
