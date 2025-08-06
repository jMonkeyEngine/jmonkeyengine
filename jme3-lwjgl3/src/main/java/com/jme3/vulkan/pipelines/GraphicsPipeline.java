package com.jme3.vulkan.pipelines;

import com.jme3.util.natives.Native;
import com.jme3.vulkan.*;
import com.jme3.vulkan.devices.LogicalDevice;
import com.jme3.vulkan.pass.RenderPass;
import com.jme3.vulkan.pipelines.states.*;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;

import static com.jme3.renderer.vulkan.VulkanUtils.check;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

public class GraphicsPipeline extends Pipeline {

    private final RenderPass compat;
    private final int subpassIndex;
    private final MeshDescription mesh;

    public GraphicsPipeline(LogicalDevice<?> device, PipelineLayout layout, RenderPass compat, int subpassIndex, MeshDescription mesh) {
        super(device, PipelineBindPoint.Graphics, layout);
        this.compat = compat;
        this.subpassIndex = subpassIndex;
        this.mesh = mesh;
    }

    public Builder build() {
        return new Builder();
    }

    public class Builder extends VulkanObject.Builder<GraphicsPipeline> {

        private final Collection<ShaderStage> stages = new ArrayList<>();
        private final DynamicState dynamic = new DynamicState();
        private final VertexInputState vertexInput = new VertexInputState(mesh);
        private final InputAssemblyState inputAssembly = new InputAssemblyState();
        private final ViewportState viewport = new ViewportState();
        private final DepthStencilState depthStencil = new DepthStencilState();
        private final RasterizationState rasterization = new RasterizationState();
        private final MultisampleState multisample = new MultisampleState();
        private final ColorBlendState colorBlend = new ColorBlendState();

        @Override
        protected void build() {
            VkPipelineShaderStageCreateInfo.Buffer stageBuf = VkPipelineShaderStageCreateInfo.calloc(stages.size(), stack);
            for (ShaderStage s : stages) {
                stageBuf.get().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                        .stage(s.stages)
                        .module(s.module.getNativeObject())
                        .pName(stack.UTF8(s.module.getEntryPoint()));
            }
            stageBuf.flip();
            VkGraphicsPipelineCreateInfo.Buffer pipeline = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .stageCount(stageBuf.limit())
                    .pStages(stageBuf)
                    .pVertexInputState(vertexInput.toStruct(stack))
                    .pInputAssemblyState(inputAssembly.toStruct(stack))
                    .pViewportState(viewport.toStruct(stack))
                    .pDepthStencilState(depthStencil.toStruct(stack))
                    .pRasterizationState(rasterization.toStruct(stack))
                    .pMultisampleState(multisample.toStruct(stack))
                    .pColorBlendState(colorBlend.toStruct(stack))
                    .pDynamicState(dynamic.toStruct(stack))
                    .layout(layout.getNativeObject())
                    .renderPass(compat.getNativeObject())
                    .subpass(subpassIndex)
                    .basePipelineHandle(VK_NULL_HANDLE)
                    .basePipelineIndex(-1);
            System.out.println("render pass: " + compat.getNativeObject());
            LongBuffer idBuf = stack.mallocLong(1);
            // todo: look into pipeline caching
            check(vkCreateGraphicsPipelines(device.getNativeObject(), VK_NULL_HANDLE, pipeline, null, idBuf),
                    "Failed to create graphics pipeline");
            object = idBuf.get(0);
            ref = Native.get().register(GraphicsPipeline.this);
            device.getNativeReference().addDependent(ref);
        }

        public void addStage(ShaderModule module, int stages) {
            this.stages.add(new ShaderStage(module, stages));
        }

        public DynamicState getDynamicState() {
            return dynamic;
        }

        public VertexInputState getVertexInput() {
            return vertexInput;
        }

        public InputAssemblyState getInputAssembly() {
            return inputAssembly;
        }

        public ViewportState getViewportState() {
            return viewport;
        }

        public DepthStencilState getDepthStencil() {
            return depthStencil;
        }

        public RasterizationState getRasterization() {
            return rasterization;
        }

        public MultisampleState getMultisample() {
            return multisample;
        }

        public ColorBlendState getColorBlend() {
            return colorBlend;
        }

    }

    public static class ShaderStage {

        private final ShaderModule module;
        private final int stages;

        public ShaderStage(ShaderModule module, int stages) {
            this.module = module;
            this.stages = stages;
        }

        public ShaderModule getModule() {
            return module;
        }

        public int getStages() {
            return stages;
        }

    }

}
