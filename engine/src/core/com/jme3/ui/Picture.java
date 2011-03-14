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

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
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

    private float width;
    private float height;

    public Picture(String name, boolean flipY){
        super(name, new Quad(1, 1, flipY));
        setQueueBucket(Bucket.Gui);
        setCullHint(CullHint.Never);
    }

    public Picture(String name){
        this(name, false);
    }

    public Picture(){
    }

    public void setWidth(float width){
        this.width = width;
        setLocalScale(new Vector3f(width, height, 1f));
    }

    public void setHeight(float height){
        this.height = height;
        setLocalScale(new Vector3f(width, height, 1f));
    }

    public void setPosition(float x, float y){
        float z = getLocalTranslation().getZ();
        setLocalTranslation(x, y, z);
    }

    public void setImage(AssetManager manager, String imgName, boolean useAlpha){
        TextureKey key = new TextureKey(imgName, true);
        Texture2D tex = (Texture2D) manager.loadTexture(key);
        setTexture(manager, tex, useAlpha);
    }

    public void setTexture(AssetManager manager, Texture2D tex, boolean useAlpha){
        if (getMaterial() == null){
            Material mat = new Material(manager, "Common/MatDefs/Gui/Gui.j3md");
            mat.setColor("Color", ColorRGBA.White);
            setMaterial(mat);
        }
        material.getAdditionalRenderState().setBlendMode(useAlpha ? BlendMode.Alpha : BlendMode.Off);
        material.setTexture("Texture", tex);
    }

}
