/*
 * Copyright (c) 2009-2020 jMonkeyEngine
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
package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonElement;
import com.jme3.asset.AssetKey;

import java.io.IOException;

import static com.jme3.scene.plugins.gltf.GltfUtils.getAsColor;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsFloat;

/**
 * Material adapter for PBR Specular Glossiness pipeline
 * Created by Nehon on 20/08/2017.
 */
public class PBRSpecGlossExtensionLoader implements ExtensionLoader {

    private PBRSpecGlossMaterialAdapter materialAdapter = new PBRSpecGlossMaterialAdapter();

    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
        MaterialAdapter adapter = materialAdapter;
        AssetKey key = loader.getInfo().getKey();
        //check for a custom adapter for spec/gloss pipeline
        if (key instanceof GltfModelKey) {
            GltfModelKey gltfKey = (GltfModelKey) key;
            MaterialAdapter ma = gltfKey.getAdapterForMaterial("pbrSpecularGlossiness");
            if (ma != null) {
                adapter = ma;
            }
        }

        adapter.init(loader.getInfo().getManager());

        adapter.setParam("diffuseFactor", getAsColor(extension.getAsJsonObject(), "diffuseFactor"));
        adapter.setParam("specularFactor", getAsColor(extension.getAsJsonObject(), "specularFactor"));
        adapter.setParam("glossinessFactor", getAsFloat(extension.getAsJsonObject(), "glossinessFactor"));
        adapter.setParam("diffuseTexture", loader.readTexture(extension.getAsJsonObject().getAsJsonObject("diffuseTexture")));
        adapter.setParam("specularGlossinessTexture", loader.readTexture(extension.getAsJsonObject().getAsJsonObject("specularGlossinessTexture")));

        return adapter;
    }
}
