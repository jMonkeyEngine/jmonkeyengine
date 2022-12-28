/*
 * Copyright (c) 2009-2022 jMonkeyEngine
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jme3.asset.AssetLoadException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Nehon on 20/08/2017.
 */
public class CustomContentManager {

    private final static Logger logger = Logger.getLogger(CustomContentManager.class.getName());

    private GltfModelKey key;
    private GltfLoader gltfLoader;

    private final Map<String, ExtensionLoader> defaultExtensionLoaders = new HashMap<>();

    public CustomContentManager() {
        defaultExtensionLoaders.put("KHR_materials_pbrSpecularGlossiness", new PBRSpecGlossExtensionLoader());
        defaultExtensionLoaders.put("KHR_lights_punctual", new LightsPunctualExtensionLoader());
        defaultExtensionLoaders.put("KHR_materials_unlit", new UnlitExtensionLoader());
        defaultExtensionLoaders.put("KHR_texture_transform", new TextureTransformExtensionLoader());
    }

    void init(GltfLoader gltfLoader) {
        this.gltfLoader = gltfLoader;

        if (gltfLoader.getInfo().getKey() instanceof GltfModelKey) {
            this.key = (GltfModelKey) gltfLoader.getInfo().getKey();
        }

        JsonArray extensionUsed = gltfLoader.getDocRoot().getAsJsonArray("extensionsUsed");
        if (extensionUsed != null) {
            for (JsonElement extElem : extensionUsed) {
                String ext = extElem.getAsString();
                if (ext != null) {
                    if (defaultExtensionLoaders.get(ext) == null && (this.key != null && this.key.getExtensionLoader(ext) == null)) {
                        logger.log(Level.WARNING, "Extension " + ext + " is not supported, please provide your own implementation in the GltfModelKey");
                    }
                }
            }
        }
        JsonArray extensionRequired = gltfLoader.getDocRoot().getAsJsonArray("extensionsRequired");
        if (extensionRequired != null) {
            for (JsonElement extElem : extensionRequired) {
                String ext = extElem.getAsString();
                if (ext != null) {
                    if (defaultExtensionLoaders.get(ext) == null && (this.key != null && this.key.getExtensionLoader(ext) == null)) {
                        logger.log(Level.SEVERE, "Extension " + ext + " is mandatory for this file, the loaded scene result will be unexpected.");
                    }
                }
            }
        }
    }

    public <T> T readExtensionAndExtras(String name, JsonElement el, T input) throws AssetLoadException, IOException {
        T output = readExtension(name, el, input);
        output = readExtras(name, el, output);
        return output;
    }

    @SuppressWarnings("unchecked")
    private <T> T readExtension(String name, JsonElement el, T input) throws AssetLoadException, IOException {
        JsonElement extensions = el.getAsJsonObject().getAsJsonObject("extensions");
        if (extensions == null) {
            return input;
        }

        for (Map.Entry<String, JsonElement> ext : extensions.getAsJsonObject().entrySet()) {
            ExtensionLoader loader = null;
            if (key != null) {
                loader = key.getExtensionLoader(ext.getKey());
            }
            if (loader == null) {
                loader = defaultExtensionLoaders.get(ext.getKey());
            }

            if (loader == null) {
                logger.log(Level.WARNING, "Could not find loader for extension " + ext.getKey());
                continue;
            }

            try {
                return (T) loader.handleExtension(gltfLoader, name, el, ext.getValue(), input);
            } catch (ClassCastException e) {
                throw new AssetLoadException("Extension loader " + loader.getClass().getName() + " for extension " + ext.getKey() + " is incompatible with type " + input.getClass(), e);
            }
        }

        return input;
    }

    @SuppressWarnings("unchecked")
    private <T> T readExtras(String name, JsonElement el, T input) throws AssetLoadException {
        if (key == null) {
            return input;
        }
        ExtrasLoader loader;
        loader = key.getExtrasLoader();
        if (loader == null) {
            return input;
        }
        JsonElement extras = el.getAsJsonObject().getAsJsonObject("extras");
        if (extras == null) {
            return input;
        }

        try {
            return (T) loader.handleExtras(gltfLoader, name, el, extras, input);
        } catch (ClassCastException e) {
            throw new AssetLoadException("Extra loader " + loader.getClass().getName() + " for " + name + " is incompatible with type " + input.getClass(), e);
        }

    }


}
