package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jme3.asset.AssetLoadException;

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

    private static Map<String, ExtensionLoader> defaultExtensionLoaders = new HashMap<>();

    static {
        defaultExtensionLoaders.put("KHR_materials_pbrSpecularGlossiness", new PBRSpecGlossExtensionLoader());
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

    public <T> T readExtension(JsonElement el, T input) throws AssetLoadException {
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
                return (T) loader.handleExtension(gltfLoader, el, ext.getValue(), input);
            } catch (ClassCastException e) {
                throw new AssetLoadException("Extension loader " + loader.getClass().getName() + " for extension " + ext.getKey() + " is incompatible with type " + input.getClass(), e);
            }
        }

        return input;
    }

}
