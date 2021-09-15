/*
 * Copyright (c) 2009-2020 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
                case "MASK": // fallthrough
                case "BLEND":
                    getMaterial().getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
                    break;
            }
            // Alpha is a RenderState not a Material Parameter, so return null
            return null;
        }
        if (param.getName().equals("doubleSided")) {
            boolean doubleSided = (boolean) param.getValue();
            if (doubleSided) {
                //Note that this is not completely right as normals on the back side will be in the wrong direction.
                getMaterial().getAdditionalRenderState().setFaceCullMode(RenderState.FaceCullMode.Off);
            }
            // FaceCulling is a RenderState not a Material Parameter, so return null
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
