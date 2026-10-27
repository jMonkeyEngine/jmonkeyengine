package com.jme3.vulkan.descriptors;

import com.jme3.util.destruction.ResourceLifecycle;
import com.jme3.util.natives.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.shader.ShaderStage;
import com.jme3.vulkan.util.Flag;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;
import java.util.*;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetLayout implements NativeHandle<Long> {

    private final Info info;
    private final long handle;
    private final ResourceLifecycle lifecycle;

    public DescriptorSetLayout(LogicalDevice<?> device, Info info) {
        this.info = info;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(info.bindings.size(), stack);
            for (Map.Entry<Integer, Binding> e : info.bindings.entrySet()) {
                layoutBindings.get()
                    .binding(e.getKey())
                    .descriptorType(e.getValue().type.getEnum())
                    .descriptorCount(e.getValue().descriptors)
                    .stageFlags(e.getValue().stages.bits());
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
    }

    @Override
    public Long getHandle() {
        return handle;
    }

    @Override
    public ResourceLifecycle getLifecycle() {
        return lifecycle;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DescriptorSetLayout that = (DescriptorSetLayout) o;
        return Objects.equals(info, that.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(info);
    }

    public Info getInfo() {
        return info;
    }

    public static Info build() {
        return new Info();
    }

    public static class Info {

        private final Map<Integer, Binding> bindings = new HashMap<>();
        private final BitSet usedBindings = new BitSet();

        public Info() {}

        /**
         * Declares a binding at {@code bindingSlot}. If a binding was already declared at that
         * slot (e.g. by another shader stage that references the same resource), this merges
         * {@code stages} into the existing binding's stage flags instead of replacing it,
         * provided the type and descriptor count agree.
         *
         * @throws IllegalStateException if {@code bindingSlot} is already declared with a
         *         different type or descriptor count
         */
        public Info addBinding(int bindingSlot, DescriptorType type, int descriptors, Flag<ShaderStage> stages) {
            Binding existing = bindings.get(bindingSlot);
            if (existing != null) {
                if (existing.type != type || existing.descriptors != descriptors) {
                    throw new IllegalStateException("Binding " + bindingSlot + " is already declared as "
                            + existing.descriptors + "x" + existing.type + ", cannot redeclare as "
                            + descriptors + "x" + type + ".");
                }
                bindings.put(bindingSlot, new Binding(type, descriptors, existing.stages.add(stages)));
            } else {
                bindings.put(bindingSlot, new Binding(type, descriptors, stages));
                usedBindings.set(bindingSlot);
            }
            return this;
        }

        public int addBinding(DescriptorType type, int descriptors, Flag<ShaderStage> stages) {
            int i = usedBindings.nextClearBit(0);
            bindings.put(i, new Binding(type, descriptors, stages));
            usedBindings.set(i);
            return i;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Info info = (Info) o;
            return Objects.equals(bindings, info.bindings);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(bindings);
        }

        public Map<Integer, Binding> getBindings() {
            return Collections.unmodifiableMap(bindings);
        }

        public DescriptorSetLayout build(LogicalDevice<?> device) {
            return new DescriptorSetLayout(device, this);
        }

    }

    public static class Binding {

        private final DescriptorType type;
        private final int descriptors;
        private final Flag<ShaderStage> stages;

        private Binding(DescriptorType type, int descriptors, Flag<ShaderStage> stages) {
            this.type = type;
            this.descriptors = descriptors;
            this.stages = stages;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Binding that = (Binding) o;
            return descriptors == that.descriptors && type == that.type && Objects.equals(stages, that.stages);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, descriptors, stages);
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

    }

}
