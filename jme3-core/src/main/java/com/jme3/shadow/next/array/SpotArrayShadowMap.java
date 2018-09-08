/*
 * Copyright (c) 2009-2016 jMonkeyEngine
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
package com.jme3.shadow.next.array;

import com.jme3.light.Light;
import com.jme3.light.SpotLight;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.GeometryList;
import com.jme3.texture.TextureArray;

/**
 * @author Kirill Vainer
 */
public class SpotArrayShadowMap extends BaseArrayShadowMap<SpotArrayShadowMapSlice> {

    private final SpotLight light;

    public SpotArrayShadowMap(SpotLight light, TextureArray array, int firstArraySlice, int textureSize) {
        super(array, firstArraySlice);
        this.light = light;
        slices = new SpotArrayShadowMapSlice[]{
            new SpotArrayShadowMapSlice(array, firstArraySlice, textureSize)
        };
    }

    public void renderShadowMap(RenderManager renderManager, ViewPort viewPort, GeometryList shadowCasters) {
        shadowCasters.clear();
        slices[0].updateShadowCamera(viewPort, light, shadowCasters);
        slices[0].renderShadowMap(renderManager, light, viewPort, shadowCasters);
    }

    @Override
    public Light.Type getLightType() {
        return Light.Type.Spot;
    }
}
