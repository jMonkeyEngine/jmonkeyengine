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
