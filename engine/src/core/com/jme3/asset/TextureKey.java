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
package com.jme3.asset;

import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.texture.Texture.Type;
import com.jme3.texture.*;
import java.io.IOException;
import java.nio.ByteBuffer;

public class TextureKey extends AssetKey<Texture> {

    private boolean generateMips;
    private boolean flipY;
    private boolean asCube;
    private boolean asTexture3D;
    private int anisotropy;
    private Texture.Type textureTypeHint=Texture.Type.TwoDimensional;

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
        return name + (flipY ? " (Flipped)" : "") + (asCube ? " (Cube)" : "") + (generateMips ? " (Mipmaped)" : "");
    }

    /**
     * Enable smart caching for textures
     * @return true to enable smart cache
     */
    @Override
    public boolean useSmartCache() {
        return true;
    }

    @Override
    public Object createClonedInstance(Object asset) {
        Texture tex = (Texture) asset;
        return tex.createSimpleClone();
    }

    @Override
    public Object postProcess(Object asset) {
        Image img = (Image) asset;
        if (img == null) {
            return null;
        }

        Texture tex;
        if (isAsCube()) {
            if (isFlipY()) {
                // also flip -y and +y image in cubemap
                ByteBuffer pos_y = img.getData(2);
                img.setData(2, img.getData(3));
                img.setData(3, pos_y);
            }
            tex = new TextureCubeMap();
        } else if (isAsTexture3D()) {
            tex = new Texture3D();
        } else {
            tex = new Texture2D();
        }

        // enable mipmaps if image has them
        // or generate them if requested by user
        if (img.hasMipmaps() || isGenerateMips()) {
            tex.setMinFilter(Texture.MinFilter.Trilinear);
        }

        tex.setAnisotropicFilter(getAnisotropy());
        tex.setName(getName());
        tex.setImage(img);
        return tex;
    }

    public boolean isFlipY() {
        return flipY;
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TextureKey)) {
            return false;
        }
        return super.equals(other) && isFlipY() == ((TextureKey) other).isFlipY();
    }

    public Type getTextureTypeHint() {
        return textureTypeHint;
    }

    public void setTextureTypeHint(Type textureTypeHint) {
        this.textureTypeHint = textureTypeHint;
    }   
    

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
