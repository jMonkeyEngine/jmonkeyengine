/*
 * Copyright (c) 2009-2010 jMonkeyEngine
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

package com.jme3.ui;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;

/**
 * A <code>Picture</code> represents a 2D image drawn on the screen.
 * It can be used to represent sprites or other background elements.
 *
 * @author Kirill Vainer
 */
public class Picture extends Geometry {

    private float width  = 1f;
    private float height = 1f;

    /**
     * Create a named picture. 
     * 
     * By default a picture's width and height are 1
     * and its position is 0, 0.
     * 
     * @param name the name of the picture in the scene graph
     * @param flipY If true, the Y coordinates of the texture will be flipped.
     */
    public Picture(String name, boolean flipY){
        super(name, new Quad(1, 1, flipY));
        setQueueBucket(Bucket.Gui);
        setCullHint(CullHint.Never);
    }

    /**
     * Creates a named picture.
     * By default a picture's width and height are 1
     * and its position is 0, 0.
     * The image texture coordinates will not be flipped.
     * 
     * @param name the name of the picture in the scene graph 
     */
    public Picture(String name){
        this(name, false);
    }

    /*
     * Serialization only. Do not use.
     */
    public Picture(){
    }

    /**
     * Set the width in pixels of the picture, if the width
     * does not match the texture's width, then the texture will
     * be scaled to fit the picture.
     * 
     * @param width the width to set.
     */
    public void setWidth(float width){
        this.width = width;
        setLocalScale(new Vector3f(width, height, 1f));
    }

    /**
     * Set the height in pixels of the picture, if the height
     * does not match the texture's height, then the texture will
     * be scaled to fit the picture.
     * 
     * @param height the height to set.
     */
    public void setHeight(float height){
        this.height = height;
        setLocalScale(new Vector3f(width, height, 1f));
    }

    /**
     * Set the position of the picture in pixels.
     * The origin (0, 0) is at the bottom-left of the screen.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     */
    public void setPosition(float x, float y){
        float z = getLocalTranslation().getZ();
        setLocalTranslation(x, y, z);
    }

    /**
     * Set the image to put on the picture.
     * 
     * @param assetManager The {@link AssetManager} to use to load the image.
     * @param imgName The image name.
     * @param useAlpha If true, the picture will appear transparent and allow
     * objects behind it to appear through. If false, the transparent
     * portions will be the image's color at that pixel.
     */
    public void setImage(AssetManager assetManager, String imgName, boolean useAlpha){
        TextureKey key = new TextureKey(imgName, true);
        Texture2D tex = (Texture2D) assetManager.loadTexture(key);
        setTexture(assetManager, tex, useAlpha);
    }

    /**
     * Set the texture to put on the picture.
     * 
     * @param assetManager The {@link AssetManager} to use to load the material.
     * @param tex The texture
     * @param useAlpha If true, the picture will appear transparent and allow
     * objects behind it to appear through. If false, the transparent
     * portions will be the image's color at that pixel.
     */
    public void setTexture(AssetManager assetManager, Texture2D tex, boolean useAlpha){
        if (getMaterial() == null){
            Material mat = new Material(assetManager, "Common/MatDefs/Gui/Gui.j3md");
            mat.setColor("Color", ColorRGBA.White);
            setMaterial(mat);
        }
        material.getAdditionalRenderState().setBlendMode(useAlpha ? BlendMode.Alpha : BlendMode.Off);
        material.setTexture("Texture", tex);
    }

}
