package com.jme3.vulkan.descriptors;

import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;

import java.util.HashMap;
import java.util.Map;

public class DynamicDescriptorSet implements ShaderBindingSet {

    private final DescriptorPool pool;
    private final Map<Integer, DescriptorBinding> bindings = new HashMap<>();
    private DescriptorSet set;

    @Override
    public void update() {
        if (set == null) {
            set = pool.allocateSets(generateLayout(ShaderStage.All)).orElseThrow(RuntimeException::new)[0];
        }
    }

    private DescriptorSetLayout generateLayout(Flag<ShaderStage> stages) {
        DescriptorSetLayout.Info info = new DescriptorSetLayout.Info();
        for (Map.Entry<Integer, DescriptorBinding> e : bindings.entrySet()) {
            info.addBinding(e.getKey(), e.getValue().getType(), e.getValue().getDescriptors(), stages);
        }
        return new DescriptorSetLayout(pool.getDevice(), info);
    }

    @Override
    public void setBinding(int slot, DescriptorBinding binding) {
        bindings.put(slot, binding);
        set = null;
    }

    @Override
    public void free() {
        if (set != null) {
            set.free();
        }
    }

    @Override
    public DescriptorPool getPool() {
        return null;
    }

    @Override
    public DescriptorSetLayout getLayout() {
        return null;
    }

}
