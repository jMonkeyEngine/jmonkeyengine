package com.jme3.vulkan.descriptors;

import java.util.function.Function;

public interface ShaderBindingSet {

    void update();

    void setBinding(int slot, DescriptorBinding binding);

    default void setBinding(int slot, Function<DescriptorSetLayout.Binding, DescriptorBinding> binding) {
        setBinding(slot, binding.apply(getLayout().getInfo().getBindings().get(slot)));
    }

    void free();

    DescriptorPool getPool();

    DescriptorSetLayout getLayout();

}
