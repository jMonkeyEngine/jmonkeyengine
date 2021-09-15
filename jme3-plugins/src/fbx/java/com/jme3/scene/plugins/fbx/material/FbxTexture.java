/*
 * Copyright (c) 2009-2021 jMonkeyEngine
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
package com.jme3.scene.plugins.fbx.material;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.scene.plugins.fbx.file.FbxElement;
import com.jme3.scene.plugins.fbx.obj.FbxObject;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.MagFilter;
import com.jme3.texture.Texture.MinFilter;
import com.jme3.texture.Texture.WrapAxis;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.texture.Texture2D;
import com.jme3.util.PlaceholderAssets;

public class FbxTexture extends FbxObject<Texture> {

    private static enum AlphaSource {
        None,
        FromTextureAlpha,
        FromTextureIntensity;
    }
    
    private FbxImage media;
    private String uvSet;
    private int wrapModeU = 0, wrapModeV = 0;

    public FbxTexture(AssetManager assetManager, String sceneFolderName) {
        super(assetManager, sceneFolderName);
    }
    
    public String getUvSet() { 
        return uvSet;
    }
    
    @Override
    protected Texture toJmeObject() {
        Image image = null;
        TextureKey key = null;
        if (media != null) {
            image = (Image) media.getJmeObject();
            key = media.getTextureKey();
        }
        if (image == null) {
            image = PlaceholderAssets.getPlaceholderImage(assetManager);
        }
        Texture2D tex = new Texture2D(image);
        if (key != null) {
            tex.setKey(key);
            tex.setName(key.getName());
            tex.setAnisotropicFilter(key.getAnisotropy());
        }
        tex.setMinFilter(MinFilter.Trilinear);
        tex.setMagFilter(MagFilter.Bilinear);
        if (wrapModeU == 0) {
            tex.setWrap(WrapAxis.S, WrapMode.Repeat);
        }
        if (wrapModeV == 0) {
            tex.setWrap(WrapAxis.T, WrapMode.Repeat);
        }
        return tex;
    }
    
    @Override
    public void fromElement(FbxElement element) {
        super.fromElement(element);
        if (getSubclassName().equals("")) {
            for (FbxElement e : element.children) {
                if (e.id.equals("Type")) {
                    e.properties.get(0);
                } 
                /*else if (e.id.equals("FileName")) {
                    filename = (String) e.properties.get(0);
                }*/
            }
            
            for (FbxElement prop : element.getFbxProperties()) {
                String propName = (String) prop.properties.get(0);
                if (propName.equals("AlphaSource")) {
                    // ???
                } else if (propName.equals("UVSet")) {
                    uvSet = (String) prop.properties.get(4);
                } else if (propName.equals("WrapModeU")) {
                    wrapModeU = (Integer) prop.properties.get(4);
                } else if (propName.equals("WrapModeV")) {
                    wrapModeV = (Integer) prop.properties.get(4);
                }
            }
        }
    }
    
    @Override
    public void connectObject(FbxObject object) {
        if (!(object instanceof FbxImage)) {
            unsupportedConnectObject(object);
//        } else if (media != null) {
//            throw new UnsupportedOperationException("An image is already attached to this texture.");
        }
        
        this.media = (FbxImage) object;
    }

    @Override
    public void connectObjectProperty(FbxObject object, String property) {
        unsupportedConnectObjectProperty(object, property);
    }
    
}
