package com.jme3.vulkan.descriptors;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.util.natives.NativeHandle;
import com.jme3.vulkan.compile.Final;
import com.jme3.vulkan.compile.FinalWriter;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;

public class DescriptorSetLayout implements NativeHandle<Long> {

    private final Map<Integer, Binding> bindings = new HashMap<>();
    @Final private long handle;
    @Final private ResourceLifecycle lifecycle;

    private DescriptorSetLayout() {}

    @Override
    public Long getHandle() {
        return handle;
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return lifecycle;
    }

    public Map<Integer, Binding> getBindings() {
        return Collections.unmodifiableMap(bindings);
    }

    @FinalWriter
    private DescriptorSetLayout build(LogicalDevice<?> device) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);
            for (Map.Entry<Integer, Binding> e : bindings.entrySet()) {
                layoutBindings.get()
                        .binding(e.getKey())
                        .descriptorType(e.getValue().getType().getEnum())
                        .descriptorCount(e.getValue().getDescriptors())
                        .stageFlags(e.getValue().getStages().bits());
            }
            VkDescriptorSetLayoutCreateInfo create = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType$Default()
                    .pBindings(layoutBindings.flip());
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(device.getHandle(), create, null, idBuf),
                    "Failed to create descriptor set layout.");
            handle = idBuf.get(0);
        }
        VkDevice deviceHandle = device.getHandle();
        lifecycle = new ResourceLifecycle(this, () -> vkDestroyDescriptorSetLayout(deviceHandle, handle, null));
        device.getLifecycle().addDependent(lifecycle);
        return this;
    }

    public static Builder build() {
        return new Builder();
    }

    public static class Builder {

        private final DescriptorSetLayout layout = new DescriptorSetLayout();

        private Builder() {}

        public Builder addBinding(DescriptorType type, int bindingSlot, Flag<ShaderStage> stages, BindingLogic logic) {
            return addBinding(type, bindingSlot, 1, stages, logic);
        }

        public Builder addBinding(DescriptorType type, int bindingSlot, int descriptors, Flag<ShaderStage> stages, BindingLogic logic) {
            layout.bindings.put(bindingSlot, new Binding(type, descriptors, stages, logic));
            return this;
        }

        public DescriptorSetLayout build(LogicalDevice<?> device) {
            return layout.build(device);
        }

    }

    public static class Binding {

        private final DescriptorType type;
        private final int descriptors;
        private final Flag<ShaderStage> stages;
        private final BindingLogic<Object, Object> logic;

        private Binding(DescriptorType type, int descriptors, Flag<ShaderStage> stages, BindingLogic<Object, Object> logic) {
            this.type = type;
            this.descriptors = descriptors;
            this.stages = stages;
            this.logic = logic;
        }

        public DescriptorType getType() {
            return type;
        }

        public int getDescriptors() {
            return descriptors;
        }

        public Flag<ShaderStage> getStages() {
            return stages;
        }

        public BindingLogic<Object, ?> getLogic() {
            return logic;
        }

    }

}
