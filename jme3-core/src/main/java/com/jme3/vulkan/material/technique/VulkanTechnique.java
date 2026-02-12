package com.jme3.vulkan.material.technique;

import com.jme3.asset.AssetManager;
import com.jme3.material.RenderState;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.descriptors.SetLayoutBinding;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.VulkanMaterial;
import com.jme3.vulkan.material.uniforms.Uniform;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.pipeline.cache.Cache;
import com.jme3.vulkan.material.shader.ShaderModule;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

import java.util.*;

public class VulkanTechnique implements NewTechnique {

    private final List<Map<String, SetLayoutBinding>> bindings = new ArrayList<>();
    private final List<PushConstantRange> pushConstants = new ArrayList<>();
    private final Map<ShaderStage, String> shaders = new HashMap<>();
    private final Map<String, Define> uniformDefines = new HashMap<>();
    private final Map<String, Integer> attributes = new HashMap<>();
    private final RenderState state = new RenderState();
    
    @Override
    public void setBinding(int set, String name, SetLayoutBinding binding) {
        while (set >= bindings.size()) {
            bindings.add(null);
        }
        Map<String, SetLayoutBinding> bindingMap = bindings.get(set);
        if (bindingMap == null) {
            bindings.set(set, bindingMap = new HashMap<>());
        }
        bindingMap.put(name, binding);
    }

    @Override
    public void setShaderSource(ShaderStage stage, String assetName) {
        shaders.put(stage, assetName);
    }

    @Override
    public void removeShader(ShaderStage stage) {
        shaders.remove(stage);
    }

    @Override
    public void linkDefine(String defineName, String uniformName, Flag<ShaderStage> scope) {
        uniformDefines.put(defineName, new Define(defineName, uniformName, scope));
    }

    @Override
    public void unlinkDefine(String defineName) {
        uniformDefines.remove(defineName);
    }

    @Override
    public NewTechnique clone() {
        try {
            VulkanTechnique clone = (VulkanTechnique)super.clone();
            for (Map<String, SetLayoutBinding> set : bindings) {
                clone.bindings.add(new HashMap<>(set));
            }
            clone.pushConstants.addAll(pushConstants);
            clone.shaders.putAll(shaders);
            clone.uniformDefines.putAll(uniformDefines);
            clone.attributes.putAll(attributes);
            clone.state.set(state);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAttributeLocation(String attribute, int location) {
        attributes.put(attribute, location);
    }

    public void removeAttribute(String attribute) {
        attributes.remove(attribute);
    }

    public void addPushConstants(PushConstantRange range) {
        pushConstants.add(range);
    }

    public Collection<ShaderModule> getShaders(LogicalDevice<?> device, AssetManager assetManager,
                                               Cache<ShaderModule> cache, VulkanMaterial material) {
        Collection<ShaderModule> modules = new ArrayList<>(shaders.size());
        for (Map.Entry<ShaderStage, String> shaderInfo : shaders.entrySet()) {
            modules.add(ShaderModule.build(device, assetManager, s -> {
                s.setCache(cache);
                s.setAssetName(shaderInfo.getValue());
                s.setStage(shaderInfo.getKey());
                for (Define d : uniformDefines.values()) {
                    if (d.scope.containsAny(shaderInfo.getKey())) {
                        Uniform<?> u = material.getUniform(d.uniform);
                        if (u != null) {
                            s.setDefine(d.define, u.getDefineValue());
                        }
                    }
                }
            }));
        }
        return modules;
    }

    public PipelineLayout getLayout(LogicalDevice<?> device, Cache<PipelineLayout> layoutCache,
                                    Cache<DescriptorSetLayout> setCache) {
        return PipelineLayout.build(device, p -> {
            p.setCache(layoutCache);
            for (Map<String, SetLayoutBinding> set : bindings) {
                if (set == null) {
                    throw new IllegalStateException("Each set layout must have at least one binding.");
                }
                p.addUniformSet(DescriptorSetLayout.build(device, set, setCache));
            }
            p.addPushConstants(pushConstants);
        });
    }

    public Map<String, Integer> getAttributeLocations() {
        return Collections.unmodifiableMap(attributes);
    }

    public RenderState getRenderState() {
        return state;
    }

    public VulkanTechnique copy() {
        VulkanTechnique copy = new VulkanTechnique();
        for (Map<String, SetLayoutBinding> set : bindings) {
            copy.bindings.add(new HashMap<>(set));
        }
        copy.shaders.putAll(shaders);
        copy.uniformDefines.putAll(uniformDefines);
        copy.attributes.putAll(attributes);
        copy.state.set(state);
        return copy;
    }

    private static class Define {

        public final String define, uniform;
        public final Flag<ShaderStage> scope;

        private Define(String define, String uniform, Flag<ShaderStage> scope) {
            this.define = define;
            this.uniform = uniform;
            this.scope = scope;
        }

    }

}
