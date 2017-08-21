package com.jme3.scene.plugins.gltf;

/**
 * Created by Nehon on 20/08/2017.
 */
public class PBRMetalRoughMaterialAdapter extends PBRMaterialAdapter {

    public PBRMetalRoughMaterialAdapter() {
        super();
        addParamMapping("baseColorFactor", "BaseColor");
        addParamMapping("baseColorTexture", "BaseColorMap");
        addParamMapping("metallicFactor", "Metallic");
        addParamMapping("roughnessFactor", "Roughness");
        addParamMapping("metallicRoughnessTexture", "MetallicRoughnessMap");
    }
}
