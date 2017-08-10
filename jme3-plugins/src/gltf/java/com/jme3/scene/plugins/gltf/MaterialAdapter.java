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
 * It maps each gltf parameter to it's matching parameter in the JME material,
 * and allows for some conversion if the JME material model doesn't exactly match the gltf material model
 * Created by Nehon on 08/08/2017.
 */
public abstract class MaterialAdapter {

    private Map<String, String> paramsMapping = new HashMap<>();

    /**
     * Should return the material definition used by this material adapter
     *
     * @return
     */
    protected abstract String getMaterialDefPath();

    protected abstract MatParam adaptMatParam(Material mat, MatParam param);

    public Material getMaterial(AssetManager assetManager) {
        return new Material(assetManager, getMaterialDefPath());
    }

    public void setParam(Material mat, String gltfParamName, Object value) {
        String name = getJmeParamName(gltfParamName);
        if (name == null || value == null) {
            //no mapping registered or value is null, let's ignore this param
            return;
        }
        MatParam param;
        if (value instanceof Texture) {
            MatParam defParam = mat.getMaterialDef().getMaterialParam(name);
            if (defParam == null) {
                throw new AssetLoadException("Material definition " + getMaterialDefPath() + " has not param with name" + name);
            }
            if (!(defParam instanceof MatParamTexture)) {
                throw new AssetLoadException("param with name" + name + "in material definition " + getMaterialDefPath() + " should be a texture param");
            }
            param = new MatParamTexture(VarType.Texture2D, name, (Texture) value, ((MatParamTexture) defParam).getColorSpace());
            param = adaptMatParam(mat, param);
            if (param != null) {
                mat.setTextureParam(param.getName(), param.getVarType(), (Texture) param.getValue());
            }
        } else {
            param = new MatParam(getVarType(value), name, value);
            param = adaptMatParam(mat, param);
            if (param != null) {
                mat.setParam(param.getName(), param.getVarType(), param.getValue());
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
