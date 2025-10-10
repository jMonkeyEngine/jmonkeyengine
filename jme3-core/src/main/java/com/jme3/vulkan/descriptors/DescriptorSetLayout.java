package com.jme3.vulkan.descriptors;

import com.jme3.util.IntMap;
import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.Objects;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetLayout extends AbstractNative<Long> {

    private final LogicalDevice<?> device;
    private final IntMap<SetLayoutBinding> bindings;

    private DescriptorSetLayout(LogicalDevice<?> device, IntMap<SetLayoutBinding> bindings) {
        this.device = device;
        this.bindings = bindings;
    }

    public DescriptorSetLayout(LogicalDevice<?> device, SetLayoutBinding... bindings) {
        this.device = device;
        this.bindings = new IntMap<>();
        for (SetLayoutBinding b : bindings) {
            if (this.bindings.put(b.getBinding(), b) != null) {
                throw new IllegalArgumentException("Duplicate binding index.");
            }
        }
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyDescriptorSetLayout(device.getNativeObject(), object, null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DescriptorSetLayout that = (DescriptorSetLayout) o;
        if (device != that.device || bindings.size() != that.bindings.size()) return false;
        for (IntMap.Entry<SetLayoutBinding> b : bindings) {
            if (!b.getValue().equals(that.bindings.get(b.getKey()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(device, bindings);
    }

    public DescriptorSetLayout build(MemoryStack stack) {
        if (isBuilt()) {
            return this;
        }
        VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.size(), stack);
        for (IntMap.Entry<SetLayoutBinding> b : bindings) {
            b.getValue().fillLayoutBinding(layoutBindings.get());
        }
        layoutBindings.flip();
        VkDescriptorSetLayoutCreateInfo create = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(layoutBindings);
        LongBuffer idBuf = stack.mallocLong(1);
        check(vkCreateDescriptorSetLayout(device.getNativeObject(), create, null, idBuf),
                "Failed to create descriptor set layout.");
        object = idBuf.get(0);
        ref = Native.get().register(DescriptorSetLayout.this);
        device.getNativeReference().addDependent(ref);
        return this;
    }

    public DescriptorSetLayout build() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            return build(stack);
        }
    }

    public DescriptorSetLayout copy() {
        return new DescriptorSetLayout(device, bindings);
    }

    public IntMap<SetLayoutBinding> getBindings() {
        return bindings;
    }

    public boolean isBuilt() {
        return ref != null;
    }

}
