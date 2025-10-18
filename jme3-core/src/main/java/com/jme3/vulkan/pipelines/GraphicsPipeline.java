package com.jme3.vulkan.pipelines;

import com.jme3.util.AbstractBuilder;
import com.jme3.util.natives.Native;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pass.Subpass;
import com.jme3.vulkan.pipelines.newnewstates.GraphicsState;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {

    private GraphicsState state;
    private long builtVersion = -1L;

    @Deprecated
    public GraphicsPipeline(LogicalDevice<?> device, PipelineLayout layout, Subpass subpass) {
        super(device, PipelineBindPoint.Graphics, layout);
        this.subpass = subpass;
    }

    public GraphicsPipeline(LogicalDevice<?> device, GraphicsState state) {
        super(device, PipelineBindPoint.Graphics, null);
        this.state = state;
    }

    public void update() {
        long version = state.getCurrentVersion();
        if (builtVersion != version) {
            build().close();
        }
        builtVersion = version;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends AbstractBuilder {

        @Override
        protected void build() {
            if (ref != null) {
                ref.destroy();
                ref = null;
                object = null;
            }
            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            state.fill(stack, pipeline.get());
            pipeline.flip();
            LongBuffer idBuf = stack.mallocLong(1);
            // todo: look into pipeline caching
            check(vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, pipeline, null, idBuf),
                    "Failed to create graphics pipeline");
            object = idBuf.get(0);
            ref = Native.get().register(GraphicsPipeline.this);
            device.getNativeReference().addDependent(ref);
        }

    }

}
