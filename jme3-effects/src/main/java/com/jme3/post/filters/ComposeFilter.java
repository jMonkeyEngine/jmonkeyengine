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
package com.jme3.post.filters;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.material.Material;
import com.jme3.post.Filter;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.Texture2D;
import java.io.IOException;

/**
 * This filter composes a texture with the viewport texture. This is used to
 * compose post-processed texture from another viewport.
 *
 * the compositing is done using the alpha value of the viewportTexture :
 * mix(compositeTextureColor, viewPortColor, viewportColor.alpha);
 *
 * It's important for a good result that the viewport clear color alpha be 0.
 *
 * @author Rémy Bouquet aka Nehon
 */
public class ComposeFilter extends Filter {

    private Texture2D compositeTexture;

    /**
     * creates a ComposeFilter
     */
    public ComposeFilter() {
        super("Compose Filter");
    }

    /**
     * creates a ComposeFilter with the given texture
     *
     * @param compositeTexture the texture to use (alias created)
     */
    public ComposeFilter(Texture2D compositeTexture) {
        this();
        this.compositeTexture = compositeTexture;
    }

    @Override
    protected Material getMaterial() {

        material.setTexture("CompositeTexture", compositeTexture);
        return material;
    }

    /**
     *
     * @return the compositeTexture
     */
    public Texture2D getCompositeTexture() {
        return compositeTexture;
    }

    /**
     * sets the compositeTexture
     *
     * @param compositeTexture the desired texture (alias created)
     */
    public void setCompositeTexture(Texture2D compositeTexture) {
        this.compositeTexture = compositeTexture;
    }

    @Override
    protected void initFilter(AssetManager manager, RenderManager renderManager, ViewPort vp, int w, int h) {
        material = new Material(manager, "Common/MatDefs/Post/Compose.j3md");
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
    }
}
