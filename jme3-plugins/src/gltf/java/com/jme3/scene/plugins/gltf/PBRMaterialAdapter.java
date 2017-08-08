package com.jme3.scene.plugins.gltf;

import com.jme3.material.*;

/**
 * Created by Nehon on 08/08/2017.
 */
public class PBRMaterialAdapter extends MaterialAdapter {


    public PBRMaterialAdapter() {
        addParamMapping("baseColorFactor", "BaseColor");
        addParamMapping("baseColorTexture", "BaseColorMap");
        addParamMapping("metallicFactor", "Metallic");
        addParamMapping("roughnessFactor", "Roughness");
        addParamMapping("metallicRoughnessTexture", "MetallicRoughnessMap");
        addParamMapping("normalTexture", "NormalMap");
        addParamMapping("occlusionTexture", "LightMap");
        addParamMapping("emisiveTexture", "EmissiveMap");
        addParamMapping("emisiveFactor", "Emissive");
        addParamMapping("alphaMode", "alpha");
        addParamMapping("alphaCutoff", "AlphaDiscardThreshold");
        addParamMapping("doubleSided", "doubleSided");
    }

    @Override
    protected String getMaterialDefPath() {
        return "Common/MatDefs/Light/PBRLighting.j3md";
    }

    @Override
    protected MatParam adaptMatParam(Material mat, MatParam param) {
        if (param.getName().equals("alpha")) {
            String alphaMode = (String) param.getValue();
            switch (alphaMode) {
                case "MASK":
                case "BLEND":
                    mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            }
            return null;
        }
        if (param.getName().equals("doubleSided")) {
            boolean doubleSided = (boolean) param.getValue();
            if (doubleSided) {
                //Note that this is not completely right as normals on the back side will be in the wrong direction.
                mat.getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            }
            return null;
        }
        if (param.getName().equals("MetallicRoughnessMap")) {
            //use packed Metallic/Roughness
            mat.setBoolean("UsePackedMR", true);
        }


        return param;
    }
}
