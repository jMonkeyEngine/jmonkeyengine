package com.jme3.vulkan.pipeline;

import com.jme3.util.natives.AbstractNative;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

public class PipelineObject extends AbstractNative<Long> {

    private final LogicalDevice<?> device;

    public PipelineObject(LogicalDevice<?> device, long id) {
        this.device = device;
        this.object = id;
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public Runnable createNativeDestroyer() {
        return () -> vkDestroyPipeline(device.getNativeObject(), object, null);
    }

    public static PipelineObject[] graphics(MemoryStack stack, LogicalDevice<?> device, VkGraphicsPipelineCreateInfo.Buffer create) {
        LongBuffer id = stack.mallocLong(create.limit());
        vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, create, null, id);
        PipelineObject[] objects = new PipelineObject[create.limit()];
        for (int i = 0; i < objects.length; i++) {
            objects[i] = new PipelineObject(device, id.get(i));
        }
        return objects;
    }

}
