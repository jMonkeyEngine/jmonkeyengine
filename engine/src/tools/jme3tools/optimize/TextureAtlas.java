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
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Lukasz Bruun - lukasz.dk, normenhansen
 */
public class TextureAtlas {

    private byte[] image;
    private int width, height;
    private Format format;
    private Node root;
    private Map<String, TextureLocation> rectangleMap;

    public TextureAtlas(int width, int height) {
        this(Format.RGBA8, width, height);
    }

    public TextureAtlas(Format format, int width, int height) {
        this.format = format;
        this.width = width;
        this.height = height;
        image = new byte[width * height * 4];//new Image(format, width, height, BufferUtils.createByteBuffer(width * height * 4));
        root = new Node(0, 0, width, height);
        rectangleMap = new TreeMap<String, TextureLocation>();
    }

    public boolean addTexture(Texture texture) {
        AssetKey key = texture.getKey();
        if (texture.getImage() != null && key != null && key.getName() != null) {
            return addImage(texture.getImage(), key.getName());
        } else {
            return false;
        }
    }

    private boolean addImage(Image image, String name) {
        Node node = root.insert(image);
        if (node == null) {
            return false;
        }
        rectangleMap.put(name, node.rect);
        drawImage(image, node.rect.getX(), node.rect.getY());
        return true;
    }

    private void drawImage(Image source, int x, int y) {
        //TODO: all buffers
        ByteBuffer sourceData = source.getData(0);
        int height = source.getHeight();
        int width = source.getWidth();
        int index = 0;
        for (int yPos = y; yPos < height + y; yPos++) {
            for (int xPos = x; xPos < width + x; xPos++) {
                int i = (xPos + yPos * width) * 4;
                image[i] = sourceData.get(index); //r
                image[i + 1] = sourceData.get(index + 1); //g
                image[i + 2] = sourceData.get(index + 2); //b
                image[i + 3] = sourceData.get(index + 3); //a
                index += 4;
            }
        }
    }

    public TextureLocation getTextureLocation(String assetName) {
        return rectangleMap.get(assetName);
    }

    public Texture getAtlasTexture() {
        return new Texture2D(new Image(format, width, height, BufferUtils.createByteBuffer(image)));
    }

    private static class Node {

        public TextureLocation rect;
        public Node child[];
        public Image image;

        public Node(int x, int y, int width, int height) {
            rect = new TextureLocation(x, y, width, height);
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

                if (image.getWidth() > rect.getWidth() || image.getHeight() > rect.getHeight()) {
                    return null; // does not fit
                }

                if (image.getWidth() == rect.getWidth() && image.getHeight() == rect.getHeight()) {
                    this.image = image; // perfect fit
                    return this;
                }

                int dw = rect.getWidth() - image.getWidth();
                int dh = rect.getHeight() - image.getHeight();

                if (dw > dh) {
                    child[0] = new Node(rect.getX(), rect.getY(), image.getWidth(), rect.getHeight());
                    child[1] = new Node(rect.getX() + image.getWidth(), rect.getY(), rect.getWidth() - image.getWidth(), rect.getHeight());
                } else {
                    child[0] = new Node(rect.getX(), rect.getY(), rect.getWidth(), image.getHeight());
                    child[1] = new Node(rect.getX(), rect.getY() + image.getHeight(), rect.getWidth(), rect.getHeight() - image.getHeight());
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
