package com.jme3.vulkan.descriptors;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorPool implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final long id;

    public DescriptorPool(LogicalDevice device, int sets, PoolSize... sizes) {
        this.device = device;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorPoolCreateInfo create = VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(PoolSize.aggregate(stack, sizes))
                    .maxSets(sets);
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreateDescriptorPool(device.getNativeObject(), create, null, idBuf),
                    "Failed to create descriptor pool.");
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
        return () -> vkDestroyDescriptorPool(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public DescriptorSet[] allocateSets(DescriptorSetLayout... layouts) {
        assert layouts.length > 0 : "Must specify at least one set layout.";
        // layouts length = number of descriptor sets created
        DescriptorSet[] sets = new DescriptorSet[layouts.length];
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkDescriptorSetAllocateInfo allocate = VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(id)
                    .pSetLayouts(VulkanUtils.accumulate(stack, layouts));
            LongBuffer setBuf = stack.mallocLong(layouts.length);
            check(vkAllocateDescriptorSets(device.getNativeObject(), allocate, setBuf),
                    "Failed to allocate descriptor sets.");
            for (int i = 0; i < setBuf.limit(); i++) {
                sets[i] = new DescriptorSet(device, this, layouts[i], setBuf.get(i));
            }
        }
        return sets;
    }

    public void reset() {
        vkResetDescriptorPool(device.getNativeObject(), id, 0);
    }

}
