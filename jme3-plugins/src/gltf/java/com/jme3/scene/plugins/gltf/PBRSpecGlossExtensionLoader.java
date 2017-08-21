package com.jme3.scene.plugins.gltf;

import com.google.gson.JsonElement;

import static com.jme3.scene.plugins.gltf.GltfUtils.getAsColor;
import static com.jme3.scene.plugins.gltf.GltfUtils.getAsFloat;

/**
 * Material adapter for PBR Specular Glossiness pipeline
 * Created by Nehon on 20/08/2017.
 */
public class PBRSpecGlossExtensionLoader implements ExtensionLoader {

    private PBRSpecGlossMaterialAdapter materialAdapter = new PBRSpecGlossMaterialAdapter();

    @Override
    public Object handleExtension(GltfLoader loader, JsonElement parent, JsonElement extension, Object input) {
        materialAdapter.init(loader.getInfo().getManager());

        materialAdapter.setParam("diffuseFactor", getAsColor(extension.getAsJsonObject(), "diffuseFactor"));
        materialAdapter.setParam("specularFactor", getAsColor(extension.getAsJsonObject(), "specularFactor"));
        materialAdapter.setParam("glossinessFactor", getAsFloat(extension.getAsJsonObject(), "glossinessFactor"));
        materialAdapter.setParam("diffuseTexture", loader.readTexture(extension.getAsJsonObject().getAsJsonObject("diffuseTexture")));
        materialAdapter.setParam("specularGlossinessTexture", loader.readTexture(extension.getAsJsonObject().getAsJsonObject("specularGlossinessTexture")));

        return materialAdapter;
    }
}
