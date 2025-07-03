package com.jme3.vulkan;

import com.jme3.util.natives.Native;
import com.jme3.util.natives.NativeReference;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.*;
import static org.lwjgl.vulkan.VK10.*;

public class PipelineLayout implements Native<Long> {

    private final LogicalDevice device;
    private final NativeReference ref;
    private LongBuffer id = MemoryUtil.memAllocLong(1);

    public PipelineLayout(LogicalDevice device) {
        this.device = device;
        VkPipelineLayoutCreateInfo create = VkPipelineLayoutCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
        check(vkCreatePipelineLayout(device.getNativeObject(), create, null, id),
                "Failed to create pipeline.");
        create.close();
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Long getNativeObject() {
        return id != null ? id.get(0) : null;
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> {
            vkDestroyPipelineLayout(device.getNativeObject(), id.get(0), null);
            MemoryUtil.memFree(id);
        };
    }

    @Override
    public void prematureNativeDestruction() {
        id = null;
    }

    @Override
    public NativeReference getNativeReference() {
        return ref;
    }

}
