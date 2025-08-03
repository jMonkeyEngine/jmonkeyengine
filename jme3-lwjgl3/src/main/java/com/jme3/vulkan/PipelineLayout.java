package com.jme3.vulkan;

import com.jme3.renderer.vulkan.VulkanUtils;
import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import com.jme3.vulkan.descriptors.DescriptorSetLayout;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineLayout implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private final DescriptorSetLayout[] layouts;
    private final long id;

    public PipelineLayout(LogicalDevice device, DescriptorSetLayout... layouts) {
        this.device = device;
        this.layouts = layouts;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineLayoutCreateInfo create = VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .setLayoutCount(layouts.length)
                    .pSetLayouts(VulkanUtils.accumulate(stack, layouts));
            LongBuffer idBuf = stack.mallocLong(1);
            check(vkCreatePipelineLayout(device.getNativeObject(), create, null, idBuf),
                    "Failed to create pipeline.");
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
        return () -> vkDestroyPipelineLayout(device.getNativeObject(), id, null);
    }

    @Override
    public void prematureNativeDestruction() {}

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

    public DescriptorSetLayout[] getDescriptorSetLayouts() {
        return layouts;
    }

}
