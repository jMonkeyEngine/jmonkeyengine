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
package com.jme3.texture;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetProcessor;
import com.jme3.asset.TextureKey;
import java.nio.ByteBuffer;

public class TextureProcessor implements AssetProcessor {

    @Override
    public Object postProcess(AssetKey key, Object obj) {
        TextureKey texKey = (TextureKey) key;
        Image img = (Image) obj;
        if (img == null) {
            return null;
        }

        Texture tex;
        if (texKey.getTextureTypeHint() == Texture.Type.CubeMap) {
            if (texKey.isFlipY()) {
                // also flip -y and +y image in cubemap
                ByteBuffer pos_y = img.getData(2);
                img.setData(2, img.getData(3));
                img.setData(3, pos_y);
            }
            tex = new TextureCubeMap();
        } else if (texKey.getTextureTypeHint() == Texture.Type.ThreeDimensional) {
            tex = new Texture3D();
        } else {
            tex = new Texture2D();
        }

        // enable mipmaps if image has them
        // or generate them if requested by user
        if (img.hasMipmaps() || texKey.isGenerateMips()) {
            tex.setMinFilter(Texture.MinFilter.Trilinear);
        }

        tex.setAnisotropicFilter(texKey.getAnisotropy());
        tex.setName(texKey.getName());
        tex.setImage(img);
        return tex;
    }

    public Object createClone(Object obj) {
        Texture tex = (Texture) obj;
        return tex.clone();
    }
    
}
