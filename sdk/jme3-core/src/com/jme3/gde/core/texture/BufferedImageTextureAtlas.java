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
package com.jme3.gde.core.texture;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;

/**
 *
 * @author Lukasz Bruun - lukasz.dk, normenhansen
 */
public class BufferedImageTextureAtlas {

    private BufferedImage image;
    private Graphics2D graphics;
    private Node root;
    private Map<String, Rectangle> rectangleMap;

    public BufferedImageTextureAtlas(int width, int height) {
        image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        graphics = image.createGraphics();

        root = new Node(0, 0, width, height);
        rectangleMap = new TreeMap<String, Rectangle>();
    }

    public boolean addImage(FileObject image, String name) {
        BufferedImage bufImage;
        try {
            bufImage = ImageIO.read(image.getInputStream());
        } catch (IOException ex) {
            return false;
        }
        return addImage(bufImage, name);
    }

    public boolean addImage(BufferedImage image, String name) {
        Node node = root.insert(image);

        if (node == null) {
            return false;
        }

        rectangleMap.put(name, node.rect);
        graphics.drawImage(image, null, node.rect.x, node.rect.y);
        return true;
    }

    public Rectangle getTextureRectangle(String name) {
        return rectangleMap.get(name);
    }

    public void write(FileObject file) {
        BufferedWriter atlas = null;
        try {
            ImageIO.write(image, "png", file.getOutputStream());
            FileObject atlasFile = file.getParent().getFileObject(file.getName(), "atl");
            if (atlasFile == null) {
                atlasFile = file.getParent().createData(file.getName(), "atl");
            }
            atlas = new BufferedWriter(new OutputStreamWriter(atlasFile.getOutputStream()));

            for (Map.Entry<String, Rectangle> e : rectangleMap.entrySet()) {
                Rectangle r = e.getValue();
                atlas.write(e.getKey() + " " + r.x + " " + r.y + " " + r.width + " " + r.height);
                atlas.newLine();
            }

        } catch (IOException e) {
            Exceptions.printStackTrace(e);
        } finally {
            if (atlas != null) {
                try {
                    atlas.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    private class Node {

        public Rectangle rect;
        public Node child[];
        public BufferedImage image;

        public Node(int x, int y, int width, int height) {
            rect = new Rectangle(x, y, width, height);
            child = new Node[2];
            child[0] = null;
            child[1] = null;
            image = null;
        }

        public boolean isLeaf() {
            return child[0] == null && child[1] == null;
        }

        // Algorithm from http://www.blackpawn.com/texts/lightmaps/
        public Node insert(BufferedImage image) {
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

                if (image.getWidth() > rect.width || image.getHeight() > rect.height) {
                    return null; // does not fit
                }

                if (image.getWidth() == rect.width && image.getHeight() == rect.height) {
                    this.image = image; // perfect fit
                    return this;
                }

                int dw = rect.width - image.getWidth();
                int dh = rect.height - image.getHeight();

                if (dw > dh) {
                    child[0] = new Node(rect.x, rect.y, image.getWidth(), rect.height);
                    child[1] = new Node(rect.x + image.getWidth(), rect.y, rect.width - image.getWidth(), rect.height);
                } else {
                    child[0] = new Node(rect.x, rect.y, rect.width, image.getHeight());
                    child[1] = new Node(rect.x, rect.y + image.getHeight(), rect.width, rect.height - image.getHeight());
                }

                return child[0].insert(image);
            }
        }
    }
}
