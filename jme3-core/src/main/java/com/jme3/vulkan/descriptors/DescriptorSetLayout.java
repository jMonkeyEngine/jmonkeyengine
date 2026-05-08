package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.experimental.ShaderSetBuilder;
import com.jme3.vulkan.pipeline.cache.Cache;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetLayout extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final Map<Integer, UniformBinding> bindings = new HashMap<>();

    protected DescriptorSetLayout(LogicalDevice<?> device) {
        this.device = device;
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyDescriptorSetLayout(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DescriptorSetLayout that = (DescriptorSetLayout) o;
        return Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bindings);
    }

    public Map<Integer, UniformBinding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public static DescriptorSetLayout build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new DescriptorSetLayout(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public static DescriptorSetLayout nullLayout(LogicalDevice<?> device, Cache<DescriptorSetLayout> cache) {
        return build(device, l -> l.setCache(cache));
    }

    public class Builder extends CacheableNativeBuilder<DescriptorSetLayout, DescriptorSetLayout> implements ShaderSetBuilder {

        protected Builder() {}

        @Override
        protected void construct() {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);
            for (UniformBinding b : bindings.values()) {
                b.fillLayoutBinding(layoutBindings.get());
            }
            layoutBindings.flip();
            VkDescriptorSetLayoutCreateInfo create = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(layoutBindings);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(device.getNativeObject(), create, null, idBuf),
                    "Failed to create descriptor set layout.");
            object = idBuf.get(0);
            ref = DisposableManager.reference(DescriptorSetLayout.this);
            device.getReference().addDependent(ref);
        }

        @Override
        protected DescriptorSetLayout getBuildTarget() {
            return DescriptorSetLayout.this;
        }

        @Override
        public void addBinding(int location, UniformBinding binding) {
            bindings.put(location, binding);
        }

    }

}
