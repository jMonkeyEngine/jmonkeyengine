package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonElement;

/**
 * Created by Nehon on 20/08/2017.
 */
public interface ExtensionLoader {

    Object handleExtension(GltfLoader loader, JsonElement parent, JsonElement extension, Object input);

}
