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

import com.jme3.asset.cache.AssetCache;
import com.jme3.asset.cache.WeakRefCloneAssetCache;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.Type;
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
    private boolean asCube;
    private boolean asTexture3D;
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
        return name + (flipY ? " (Flipped)" : "") + (asCube ? " (Cube)" : "") + (generateMips ? " (Mipmapped)" : "");
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

    public boolean isAsCube() {
        return asCube;
    }

    public void setAsCube(boolean asCube) {
        this.asCube = asCube;
    }

    public boolean isGenerateMips() {
        return generateMips;
    }

    public void setGenerateMips(boolean generateMips) {
        this.generateMips = generateMips;
    }

    public boolean isAsTexture3D() {
        return asTexture3D;
    }

    public void setAsTexture3D(boolean asTexture3D) {
        this.asTexture3D = asTexture3D;
    }

    public Type getTextureTypeHint() {
        return textureTypeHint;
    }

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
        if (this.asCube != other.asCube) {
            return false;
        }
        if (this.asTexture3D != other.asTexture3D) {
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
        hash = 17 * hash + (this.asCube ? 1 : 0);
        hash = 17 * hash + (this.asTexture3D ? 1 : 0);
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
        oc.write(asCube, "as_cubemap", false);
        oc.write(anisotropy, "anisotropy", 0);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        flipY = ic.readBoolean("flip_y", false);
        generateMips = ic.readBoolean("generate_mips", false);
        asCube = ic.readBoolean("as_cubemap", false);
        anisotropy = ic.readInt("anisotropy", 0);
    }
}
