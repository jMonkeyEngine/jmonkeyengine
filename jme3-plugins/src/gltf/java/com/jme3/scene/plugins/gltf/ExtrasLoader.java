package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonElement;

/**
 * Base Interface for extra loading implementation.
 * Created by Nehon on 30/08/2017.
 */
public interface ExtrasLoader {

    /**
     * Implement this methods to handle gltf extra reading
     * Note that this method will be invoked every time an "extras" element will be found in the gltf file.
     * You can check the parentName to know where the "extras" element has been found.
     *
     * @param loader     the GltfLoader with all the data structure.
     * @param parentName the name of the element being read
     * @param parent     the element being read
     * @param extras     the content of the extras found in the element being read
     * @param input      an object containing already loaded data from the element, this is most probably a JME object
     * @return An object of the same type as input, containing the data from the input object and the eventual additional data read from the extras
     */
    Object handleExtras(GltfLoader loader, String parentName, JsonElement parent, JsonElement extras, Object input);

}
