package com.jme3.vulkan.pipelines;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.shader.ShaderModule;
import com.jme3.vulkan.devices.LogicalDevice;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class ComputePipeline extends Pipeline {

    private final ShaderModule shader;

    public ComputePipeline(LogicalDevice<?> device, PipelineBindPoint bindPoint, PipelineLayout layout, ShaderModule shader) {
        super(device, bindPoint, layout);
        this.shader = shader;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkPipelineShaderStageCreateInfo stage = VkPipelineShaderStageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_COMPUTE_BIT)
                    .module(shader.getNativeObject())
                    .pName(stack.UTF8(shader.getEntryPoint()));
            VkComputePipelineCreateInfo.Buffer pipeline = VkComputePipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .stage(stage)
                    .layout(layout.getNativeObject())
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            LongBuffer idBuf = stack.mallocLong(1);
            vkCreateComputePipelines(device.getNativeObject(), VK_NULL_HANDLE, pipeline, null, idBuf);
            object = idBuf.get(0);
        }
        ref = Native.get().register(this);
        device.getNativeReference().addDependent(ref);
    }

    public ShaderModule getShader() {
        return shader;
    }

}
