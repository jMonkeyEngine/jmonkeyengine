/*
 * Copyright (c) 2023-2024 jMonkeyEngine
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

import com.jme3.asset.AssetKey;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;

import java.io.IOException;
import java.util.logging.Logger;

import static com.jme3.scene.plugins.gltf.GltfMaterialData.MATERIAL_EXTENSION_PARAM_PREFIX;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsFloat;

/**
 * Extension loader for "KHR_materials_emissive_strength".
 * 
 * @author codex
 */
public class PBREmissiveStrengthExtensionLoader implements ExtensionLoader {

    public static final String EXTENSION_NAME = "KHR_materials_emissive_strength";

    public static final String EMISSIVE_STRENGTH_PARAM = MATERIAL_EXTENSION_PARAM_PREFIX + EXTENSION_NAME + ".emissiveStrength";

    private static final Logger logger = Logger.getLogger(PBREmissiveStrengthExtensionLoader.class.getName());

    private PBREmissiveStrengthMaterialAdapter materialAdapter = new PBREmissiveStrengthMaterialAdapter();

    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
        if (input instanceof GltfMaterialData) {
            GltfMaterialData gltfMaterialData = (GltfMaterialData) input;
            gltfMaterialData.addGltfExtension(EXTENSION_NAME);

            JsonObject extensionJson = extension.getAsJsonObject();
            gltfMaterialData.setGltfParam(EMISSIVE_STRENGTH_PARAM, getAsFloat(extensionJson, "emissiveStrength"));

        } else if (input instanceof MaterialAdapter) {
            return handleExtensionForMaterialAdapter(loader, parentName, parent, extension, input);

        } else {
            logger.warning(EXTENSION_NAME + " extension added on unsupported element");
        }

        return input;
    }

    private Object handleExtensionForMaterialAdapter(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
        MaterialAdapter adapter = materialAdapter;
        AssetKey key = loader.getInfo().getKey();
        //check for a custom adapter for emissive strength
        if (key instanceof GltfModelKey) {
            MaterialAdapter custom = ((GltfModelKey)key).getAdapterForMaterial("pbrEmissiveStrength");
            if (custom != null) {
                adapter = custom;
            }
        }        
        adapter.init(loader.getInfo().getManager());
        adapter.setParam("emissiveStrength", GltfUtils.getAsFloat(extension.getAsJsonObject(), "emissiveStrength"));
        return adapter;
    }
    
}
