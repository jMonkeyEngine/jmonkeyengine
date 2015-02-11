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
package com.jme3.util;

import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioBuffer;
import com.jme3.audio.AudioData;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.texture.Image;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import java.nio.ByteBuffer;

public class PlaceholderAssets {
    
    /**
     * Checkerboard of white and red squares
     */
    private static final byte[] imageData = {
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
        (byte)0xFF, (byte)0x00, (byte)0x00,
        (byte)0xFF, (byte)0xFF, (byte)0xFF,
    };
    
    @Deprecated
    public static Image getPlaceholderImage(){
        ByteBuffer tempData = BufferUtils.createByteBuffer(3 * 4 * 4);
        tempData.put(imageData).flip();
        return new Image(Format.RGB8, 4, 4, tempData, null, ColorSpace.Linear);
    }
    
    public static Image getPlaceholderImage(AssetManager assetManager){
        return assetManager.loadTexture("Common/Textures/MissingTexture.png").getImage();
    }
    
    public static Material getPlaceholderMaterial(AssetManager assetManager){
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("Common/Textures/MissingMaterial.png");
        tex.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", tex);
        return mat;
    }
    
    public static Spatial getPlaceholderModel(AssetManager assetManager){
        // What should be the size? Nobody knows
        // the user's expected scale...
        Box box = new Box(1, 1, 1);
        Geometry geom = new Geometry("placeholder", box);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture tex = assetManager.loadTexture("Common/Textures/MissingModel.png");
        tex.setWrap(Texture.WrapMode.Repeat);
        mat.setTexture("ColorMap", tex);
        geom.setMaterial(mat);
        return geom;
    }
    
    public static AudioData getPlaceholderAudio(){
        AudioBuffer audioBuf = new AudioBuffer();
        audioBuf.setupFormat(1, 8, 44100);
        ByteBuffer bb = BufferUtils.createByteBuffer(1);
        bb.put((byte)0).flip();
        audioBuf.updateData(bb);
        return audioBuf;
    }
    
}
