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
package com.jme3.asset;

import com.jme3.texture.Texture.Type;
import com.jme3.asset.cache.AssetCache;
import com.jme3.asset.cache.WeakRefCloneAssetCache;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureProcessor;
import java.io.IOException;

/**
 * Used to load textures from image files such as JPG or PNG. 
 * Note that texture loaders actually load the asset as an {@link Image}
 * object, which is then converted to a {@link Texture} in the 
 * {@link TextureProcessor#postProcess(com.jme3.asset.AssetKey, java.lang.Object) }
 * method. Since textures are cloneable smart assets, the texture stored
 * in the cache will be collected when all clones of the texture become
 * unreachable.
 * 
 * @author Kirill Vainer
 */
public class TextureKey extends AssetKey<Texture> {

    private boolean generateMips;
    private boolean flipY;
    private int anisotropy;
    private Texture.Type textureTypeHint = Texture.Type.TwoDimensional;

    public TextureKey(String name, boolean flipY) {
        super(name);
        this.flipY = flipY;
    }

    public TextureKey(String name) {
        super(name);
        this.flipY = true;
    }

    public TextureKey() {
    }

    @Override
    public String toString() {
        String type;
        switch (textureTypeHint) {
            case CubeMap:
                type = " (Cube)";
                break;
            case ThreeDimensional:
                type = " (3D)";
                break;
            case TwoDimensionalArray:
                type = " (Array)";
                break;
            case TwoDimensional:
                type = "";
                break;
            default:
                type = " (" + textureTypeHint.toString() + ")";
                break;
        }
        return name + (flipY ? " (Flipped)" : "") + type + (generateMips ? " (Mipmapped)" : "");
    }
    
    @Override
    public Class<? extends AssetCache> getCacheType(){
        return WeakRefCloneAssetCache.class;
    }

    @Override
    public Class<? extends AssetProcessor> getProcessorType(){
        return TextureProcessor.class;
    }
    
    public boolean isFlipY() {
        return flipY;
    }

    public void setFlipY(boolean flipY) {
        this.flipY = flipY;
    }
    
    public int getAnisotropy() {
        return anisotropy;
    }

    public void setAnisotropy(int anisotropy) {
        this.anisotropy = anisotropy;
    }

    public boolean isGenerateMips() {
        return generateMips;
    }

    public void setGenerateMips(boolean generateMips) {
        this.generateMips = generateMips;
    }

    /**
     * The type of texture expected to be returned.
     * 
     * @return type of texture expected to be returned.
     */
    public Type getTextureTypeHint() {
        return textureTypeHint;
    }

    /**
     * Hints the loader as to which type of texture is expected.
     * 
     * @param textureTypeHint The type of texture expected to be loaded.
     */
    public void setTextureTypeHint(Type textureTypeHint) {
        this.textureTypeHint = textureTypeHint;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TextureKey other = (TextureKey) obj;
        if (!super.equals(obj)) {
            return false;
        }
        if (this.generateMips != other.generateMips) {
            return false;
        }
        if (this.flipY != other.flipY) {
            return false;
        }
        if (this.anisotropy != other.anisotropy) {
            return false;
        }
        if (this.textureTypeHint != other.textureTypeHint) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 17 * hash + (super.hashCode());
        hash = 17 * hash + (this.generateMips ? 1 : 0);
        hash = 17 * hash + (this.flipY ? 1 : 0);
        hash = 17 * hash + this.anisotropy;
        hash = 17 * hash + (this.textureTypeHint != null ? this.textureTypeHint.hashCode() : 0);
        return hash;
    }
    
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(flipY, "flip_y", false);
        oc.write(generateMips, "generate_mips", false);
        oc.write(anisotropy, "anisotropy", 0);
        oc.write(textureTypeHint, "tex_type", Type.TwoDimensional);
        
        // Backwards compat
        oc.write(textureTypeHint == Type.CubeMap, "as_cubemap", false);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        flipY = ic.readBoolean("flip_y", false);
        generateMips = ic.readBoolean("generate_mips", false);
        anisotropy = ic.readInt("anisotropy", 0);
        boolean asCube = ic.readBoolean("as_cubemap", false);
        
        if (asCube) {
            // Backwards compat
            textureTypeHint = Type.CubeMap;
        } else {
            textureTypeHint = ic.readEnum("tex_type", Texture.Type.class, Type.TwoDimensional);
        }
    }
}
