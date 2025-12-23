package com.jme3.vulkan.material;

import com.jme3.asset.AssetManager;
import com.jme3.material.RenderState;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.shader.ShaderStage;

import java.util.*;

public class VulkanTechnique {

    private final List<Map<String, SetLayoutBinding>> bindings = new ArrayList<>();
    private final Map<ShaderStage, String> shaders = new HashMap<>();
    private final Map<String, String> uniformDefines = new HashMap<>();
    private final Map<String, Integer> attributes = new HashMap<>();
    private final RenderState state = new RenderState();

    public void setShaderSource(ShaderStage stage, String assetName) {
        shaders.put(stage, assetName);
    }

    public void removeShader(ShaderStage stage) {
        shaders.remove(stage);
    }

    public void linkDefine(String defineName, String uniformName) {
        uniformDefines.put(defineName, uniformName);
    }

    public void unlinkDefine(String defineName) {
        uniformDefines.remove(defineName);
    }

    public void setAttributeLocation(String attribute, int location) {
        attributes.put(attribute, location);
    }

    public void removeAttribute(String attribute) {
        attributes.remove(attribute);
    }

    public Collection<ShaderModule> getShaders(LogicalDevice<?> device, AssetManager assetManager, Cache<ShaderModule> cache, VulkanMaterial material) {
        Map<String, String> defines = new HashMap<>();
        for (Map.Entry<String, String> e : uniformDefines.entrySet()) {
            Uniform<?> u = material.getUniform(e.getValue());
            if (u != null) {
                defines.put(e.getKey(), u.getDefineValue());
            }
        }
        Collection<ShaderModule> modules = new ArrayList<>(shaders.size());
        for (Map.Entry<ShaderStage, String> shaderInfo : shaders.entrySet()) {
            modules.add(ShaderModule.build(device, assetManager, s -> {
                s.setCache(cache);
                s.setAssetName(shaderInfo.getValue());
                s.setDefines(defines);
                s.setStage(shaderInfo.getKey());
            }));
        }
        return modules;
    }

    public PipelineLayout getLayout(LogicalDevice<?> device, Cache<PipelineLayout> layoutCache, Cache<DescriptorSetLayout> setCache) {
        return PipelineLayout.build(device, p -> {
            p.setCache(layoutCache);
            for (Map<String, SetLayoutBinding> bMap : bindings) {
                if (!bMap.isEmpty()) {
                    p.nextUniformSet(DescriptorSetLayout.build(device, bMap, setCache));
                }
            }
        });
    }

    public Map<String, Integer> getAttributeLocations() {
        return Collections.unmodifiableMap(attributes);
    }

    public RenderState getRenderState() {
        return state;
    }

}
