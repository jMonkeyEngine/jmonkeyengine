package com.jme3.scene.plugins.gltf;

import com.jme3.material.*;

/**
 * Created by Nehon on 08/08/2017.
 */
public abstract class PBRMaterialAdapter extends MaterialAdapter {


    public PBRMaterialAdapter() {
        addParamMapping("normalTexture", "NormalMap");
        addParamMapping("occlusionTexture", "LightMap");
        addParamMapping("emissiveTexture", "EmissiveMap");
        addParamMapping("emissiveFactor", "Emissive");
        addParamMapping("alphaMode", "alpha");
        addParamMapping("alphaCutoff", "AlphaDiscardThreshold");
        addParamMapping("doubleSided", "doubleSided");
    }

    @Override
    protected String getMaterialDefPath() {
        return "Common/MatDefs/Light/PBRLighting.j3md";
    }

    @Override
    protected MatParam adaptMatParam(MatParam param) {
        if (param.getName().equals("alpha")) {
            String alphaMode = (String) param.getValue();
            switch (alphaMode) {
                case "MASK":
                case "BLEND":
                    getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
            }
            return null;
        }
        if (param.getName().equals("doubleSided")) {
            boolean doubleSided = (boolean) param.getValue();
            if (doubleSided) {
                //Note that this is not completely right as normals on the back side will be in the wrong direction.
                getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            }
            return null;
        }
        if (param.getName().equals("NormalMap")) {
            //Set the normal map type to OpenGl
            getMaterial().setFloat("NormalType", 1.0f);
        }
        if (param.getName().equals("LightMap")) {
            //Gltf only supports AO maps (gray scales and only the r channel must be read)
            getMaterial().setBoolean("LightMapAsAOMap", true);
        }




        return param;
    }
}
