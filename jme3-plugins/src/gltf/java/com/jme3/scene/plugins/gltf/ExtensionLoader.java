package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonElement;

import java.io.IOException;

/**
 * Base Interface for extension loading implementation.
 *
 * Created by Nehon on 20/08/2017.
 */
public interface ExtensionLoader {

    /**
     * Implement this methods to handle a gltf extension reading
     *
     * @param loader     the GltfLoader with all the data structure.
     * @param parentName the name of the element being read
     * @param parent     the element being read
     * @param extension  the content of the extension found in the element being read
     * @param input      an object containing already loaded data from the element, this is most probably a JME object
     * @return An object of the same type as input, containing the data from the input object and the eventual additional data read from the extension
     */
    Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException;

}
