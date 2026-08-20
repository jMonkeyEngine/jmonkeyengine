package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.material.experimental.ShaderBindingLayout;
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

public class DescriptorSetLayout implements ShaderBindingLayout, NativeHandle<Long> {

    private final Info info;
    private final long handle;
    private final Destructor destructor;

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
        this.destructor = device.getDestructor().addDependent(new Destructor(this) {
            @Override
            protected void runDestroy() {
                vkDestroyDescriptorSetLayout(deviceHandle, handle, null);
            }
        });
    }

    @Override
    public Long getHandle() {
        return handle;
    }

    @Override
    public Destructor getDestructor() {
        return destructor;
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

    public static class Info {

        private final Map<Integer, Binding> bindings = new HashMap<>();
        private final BitSet usedBindings = new BitSet();

        public Info() {}

        public Info addBinding(int bindingSlot, DescriptorType type, int descriptors, Flag<ShaderStage> stages) {
            bindings.put(bindingSlot, new Binding(type, descriptors, stages));
            usedBindings.set(bindingSlot);
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
