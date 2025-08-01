package com.jme3.vulkan.descriptors;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorSetLayout implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final SetLayoutBinding[] bindings;
    private final long id;

    public DescriptorSetLayout(LogicalDevice device, SetLayoutBinding... bindings) {
        this.device = device;
        this.bindings = bindings;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer layoutBindings = VkDescriptorSetLayoutBinding.calloc(bindings.length, stack);
            for (SetLayoutBinding b : bindings) {
                b.fillLayoutBinding(layoutBindings.get());
            }
            layoutBindings.flip();
            VkDescriptorSetLayoutCreateInfo create = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(layoutBindings);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(device.getNativeObject(), create, null, idBuf),
                    "Failed to create descriptor set layout.");
            id = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyDescriptorSetLayout(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public SetLayoutBinding[] getBindings() {
        return bindings;
    }

}
