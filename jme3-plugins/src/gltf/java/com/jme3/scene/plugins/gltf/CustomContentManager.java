/*
 * Copyright (c) 2009-2024 jMonkeyEngine
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.asset.AssetLoadException;
import com.jme3.plugins.json.JsonArray;
import com.jme3.plugins.json.JsonElement;
import com.jme3.plugins.json.JsonObject;

/**
 * Created by Nehon on 20/08/2017.
 */
public class CustomContentManager {
    static volatile Class<? extends ExtrasLoader> defaultExtraLoaderClass = UserDataLoader.class;
    private ExtrasLoader defaultExtraLoaderInstance;


    private final static Logger logger = Logger.getLogger(CustomContentManager.class.getName());

    private GltfModelKey key;
    private GltfLoader gltfLoader;

    
    /**
     * The mapping from glTF extension names to the classes that
     * represent the respective laoders.
     */
    static final Map<String, Class<? extends ExtensionLoader>> defaultExtensionLoaders = new ConcurrentHashMap<>();
    static {
        defaultExtensionLoaders.put("KHR_materials_pbrSpecularGlossiness", PBRSpecGlossExtensionLoader.class);
        defaultExtensionLoaders.put("KHR_lights_punctual", LightsPunctualExtensionLoader.class);
        defaultExtensionLoaders.put("KHR_materials_unlit", UnlitExtensionLoader.class);
        defaultExtensionLoaders.put("KHR_texture_transform", TextureTransformExtensionLoader.class);
        defaultExtensionLoaders.put("KHR_materials_emissive_strength", PBREmissiveStrengthExtensionLoader.class);
        defaultExtensionLoaders.put("KHR_draco_mesh_compression", DracoMeshCompressionExtensionLoader.class);
    }
    
    /**
     * The mapping from glTF extension names to the actual loader instances
     * that have been lazily created from the defaultExtensionLoaders,
     * in {@link #findExtensionLoader(String)}
     */
    private final Map<String, ExtensionLoader> loadedExtensionLoaders = new HashMap<>();

    public CustomContentManager() {
    }
    
    /**
     * Returns the default extras loader.
     * @return the default extras loader.
     */
    public ExtrasLoader getDefaultExtrasLoader() {
        if (defaultExtraLoaderClass == null) { 
            defaultExtraLoaderInstance = null; // do not hold reference
            return null;
        }

        if (defaultExtraLoaderInstance != null
                && defaultExtraLoaderInstance.getClass() != defaultExtraLoaderClass) {
            defaultExtraLoaderInstance = null; // reset instance if class changed
        }

        try {
            defaultExtraLoaderInstance = defaultExtraLoaderClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not instantiate default extras loader", e);
            defaultExtraLoaderInstance = null;
        }

        return defaultExtraLoaderInstance;
    }

    void init(GltfLoader gltfLoader) {
        this.gltfLoader = gltfLoader;

        if (gltfLoader.getInfo().getKey() instanceof GltfModelKey) {
            this.key = (GltfModelKey) gltfLoader.getInfo().getKey();
        }

        // For extensions that are USED but not supported, print a warning
        List<String> extensionsUsed = getArrayAsStringList(gltfLoader.getDocRoot(), "extensionsUsed");
        for (String extensionName : extensionsUsed) {
            if (!isExtensionSupported(extensionName)) {
                logger.log(Level.WARNING, "Extension " + extensionName
                        + " is not supported, please provide your own implementation in the GltfModelKey");
            }
        }

        // For extensions that are REQUIRED but not supported,
        // throw an AssetLoadException by default
        // If the GltfModelKey#isStrict returns false, then
        // still print an error message, at least
        List<String> extensionsRequired = getArrayAsStringList(gltfLoader.getDocRoot(), "extensionsRequired");
        for (String extensionName : extensionsRequired) {
            if (!isExtensionSupported(extensionName)) {
                if (this.key != null && this.key.isStrict()) {
                    throw new AssetLoadException(
                            "Extension " + extensionName + " is required for this file.");
                } else {
                    logger.log(Level.SEVERE, "Extension " + extensionName
                            + " is required for this file. The behavior of the loader is unspecified.");
                }
            }
        }
    }

    /**
     * Returns a (possibly unmodifiable) list of the string representations of the elements in the specified
     * array, or an empty list if the specified array does not exist.
     * 
     * @param jsonObject
     *            The JSON object
     * @param property
     *            The property name of the array property
     * @return The list
     */
    private static List<String> getArrayAsStringList(JsonObject jsonObject, String property) {
        JsonArray jsonArray = jsonObject.getAsJsonArray(property);
        if (jsonArray == null) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<String>();
        for (JsonElement jsonElement : jsonArray) {
            String string = jsonElement.getAsString();
            if (string != null) {
                list.add(string);
            }
        }
        return list;
    }

    /**
     * Returns whether the specified glTF extension is supported.
     * 
     * The given string is the name of the extension, e.g. <code>KHR_texture_transform</code>.
     * 
     * This will return whether there is a default extension loader for the given extension registered in the
     * {@link #defaultExtensionLoaders}, or whether the <code>GltfModelKey</code> that was obtained from the
     * <code>GltfLoader</code> contains a custom extension loader that was registered via
     * {@link GltfModelKey#registerExtensionLoader(String, ExtensionLoader)}.
     * 
     * @param ext
     *            The glTF extension name
     * @return Whether the given extension is supported
     */
    private boolean isExtensionSupported(String ext) {
        if (defaultExtensionLoaders.containsKey(ext)) {
            return true;
        }
        if (this.key != null && this.key.getExtensionLoader(ext) != null) {
            return true;
        }
        return false;
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
            ExtensionLoader loader = findExtensionLoader(ext.getKey());
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

    /**
     * Returns the <code>ExtensionLoader</code> for the given glTF extension name.
     * 
     * The extension name is a name like <code>KHR_texture_transform</code>. This method will first try to
     * return the custom extension loader that was registered in the GltfModelKey.
     * 
     * If it does not exist, it will return an instance of the default extension loader that was registered
     * for the given extension, lazily creating the instance based on the registered defaultExtensionLoaders.
     * 
     * @param extensionName
     *            The extension name
     * @return The loader, or <code>null</code> if no loader could be found or instantiated
     */
    private ExtensionLoader findExtensionLoader(String extensionName) {
        if (key != null) {
            ExtensionLoader loader = key.getExtensionLoader(extensionName);
            if (loader != null) {
                return loader;
            }
        }

        ExtensionLoader loader = loadedExtensionLoaders.get(extensionName);
        if (loader != null) {
            return loader;
        }
        try {
            Class<? extends ExtensionLoader> clz = defaultExtensionLoaders.get(extensionName);
            if (clz != null) {
                loader = clz.getDeclaredConstructor().newInstance();
                loadedExtensionLoaders.put(extensionName, loader);
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            logger.log(Level.WARNING, "Could not instantiate loader", e);
        }
        return loader;

    }

    @SuppressWarnings("unchecked")
    private <T> T readExtras(String name, JsonElement el, T input) throws AssetLoadException {
        ExtrasLoader loader = null;

        if (key != null) { // try to get the extras loader from the model key if available
            loader = key.getExtrasLoader();
        }
 
        if (loader == null) { // if no loader was found, use the default extras loader
            loader = getDefaultExtrasLoader();
        }

        if (loader == null) { // if default loader is not set or failed to instantiate, skip extras
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
