package com.jme3.vulkan.descriptors;

import com.jme3.util.cache.InlineTimedCache;
import com.jme3.util.natives.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.experimental.ShaderSetBuilder;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetLayout {

    public static final InlineTimedCache<DescriptorSetLayout, Handle> cache = new InlineTimedCache<>(TimeUnit.SECONDS.toMillis(2));

    private final Map<Integer, UniformBinding> bindings = new HashMap<>();
    private Handle handle;

    protected DescriptorSetLayout() {}

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

    public long getId(LogicalDevice<?> device) {
        if (handle == null) {
            handle = cache.computeIfAbsent(this, k -> k.createHandle(device));
        }
        return handle.getId();
    }

    protected Handle createHandle(LogicalDevice<?> device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
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
            return new Handle(device, idBuf.get(0));
        }
    }

    public Map<Integer, UniformBinding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    public static DescriptorSetLayout build(Consumer<Builder> config) {
        DescriptorSetLayout l = new DescriptorSetLayout();
        config.accept(l.new Builder());
        return l;
    }

    public static DescriptorSetLayout nullLayout() {
        return new DescriptorSetLayout();
    }

    public static class Handle implements Disposable {

        private final LogicalDevice<?> device;
        private final long id;
        private final DisposableReference ref;

        public Handle(LogicalDevice<?> device, long id) {
            this.device = device;
            this.id = id;
            this.ref = DisposableManager.reference(this);
            this.device.getReference().addDependent(this.ref);
        }

        @Override
        public Runnable createDestroyer() {
            return () -> vkDestroyDescriptorSetLayout(device.getNativeObject(), id, null);
        }

        @Override
        public DisposableReference getReference() {
            return ref;
        }

        public LogicalDevice<?> getDevice() {
            return device;
        }

        public long getId() {
            return id;
        }

    }

    public class Builder implements ShaderSetBuilder {

        protected Builder() {}

        @Override
        public void addBinding(int location, UniformBinding binding) {
            bindings.put(location, binding);
        }

    }

}
