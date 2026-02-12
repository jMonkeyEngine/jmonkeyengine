package com.jme3.vulkan.material;

import com.jme3.asset.AssetKey;
import com.jme3.asset.CloneableSmartAsset;
import com.jme3.backend.Engine;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.vulkan.material.technique.NewTechnique;
import com.jme3.vulkan.material.technique.VulkanTechnique;
import com.jme3.vulkan.material.uniforms.Uniform;

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the uniforms, techniques, and render state of a material from
 * which new materials may be created.
 */
public class NewMaterialDef <T extends Material> implements CloneableSmartAsset {

    private final Map<String, Uniform<?>> uniforms = new HashMap<>();
    private final Map<String, NewTechnique> techniques = new HashMap<>();
    private final RenderState renderState = new RenderState();
    private AssetKey key;

    public T createMaterial(Engine engine) {
        T mat = (T)engine.createMaterial();
        for (Map.Entry<String, Uniform<?>> u : uniforms.entrySet()) {
            mat.setUniform(u.getKey(), u.getValue().clone(engine));
        }
        for (Map.Entry<String, NewTechnique> t : techniques.entrySet()) {
            mat.setTechnique(t.getKey(), t.getValue().clone());
        }
        mat.getAdditionalRenderState().set(renderState);
        return mat;
    }

    @Override
    public CloneableSmartAsset clone() {
        try {
            NewMaterialDef clone = (NewMaterialDef)super.clone();
            clone.uniforms.putAll(uniforms);
            clone.renderState.set(renderState);
            techniques.forEach((k, v) -> clone.techniques.put(k, v.clone()));
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Failed to clone.", e);
        }
    }

    @Override
    public void setKey(AssetKey key) {
        this.key = key;
    }

    @Override
    public AssetKey getKey() {
        return key;
    }

    public void setUniform(String name, Uniform<?> def) {
        uniforms.put(name, def);
    }

    public void setTechnique(String name, VulkanTechnique technique) {
        techniques.put(name, technique);
    }

    public Uniform<?> getUniform(String name) {
        return uniforms.get(name);
    }

    public NewTechnique getTechnique(String name) {
        return techniques.get(name);
    }

}
