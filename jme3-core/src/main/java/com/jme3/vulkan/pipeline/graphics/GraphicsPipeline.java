package com.jme3.vulkan.pipeline.graphics;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipeline.Pipeline;
import com.jme3.vulkan.pipeline.PipelineBindPoint;
import com.jme3.vulkan.pipeline.PipelineLayout;
import com.jme3.vulkan.pipeline.states.BasePipelineState;
import com.jme3.vulkan.pipeline.states.PipelineState;
import com.jme3.vulkan.shader.ShaderModule;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.Collection;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {

    private BasePipelineState<?, VkGraphicsPipelineCreateInfo> state;

    @Deprecated
    public GraphicsPipeline(LogicalDevice<?> device, PipelineLayout layout, Subpass subpass) {
        super(device, PipelineBindPoint.Graphics, null);
    }

    /**
     * Creates a new GraphicsPipeline with the given GraphicsState.
     *
     * @param device logical device
     * @param state graphics state (alias created)
     */
    public GraphicsPipeline(LogicalDevice<?> device, Pipeline parent, BasePipelineState<?, VkGraphicsPipelineCreateInfo> state, Collection<ShaderModule> shaders) {
        super(device, PipelineBindPoint.Graphics, parent);
        this.state = state.copy(); // create alias to avoid outside mutations mucking things up
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkGraphicsPipelineCreateInfo.Buffer create = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            state.fill(stack, create.get(), shaders);
            if (parent != null) {
                create.basePipelineHandle(parent.getNativeObject());
            }
            create.flip();
            LongBuffer idBuf = stack.mallocLong(1);
            // todo: look into pipeline caching in the vulkan API
            check(vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, create, null, idBuf),
                    "Failed to create graphics pipeline");
            object = idBuf.get(0);
        }
        ref = Native.get().register(GraphicsPipeline.this);
        device.getNativeReference().addDependent(ref);
    }

    @Override
    public BasePipelineState<?, ?> getState() {
        return state;
    }

}
