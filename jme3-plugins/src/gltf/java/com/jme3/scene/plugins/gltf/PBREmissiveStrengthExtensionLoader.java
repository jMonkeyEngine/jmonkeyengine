/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.jme3.scene.plugins.gltf;

import com.jme3.asset.AssetKey;
import com.jme3.plugins.json.JsonElement;
import java.io.IOException;

/**
 * Extension loader for "KHR_materials_emissive_strength".
 * 
 * @author codex
 */
public class PBREmissiveStrengthExtensionLoader implements ExtensionLoader {
    
    private PBREmissiveStrengthMaterialAdapter materialAdapter = new PBREmissiveStrengthMaterialAdapter();
    
    @Override
    public Object handleExtension(GltfLoader loader, String parentName, JsonElement parent, JsonElement extension, Object input) throws IOException {
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
