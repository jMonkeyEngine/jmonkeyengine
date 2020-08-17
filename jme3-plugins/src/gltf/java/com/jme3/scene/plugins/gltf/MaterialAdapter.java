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


import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import com.jme3.material.*;
import com.jme3.math.*;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;

import java.util.HashMap;
import java.util.Map;

/**
 * A MaterialAdapter allows to map a GLTF material to a JME material.
 * It maps each gltf parameter to its matching parameter in the JME material,
 * and allows for some conversion if the JME material model doesn't exactly match the gltf material model
 * Created by Nehon on 08/08/2017.
 */
public abstract class MaterialAdapter {

    private Map<String, String> paramsMapping = new HashMap<>();
    private Material mat;
    private AssetManager assetManager;

    /**
     * Should return the material definition used by this material adapter
     *
     * @return path to the material definition
     */
    protected abstract String getMaterialDefPath();

    protected abstract MatParam adaptMatParam(MatParam param);

    protected void init(AssetManager assetManager) {
        this.assetManager = assetManager;
        this.reset();
    }

    void reset() {
        mat = null;
    }

    protected Material getMaterial() {
        if (mat == null) {
            mat = new Material(assetManager, getMaterialDefPath());
        }
        return mat;
    }

    public void setParam(String gltfParamName, Object value) {
        String name = getJmeParamName(gltfParamName);
        if (name == null || value == null) {
            //no mapping registered or value is null, let's ignore this param
            return;
        }
        MatParam param;
        if (value instanceof Texture) {
            MatParam defParam = getMaterial().getMaterialDef().getMaterialParam(name);
            if (defParam == null) {
                throw new AssetLoadException("Material definition " + getMaterialDefPath() + " has not param with name" + name);
            }
            if (!(defParam instanceof MatParamTexture)) {
                throw new AssetLoadException("param with name" + name + "in material definition " + getMaterialDefPath() + " should be a texture param");
            }
            param = new MatParamTexture(VarType.Texture2D, name, (Texture) value, ((MatParamTexture) defParam).getColorSpace());
            param = adaptMatParam(param);
            if (param != null) {
                getMaterial().setTextureParam(param.getName(), param.getVarType(), (Texture) param.getValue());
            }
        } else {
            param = new MatParam(getVarType(value), name, value);
            param = adaptMatParam(param);
            if (param != null) {
                getMaterial().setParam(param.getName(), param.getVarType(), param.getValue());
            }
        }
    }

    protected void addParamMapping(String gltfParamName, String jmeParamName) {
        paramsMapping.put(gltfParamName, jmeParamName);
    }

    protected String getJmeParamName(String gltfParamName) {
        return paramsMapping.get(gltfParamName);
    }

    private VarType getVarType(Object value) {
        if (value instanceof Float) return VarType.Float;
        if (value instanceof Integer) return VarType.Int;
        if (value instanceof Boolean) return VarType.Boolean;
        if (value instanceof ColorRGBA) return VarType.Vector4;
        if (value instanceof Vector4f) return VarType.Vector4;
        if (value instanceof Vector3f) return VarType.Vector3;
        if (value instanceof Vector2f) return VarType.Vector2;
        if (value instanceof Matrix3f) return VarType.Matrix3;
        if (value instanceof Matrix4f) return VarType.Matrix4;
        if (value instanceof String) return VarType.Boolean;
        throw new AssetLoadException("Unsupported material parameter type : " + value.getClass().getSimpleName());
    }
}
