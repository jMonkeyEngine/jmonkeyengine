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
package com.jme3.shadow;

import com.jme3.asset.AssetManager;
import com.jme3.export.InputCapsule;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.OutputCapsule;
import com.jme3.light.SpotLight;
import java.io.IOException;

/**
 *
 * This Filter does basically the same as a SpotLightShadowRenderer except it
 * renders the post shadow pass as a fulscreen quad pass instead of a geometry
 * pass. It's mostly faster than PssmShadowRenderer as long as you have more
 * than a about ten shadow recieving objects. The expense is the draw back that
 * the shadow Recieve mode set on spatial is ignored. So basically all and only
 * objects that render depth in the scene receive shadows. See this post for
 * more details
 * http://jmonkeyengine.org/groups/general-2/forum/topic/silly-question-about-shadow-rendering/#post-191599
 *
 * API is basically the same as the PssmShadowRenderer;
 *
 * @author Rémy Bouquet aka Nehon
 */
public class SpotLightShadowFilter extends AbstractShadowFilter<SpotLightShadowRenderer> {

    /**
     * Creates a SpotLight Shadow Filter
     *
     * @param assetManager the application asset manager
     * @param shadowMapSize the size of the rendered shadowmaps (512,1024,2048,
     * etc...) the more quality, the less fps).
     */
    public SpotLightShadowFilter(AssetManager assetManager, int shadowMapSize) {
        super(assetManager, shadowMapSize, new SpotLightShadowRenderer(assetManager, shadowMapSize));
    }

    /**
     * return the light used to cast shadows
     *
     * @return the SpotLight
     */
    public SpotLight getLight() {
        return shadowRenderer.getLight();
    }

    /**
     * Sets the light to use to cast shadows
     *
     * @param light a SpotLight
     */
    public void setLight(SpotLight light) {
        shadowRenderer.setLight(light);
    }
   
    @Override
    public void write(JmeExporter ex) throws IOException {
        super.write(ex);
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(shadowRenderer, "shadowRenderer", null);

    }

    @Override
    public void read(JmeImporter im) throws IOException {
        super.read(im);
        InputCapsule ic = im.getCapsule(this);
        shadowRenderer = (SpotLightShadowRenderer) ic.readSavable("shadowRenderer", null);
    }
}
