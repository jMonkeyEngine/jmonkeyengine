package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.CacheableNativeBuilder;
import com.jme3.util.natives.DisposableManager;
import com.jme3.vulkan.devices.LogicalDevice;
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
    private final Map<String, SetLayoutBinding> bindings = new HashMap<>();

    protected DescriptorSetLayout(LogicalDevice<?> device) {
        this.device = device;
    }

    protected DescriptorSetLayout(LogicalDevice<?> device, Map<String, SetLayoutBinding> bindings) {
        this(device);
        this.bindings.putAll(bindings);
    }

    @Override
    public Runnable createDestroyer() {
        return () -> vkDestroyDescriptorSetLayout(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DescriptorSetLayout that = (DescriptorSetLayout) o;
        return Objects.equals(device, that.device) && Objects.equals(bindings, that.bindings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, bindings);
    }

    public DescriptorSetLayout copy() {
        return new DescriptorSetLayout(device, bindings);
    }

    public Map<String, SetLayoutBinding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public static DescriptorSetLayout build(LogicalDevice<?> device, Consumer<Builder> config) {
        Builder b = new DescriptorSetLayout(device).new Builder();
        config.accept(b);
        return b.build();
    }

    public static DescriptorSetLayout build(LogicalDevice<?> device, Map<String, SetLayoutBinding> bindings) {
        return new DescriptorSetLayout(device, bindings).new Builder().build();
    }

    public static DescriptorSetLayout build(LogicalDevice<?> device, Map<String, SetLayoutBinding> bindings, Cache<DescriptorSetLayout> cache) {
        return new DescriptorSetLayout(device, bindings).new Builder(cache).build();
    }

    public class Builder extends CacheableNativeBuilder<DescriptorSetLayout, DescriptorSetLayout> {

        private Builder() {}

        private Builder(Cache<DescriptorSetLayout> cache) {
            setCache(cache);
        }

        @Override
        protected void construct() {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);
            for (SetLayoutBinding b : bindings.values()) {
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

        public void addBinding(String uniform, SetLayoutBinding binding) {
            bindings.put(uniform, binding);
        }

    }

}
