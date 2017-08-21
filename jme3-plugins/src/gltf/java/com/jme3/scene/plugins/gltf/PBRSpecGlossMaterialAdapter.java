package com.jme3.scene.plugins.gltf;

import com.jme3.material.MatParam;

/**
 * Created by Nehon on 20/08/2017.
 */
public class PBRSpecGlossMaterialAdapter extends PBRMaterialAdapter {

    public PBRSpecGlossMaterialAdapter() {
        super();
        addParamMapping("diffuseFactor", "BaseColor");
        addParamMapping("diffuseTexture", "BaseColorMap");
        addParamMapping("specularFactor", "Specular");
        addParamMapping("glossinessFactor", "Glossiness");
        addParamMapping("specularGlossinessTexture", "SpecularGlossinessMap");
    }

    @Override
    protected MatParam adaptMatParam(MatParam param) {
        getMaterial().setBoolean("UseSpecGloss", true);
        return super.adaptMatParam(param);
    }
}
