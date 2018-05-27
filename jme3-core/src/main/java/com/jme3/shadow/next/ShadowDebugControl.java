/*
 * Copyright (c) 2009-2017 jMonkeyEngine
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
package com.jme3.shadow.next;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.AbstractControl;
import com.jme3.texture.Image;
import com.jme3.texture.TextureArray;
import com.jme3.ui.Picture;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the shadow maps on the screen
 *
 * @author Kirill Vainer
 */
final class ShadowDebugControl extends AbstractControl {

    private final List<Picture> pictures = new ArrayList<>();

    public ShadowDebugControl(AssetManager assetManager, InPassShadowRenderer shadowRenderer) {
        TextureArray shadowMapArray = shadowRenderer.getShadowMapTexture();
        Image shadowMap = shadowMapArray.getImage();
        for (int i = 0; i < shadowMap.getDepth(); i++) {
            Picture picture = new Picture("Shadow Map " + i);
            picture.setPosition(20, i * 128 + 20);
            picture.setWidth(128);
            picture.setHeight(128);

            Material material = new Material(assetManager, "Common/MatDefs/Shadow/ShowShadowArray.j3md");
            material.setTexture("ShadowMapArray", shadowMapArray);
            material.setFloat("ShadowMapSlice", i);
            picture.setMaterial(material);

            pictures.add(picture);
        }
    }

    @Override
    public void setSpatial(Spatial spatial) {
        if (spatial != null) {
            for (Picture picture : pictures) {
                ((Node) spatial).detachChild(picture);
            }
        }
        super.setSpatial(spatial);
        if (spatial != null) {
            for (Picture picture : pictures) {
                ((Node) spatial).attachChild(picture);
            }
        }
    }

    @Override
    protected void controlUpdate(float tpf) {

    }

    @Override
    protected void controlRender(RenderManager rm, ViewPort vp) {
    }

}
